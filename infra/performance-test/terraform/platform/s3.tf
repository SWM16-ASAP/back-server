data "aws_caller_identity" "current" {}

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
