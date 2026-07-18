output "aws_region" {
  description = "AWS region used by this test run."
  value       = var.aws_region
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

output "dependency_endpoints" {
  description = "Private DNS endpoints exposed by Cloud Map."
  value = {
    for name, service in local.dependency_services :
    name => "${name}.${aws_service_discovery_private_dns_namespace.this.name}:${service.port}"
  }
}
