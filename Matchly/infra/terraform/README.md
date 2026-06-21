# Matchly — Terraform (AWS)

> ## ⚠️ STARTER MODULES — READ BEFORE RUNNING
> This wiring **provisions real, BILLABLE AWS resources** (EKS, RDS, ElastiCache,
> MSK, NAT gateways). A `terraform apply` here can cost tens of dollars per day.
> It is a **starter** scaffold of the topology in `docs/ARCHITECTURE.md §8`, not
> a production-ready configuration. **Review every resource** — networking, IAM,
> encryption in transit, backups, deletion protection, multi-AZ, sizing — before
> using it for anything real. Always run `terraform plan` and read it carefully
> before `apply`.

## What it provisions

| Resource | Module / type | Notes |
|---|---|---|
| VPC (public + private subnets, NAT) | `terraform-aws-modules/vpc` | single NAT in non-prod |
| EKS cluster + managed node group | `terraform-aws-modules/eks` | `t3.large` x2 default |
| RDS PostgreSQL 16 | `terraform-aws-modules/rds` | **one instance**, per-service DBs created at app bootstrap |
| ElastiCache Redis 7 | `aws_elasticache_replication_group` | single node (non-prod), 1 replica (prod) |
| MSK (Kafka 3.6) | `aws_msk_cluster` | 3 brokers default |
| ECR repos | `aws_ecr_repository` (for_each) | one per component: `matchly-<component>` |
| ALB / Ingress | *not provisioned* | created in-cluster by AWS LB Controller via the K8s Ingress |

## Prerequisites

- **Terraform >= 1.5** and the **AWS provider ~> 5** (pinned in `versions.tf`).
- **AWS credentials** with broad permissions (VPC, EKS, RDS, ElastiCache, MSK,
  ECR, IAM). Configure via `aws configure`, `AWS_PROFILE`, or env vars.
- `kubectl` + `aws` CLI to talk to the cluster after creation.
- **Remote state backend is a TODO.** `versions.tf` has a commented S3+DynamoDB
  backend block — set one up before any shared/team use. The first run uses
  local state.

## Usage

```bash
cd infra/terraform

cp terraform.tfvars.example terraform.tfvars   # then edit
export TF_VAR_db_password='choose-a-strong-password'   # keep secrets OUT of tfvars

terraform init      # downloads providers + community modules
terraform plan      # REVIEW the plan carefully (billable resources!)
terraform apply     # provisions everything

terraform destroy   # tears it all down (do this to stop charges)
```

After apply, point kubectl at the new cluster and feed endpoints into the K8s
manifests:

```bash
aws eks update-kubeconfig --name "$(terraform output -raw eks_cluster_name)" --region "$(terraform output -raw region)"

terraform output rds_endpoint
terraform output redis_primary_endpoint
terraform output msk_bootstrap_brokers
terraform output ecr_repository_urls
```

Use `redis_primary_endpoint` and `msk_bootstrap_brokers` to fill in the prod
overlay ConfigMap (`infra/k8s/overlays/prod/kustomization.yaml`), and the ECR
URLs as the image registry for your kustomize `images:` entries.

## Secrets

- `db_password` is a **sensitive** variable. Pass it via `TF_VAR_db_password` or
  `-var`, never a committed tfvars. For prod prefer
  `manage_master_user_password = true` (AWS Secrets Manager) in `main.tf`.
- JWT secret and LLM API keys are **application** secrets, not infra — manage
  them in the cluster (Kubernetes Secret / External Secrets / Secrets Manager),
  see `infra/k8s/base/secret.yaml`.

## Known gaps (intentional, for a starter)

- No remote state backend wired (commented TODO in `versions.tf`).
- Security groups are scoped to the VPC CIDR, not tightened to the EKS node SG.
- MSK and Redis use `TLS_PLAINTEXT` / no transit encryption — enable TLS for prod.
- EKS public endpoint is open — restrict or make private for prod.
- No IRSA / AWS Load Balancer Controller / cert-manager / autoscaler add-ons.
- Per-service RDS databases are created at app bootstrap (Flyway), not here;
  splitting into one instance per service is a deliberate later step.
