variable "region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "us-east-1"
}

variable "env" {
  description = "Environment name (dev, staging, prod). Used in resource names/tags."
  type        = string
  default     = "dev"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "azs" {
  description = "Availability zones to spread subnets across."
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b", "us-east-1c"]
}

# --- EKS ---
variable "cluster_version" {
  description = "EKS Kubernetes version."
  type        = string
  default     = "1.30"
}

variable "node_instance_types" {
  description = "Instance types for the managed node group."
  type        = list(string)
  default     = ["t3.large"]
}

variable "node_desired_size" {
  description = "Desired number of worker nodes."
  type        = number
  default     = 2
}

variable "node_min_size" {
  description = "Minimum number of worker nodes."
  type        = number
  default     = 2
}

variable "node_max_size" {
  description = "Maximum number of worker nodes."
  type        = number
  default     = 5
}

# --- RDS PostgreSQL ---
variable "db_instance_class" {
  description = "RDS instance class. STARTER sizing — review for prod."
  type        = string
  default     = "db.t3.medium"
}

variable "db_allocated_storage" {
  description = "RDS allocated storage in GiB."
  type        = number
  default     = 20
}

variable "db_username" {
  description = "Master username for RDS PostgreSQL."
  type        = string
  default     = "matchly"
}

variable "db_password" {
  description = <<-EOT
    Master password for RDS PostgreSQL. DO NOT hardcode in a committed tfvars.
    Pass via TF_VAR_db_password, -var, or (preferred) sourced from AWS Secrets
    Manager / SSM. Marked sensitive so it is redacted in plan/apply output.
  EOT
  type        = string
  sensitive   = true
}

# --- ElastiCache (Redis) ---
variable "redis_node_type" {
  description = "ElastiCache Redis node type."
  type        = string
  default     = "cache.t3.micro"
}

# --- MSK (Kafka) ---
variable "msk_instance_type" {
  description = "MSK broker instance type."
  type        = string
  default     = "kafka.t3.small"
}

variable "msk_broker_count" {
  description = "Number of MSK broker nodes (must be a multiple of the AZ count)."
  type        = number
  default     = 3
}

# --- Per-service component list (drives ECR repos and per-service DB names) ---
variable "components" {
  description = "Matchly app components that get an ECR repository."
  type        = set(string)
  default = [
    "api-gateway",
    "auth-service",
    "candidate-service",
    "job-service",
    "application-service",
    "matching-service",
    "interview-service",
    "analytics-service",
    "ai-service",
    "frontend",
  ]
}

variable "service_databases" {
  description = "Per-service database names created on the single RDS instance to start (split later)."
  type        = list(string)
  default = [
    "auth_db",
    "candidate_db",
    "job_db",
    "application_db",
    "matching_db",
    "interview_db",
    "analytics_db",
    "ai_db",
  ]
}
