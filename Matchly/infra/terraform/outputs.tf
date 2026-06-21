output "region" {
  description = "AWS region."
  value       = var.region
}

# --- EKS ---
output "eks_cluster_name" {
  description = "EKS cluster name."
  value       = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  description = "EKS API server endpoint. Use with `aws eks update-kubeconfig`."
  value       = module.eks.cluster_endpoint
}

output "eks_cluster_certificate_authority_data" {
  description = "Base64 CA data for the EKS cluster."
  value       = module.eks.cluster_certificate_authority_data
  sensitive   = true
}

# --- RDS ---
output "rds_endpoint" {
  description = "RDS PostgreSQL connection endpoint (host:port)."
  value       = module.rds.db_instance_endpoint
}

output "rds_database_name" {
  description = "Initial database created by the module (per-service DBs created at app bootstrap)."
  value       = module.rds.db_instance_name
}

# --- Redis ---
output "redis_primary_endpoint" {
  description = "ElastiCache Redis primary endpoint."
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
}

# --- MSK ---
output "msk_bootstrap_brokers" {
  description = "MSK bootstrap brokers (plaintext). Set as KAFKA_BOOTSTRAP_SERVERS."
  value       = aws_msk_cluster.kafka.bootstrap_brokers
}

output "msk_bootstrap_brokers_tls" {
  description = "MSK bootstrap brokers (TLS)."
  value       = aws_msk_cluster.kafka.bootstrap_brokers_tls
}

# --- ECR ---
output "ecr_repository_urls" {
  description = "Map of component name -> ECR repository URL."
  value       = { for name, repo in aws_ecr_repository.component : name => repo.repository_url }
}
