provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      managed_by  = "terraform"
      project     = "llv-performance-test"
      test_run_id = var.test_run_id
    }
  }
}
