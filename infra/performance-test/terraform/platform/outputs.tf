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

output "app_ecr_repository_url" {
  description = "Temporary ECR repository receiving the LLV API image under test."
  value       = aws_ecr_repository.app.repository_url
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

output "ecs_cluster_arn" {
  description = "ECS cluster used to run the one-off k6 task."
  value       = aws_ecs_cluster.this.arn
}

output "k6_task_definition_arn" {
  description = "Task definition used by the k6 smoke runner."
  value       = aws_ecs_task_definition.k6.arn
}

output "k6_security_group_id" {
  description = "Security group assigned to the one-off k6 task."
  value       = aws_security_group.k6.id
}

output "k6_subnet_id" {
  description = "Public subnet used by the one-off k6 task."
  value       = values(aws_subnet.public)[0].id
}

output "app_desired_count" {
  description = "Expected number of healthy application targets."
  value       = var.app_desired_count
}

output "app_security_group_id" {
  description = "Security group reused by the dependency probe."
  value       = aws_security_group.app.id
}

output "dependency_probe_task_definition_arn" {
  description = "Task definition that verifies private dependency connectivity."
  value       = aws_ecs_task_definition.dependency_probe.arn
}

output "grafana_service_name" {
  description = "ECS service name used to discover the temporary Grafana public IP."
  value       = aws_ecs_service.grafana.name
}

output "results_bucket" {
  description = "Temporary S3 bucket containing k6 result artifacts."
  value       = aws_s3_bucket.results.id
}
