output "app_url" {
  description = "HTTPS URL for the trading app, served by CloudFront."
  value       = local.cloudfront_url
}

output "cloudfront_distribution_id" {
  description = "CloudFront distribution ID for invalidations and operations."
  value       = aws_cloudfront_distribution.app.id
}

output "admin_urls" {
  description = "Admin UIs, reachable from admin_cidr_blocks only."
  value = {
    grafana    = "${local.public_url}:${local.alb_ports["grafana"]}"
    dashboard  = "${local.public_url}:${local.alb_ports["python"]}"
    kafka_ui   = "${local.public_url}:${local.alb_ports["kafka-ui"]}"
    prometheus = "${local.public_url}:${local.alb_ports["prometheus"]}"
  }
}

output "grafana_admin_password" {
  description = "Grafana admin password. Show it with: tofu output -raw grafana_admin_password"
  value       = random_password.grafana.result
  sensitive   = true
}

output "rds_endpoint" {
  description = "Postgres host, reachable from inside the VPC only."
  value       = aws_db_instance.this.address
}

output "kafka_bootstrap_brokers" {
  description = "MSK bootstrap brokers, reachable from inside the VPC only."
  value       = aws_msk_cluster.this.bootstrap_brokers
}

output "ecs_cluster" {
  description = "ECS cluster name, for aws ecs commands."
  value       = aws_ecs_cluster.this.name
}
