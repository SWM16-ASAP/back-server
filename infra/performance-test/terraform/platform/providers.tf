provider "aws" {
  region = var.aws_region

  dynamic "assume_role" {
    for_each = var.deployment_role_arn == null ? [] : [var.deployment_role_arn]

    content {
      role_arn     = assume_role.value
      session_name = "llv-performance-test"
    }
  }

  default_tags {
    tags = {
      managed_by  = "terraform"
      project     = "llv-performance-test"
      test_run_id = var.test_run_id
    }
  }
}
