provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile

  default_tags {
    tags = {
      Project   = "TheUnderFrogs"
      ManagedBy = "OpenTofu"
    }
  }
}

data "aws_caller_identity" "current" {}

# Only looked up when var.availability_zones is empty.
data "aws_availability_zones" "available" {
  count = length(var.availability_zones) == 0 ? 1 : 0
  state = "available"
}

locals {
  # Environment for the local-exec scripts, so their aws calls use the same account as the provider.
  exec_env = merge(
    { AWS_REGION = var.aws_region },
    var.aws_profile == null ? {} : { AWS_PROFILE = var.aws_profile },
  )
}
