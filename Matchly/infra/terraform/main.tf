# =============================================================================
# Matchly — AWS infrastructure (STARTER).
#
# This is a starter wiring of the prod topology from docs/ARCHITECTURE.md §8:
#   VPC -> EKS (managed node group) -> RDS PostgreSQL (per-service DBs) ->
#   ElastiCache Redis -> MSK (Kafka) -> ECR (one repo per component).
#
# It is NOT production-hardened. Review networking, IAM, encryption, backups,
# multi-AZ, deletion protection, and sizing before any real use. It provisions
# BILLABLE resources (EKS, RDS, ElastiCache, MSK, NAT gateways).
# =============================================================================

locals {
  name = "matchly-${var.env}"

  tags = {
    Project     = "matchly"
    Environment = var.env
  }
}

# -----------------------------------------------------------------------------
# Networking — VPC with public + private subnets across the AZs.
# -----------------------------------------------------------------------------
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "${local.name}-vpc"
  cidr = var.vpc_cidr
  azs  = var.azs

  # /20 private (workloads) + /24 public (load balancers) per AZ.
  private_subnets = [for i, az in var.azs : cidrsubnet(var.vpc_cidr, 4, i)]
  public_subnets  = [for i, az in var.azs : cidrsubnet(var.vpc_cidr, 8, i + 48)]

  enable_nat_gateway = true
  single_nat_gateway = var.env != "prod" # one NAT in non-prod to save cost
  enable_dns_hostnames = true

  # Tags so the AWS Load Balancer Controller can discover subnets.
  public_subnet_tags = {
    "kubernetes.io/role/elb" = "1"
  }
  private_subnet_tags = {
    "kubernetes.io/role/internal-elb" = "1"
  }

  tags = local.tags
}

# -----------------------------------------------------------------------------
# EKS — managed control plane + one managed node group.
# -----------------------------------------------------------------------------
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.0"

  cluster_name    = local.name
  cluster_version = var.cluster_version

  cluster_endpoint_public_access = true # STARTER: lock down / make private for prod

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  eks_managed_node_groups = {
    default = {
      instance_types = var.node_instance_types
      desired_size   = var.node_desired_size
      min_size       = var.node_min_size
      max_size       = var.node_max_size
    }
  }

  tags = local.tags
}

# -----------------------------------------------------------------------------
# Security groups for the backing data stores (allow from inside the VPC).
# STARTER: scoped to the VPC CIDR. Tighten to the EKS node SG for prod.
# -----------------------------------------------------------------------------
resource "aws_security_group" "data" {
  name_prefix = "${local.name}-data-"
  description = "Allow Postgres/Redis/Kafka from within the VPC"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description = "PostgreSQL"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  ingress {
    description = "Redis"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  ingress {
    description = "Kafka (MSK plaintext + TLS)"
    from_port   = 9092
    to_port     = 9098
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags

  lifecycle {
    create_before_destroy = true
  }
}

# -----------------------------------------------------------------------------
# RDS PostgreSQL — ONE instance to start, with a database PER SERVICE.
# NOTE: the module creates the instance + the first database (here: postgres).
# The per-service databases (auth_db, candidate_db, ...) are created at app
# bootstrap (Flyway per service) or via a one-off init job — see
# infra/docker/postgres/init-databases.sh for the local equivalent. Splitting
# into one instance per service is a later step (see ARCHITECTURE §8).
# -----------------------------------------------------------------------------
module "rds" {
  source  = "terraform-aws-modules/rds/aws"
  version = "~> 6.0"

  identifier = "${local.name}-postgres"

  engine               = "postgres"
  engine_version       = "16"
  family               = "postgres16"
  major_engine_version = "16"
  instance_class       = var.db_instance_class

  allocated_storage     = var.db_allocated_storage
  max_allocated_storage = var.db_allocated_storage * 4

  db_name  = "postgres"
  username = var.db_username
  password = var.db_password
  port     = 5432
  # Module manages the password directly here (do not also let it manage a
  # random one). For prod prefer manage_master_user_password = true (Secrets Mgr).
  manage_master_user_password = false

  multi_az               = var.env == "prod"
  vpc_security_group_ids = [aws_security_group.data.id]
  create_db_subnet_group = true
  subnet_ids             = module.vpc.private_subnets

  storage_encrypted   = true
  skip_final_snapshot = var.env != "prod"
  deletion_protection = var.env == "prod"

  tags = local.tags
}

# -----------------------------------------------------------------------------
# ElastiCache Redis — gateway rate limiting, caches (ARCHITECTURE §7.3).
# -----------------------------------------------------------------------------
resource "aws_elasticache_subnet_group" "redis" {
  name       = "${local.name}-redis"
  subnet_ids = module.vpc.private_subnets
  tags       = local.tags
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = "${local.name}-redis"
  description          = "Matchly Redis (${var.env})"

  engine         = "redis"
  engine_version = "7.1"
  node_type      = var.redis_node_type
  port           = 6379

  # STARTER: single node in non-prod, one replica in prod.
  num_cache_clusters         = var.env == "prod" ? 2 : 1
  automatic_failover_enabled = var.env == "prod"

  subnet_group_name  = aws_elasticache_subnet_group.redis.name
  security_group_ids = [aws_security_group.data.id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = false # STARTER: enable + use AUTH token for prod

  tags = local.tags
}

# -----------------------------------------------------------------------------
# MSK (managed Kafka) — the event bus (ARCHITECTURE §4, §8).
# -----------------------------------------------------------------------------
resource "aws_msk_cluster" "kafka" {
  cluster_name           = "${local.name}-kafka"
  kafka_version          = "3.6.0"
  number_of_broker_nodes = var.msk_broker_count

  broker_node_group_info {
    instance_type   = var.msk_instance_type
    client_subnets  = module.vpc.private_subnets
    security_groups = [aws_security_group.data.id]

    storage_info {
      ebs_storage_info {
        volume_size = 50
      }
    }
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "TLS_PLAINTEXT" # STARTER: move to TLS-only for prod
      in_cluster    = true
    }
  }

  tags = local.tags
}

# -----------------------------------------------------------------------------
# ECR — one repository per component. Images: <ecr-url>/matchly-<component>.
# -----------------------------------------------------------------------------
resource "aws_ecr_repository" "component" {
  for_each = var.components

  name                 = "matchly-${each.value}"
  image_tag_mutability = "MUTABLE" # STARTER: IMMUTABLE recommended for prod
  force_delete         = var.env != "prod"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = local.tags
}

# -----------------------------------------------------------------------------
# ALB / Ingress (NOTE, not provisioned here):
# Application traffic is fronted by an ALB created by the AWS Load Balancer
# Controller in-cluster, driven by the Kubernetes Ingress in
# infra/k8s/base/ingress.yaml (switch ingressClassName to "alb" for EKS).
# Install the controller via Helm and grant it IRSA permissions; that wiring is
# intentionally out of scope for this starter Terraform.
# -----------------------------------------------------------------------------
