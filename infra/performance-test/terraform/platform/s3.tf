data "aws_caller_identity" "current" {}

locals {
  ai_bucket_names = {
    input  = "${local.name_prefix}-${data.aws_caller_identity.current.account_id}-${substr(md5(var.aws_region), 0, 6)}-ai-in"
    output = "${local.name_prefix}-${data.aws_caller_identity.current.account_id}-${substr(md5(var.aws_region), 0, 6)}-ai-out"
  }
}

resource "aws_s3_bucket" "environment_files" {
  bucket        = "${local.name_prefix}-${data.aws_caller_identity.current.account_id}-${var.aws_region}-env"
  force_destroy = true
}

resource "aws_s3_bucket_public_access_block" "environment_files" {
  bucket = aws_s3_bucket.environment_files.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "environment_files" {
  bucket = aws_s3_bucket.environment_files.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket" "ai" {
  for_each = local.ai_bucket_names

  bucket        = each.value
  force_destroy = true
}

resource "aws_s3_bucket_public_access_block" "ai" {
  for_each = aws_s3_bucket.ai

  bucket = each.value.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "ai" {
  for_each = aws_s3_bucket.ai

  bucket = each.value.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket" "results" {
  bucket        = "${local.name_prefix}-${data.aws_caller_identity.current.account_id}-${var.aws_region}-results"
  force_destroy = true
}

resource "aws_s3_bucket_public_access_block" "results" {
  bucket = aws_s3_bucket.results.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "results" {
  bucket = aws_s3_bucket.results.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}
