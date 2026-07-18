output "aws_region" {
  description = "AWS region used by this test run."
  value       = var.aws_region
}

output "test_run_id" {
  description = "Identifier shared by resources in this test run."
  value       = var.test_run_id
}

output "target_group_arn" {
  description = "Target group inspected by the phase-one verification script."
  value       = aws_lb_target_group.app.arn
}

output "environment_file_bucket" {
  description = "Temporary S3 bucket used for ECS environment files."
  value       = aws_s3_bucket.environment_files.id
}

output "app_environment_file_key" {
  description = "Object key reserved for the application environment file."
  value       = local.environment_file_key
}

output "ai_input_bucket" {
  description = "Temporary S3 bucket receiving AI input objects."
  value       = aws_s3_bucket.ai["input"].id
}

output "ai_output_bucket" {
  description = "Temporary S3 bucket containing AI output objects."
  value       = aws_s3_bucket.ai["output"].id
}

output "dependency_endpoints" {
  description = "Private DNS endpoints exposed by Cloud Map."
  value = {
    for name, service in local.dependency_services :
    name => "${name}.${aws_service_discovery_private_dns_namespace.this.name}:${service.port}"
  }
}
