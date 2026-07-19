variable "aws_region" {
  description = "AWS region for the temporary test environment."
  type        = string
}

variable "test_run_id" {
  description = "Lowercase identifier shared by all resources created for one test run."
  type        = string

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{2,19}$", var.test_run_id))
    error_message = "test_run_id must start with a lowercase letter and contain 3 to 20 lowercase letters, numbers, or hyphens."
  }
}

variable "app_image_tag" {
  description = "Immutable ECR tag assigned to the LLV API image under test."
  type        = string

  validation {
    condition     = can(regex("^[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}$", var.app_image_tag))
    error_message = "app_image_tag must be a valid non-empty container image tag."
  }
}

variable "redis_image" {
  description = "Redis image used by the temporary dependency service."
  type        = string
  default     = "redis:7.4.9-alpine"
}

variable "mysql_image" {
  description = "MySQL image used by the temporary dependency service."
  type        = string
  default     = "mysql:8.4.10"
}

variable "mock_image" {
  description = "WireMock image used to control external API responses."
  type        = string
  default     = "wiremock/wiremock:3.13.2"
}

variable "mock_init_image" {
  description = "Container image that registers WireMock mappings through the Admin API."
  type        = string
  default     = "curlimages/curl:8.12.1"
}

variable "mock_scenario" {
  description = "WireMock response profile loaded when the mock service starts."
  type        = string
  default     = "success"

  validation {
    condition     = contains(["success", "delay", "error"], var.mock_scenario)
    error_message = "mock_scenario must be one of success, delay, or error."
  }
}

variable "k6_image" {
  description = "k6 image used by the one-off smoke task."
  type        = string
  default     = "grafana/k6:2.0.0"
}

variable "k6_task_cpu" {
  description = "Fargate CPU units assigned to the one-off k6 task."
  type        = number
  default     = 256
}

variable "k6_task_memory" {
  description = "Memory in MiB assigned to the one-off k6 task."
  type        = number
  default     = 512
}

variable "dependency_probe_image" {
  description = "Alpine image used by the one-off dependency connectivity probe."
  type        = string
  default     = "public.ecr.aws/docker/library/alpine:3.22.2"
}

variable "container_port" {
  description = "TCP port exposed by the phase-one container."
  type        = number
  default     = 80
}

variable "health_check_path" {
  description = "HTTP path used by the ALB target group health check."
  type        = string
  default     = "/"
}

variable "health_check_grace_period_seconds" {
  description = "Time allowed for the Spring application to start before ALB health failures count."
  type        = number
  default     = 180
}

variable "task_cpu" {
  description = "Fargate CPU units for the phase-one task."
  type        = number
  default     = 256
}

variable "task_memory" {
  description = "Fargate memory in MiB for the phase-one task."
  type        = number
  default     = 512
}

variable "app_desired_count" {
  description = "Number of application tasks behind the internal ALB."
  type        = number
  default     = 2

  validation {
    condition     = var.app_desired_count >= 1
    error_message = "app_desired_count must be at least 1."
  }
}

variable "vpc_cidr" {
  description = "CIDR block reserved for the temporary phase-one VPC."
  type        = string
  default     = "10.240.0.0/16"
}

variable "availability_zones" {
  description = "Two availability zones used by the internal ALB and Fargate task."
  type        = list(string)

  validation {
    condition     = length(var.availability_zones) == 2
    error_message = "availability_zones must contain exactly two availability zones."
  }
}

variable "log_retention_days" {
  description = "CloudWatch log retention for the temporary task."
  type        = number
  default     = 7
}
