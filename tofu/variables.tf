variable "aws_region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "eu-west-1"
}

variable "aws_profile" {
  description = "AWS CLI profile to deploy with (from ~/.aws/config). Null uses the default credentials."
  type        = string
  default     = null
}

variable "project_name" {
  description = "Prefix for resource names. Also the private DNS domain (<project_name>.local) services use to find each other."
  type        = string
  default     = "underfrogs"
}

variable "image_tag" {
  description = "Tag for every image built and pushed to ECR. Change it (e.g. to the git commit) to rebuild and redeploy."
  type        = string
  default     = "latest"
}

variable "build_images" {
  description = "Build the images and push them to ECR during apply. Needs Docker, Maven and JDK 21 on the machine running tofu."
  type        = bool
  default     = true
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.20.0.0/16"
}

variable "availability_zones" {
  description = "Two availability zones to use. Empty picks the region's first two."
  type        = list(string)
  default     = []

  validation {
    condition     = length(var.availability_zones) == 0 || length(var.availability_zones) == 2
    error_message = "Give exactly two availability zones, or none."
  }
}

variable "admin_cidr_blocks" {
  description = "CIDR blocks allowed to reach the admin ports (Grafana 8080, dashboard 8085, Kafka UI 8086, Prometheus 9090). Empty closes them."
  type        = list(string)
  default     = []
}

variable "desired_count" {
  description = "Tasks to run per ECS service."
  type        = number
  default     = 1
}

variable "log_retention_days" {
  description = "Days to keep container logs in CloudWatch."
  type        = number
  default     = 7
}

variable "rds_instance_class" {
  description = "Instance class for the shared Postgres instance."
  type        = string
  default     = "db.t4g.micro"
}

variable "rds_allocated_storage" {
  description = "Storage for the Postgres instance, in GB."
  type        = number
  default     = 20
}

variable "skip_final_snapshot" {
  description = "Skip the final RDS snapshot on destroy. Set false to keep the data."
  type        = bool
  default     = true
}

variable "msk_instance_type" {
  description = "Broker instance type for MSK."
  type        = string
  default     = "kafka.t3.small"
}

variable "msk_kafka_version" {
  description = "Kafka version for MSK. Compose runs 3.9.1."
  type        = string
  default     = "3.9.x"
}

variable "grafana_admin_user" {
  description = "Grafana admin username."
  type        = string
  default     = "admin"
}

variable "stripe_secret_key" {
  description = "Stripe secret key for accounts-service top-ups. Optional."
  type        = string
  default     = ""
  sensitive   = true
}

variable "alpaca_key_id" {
  description = "Alpaca key ID for news and live quotes. Optional; without it orders price from the daily close."
  type        = string
  default     = ""
  sensitive   = true
}

variable "alpaca_secret_key" {
  description = "Alpaca secret key. Optional."
  type        = string
  default     = ""
  sensitive   = true
}

variable "ai_api_key" {
  description = "Groq API key for the AI trading assistant. Optional; without it the assistant reports itself unavailable."
  type        = string
  default     = ""
  sensitive   = true
}
