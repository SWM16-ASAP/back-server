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

variable "redis_exporter_image" {
  description = "Redis exporter image used to expose Redis metrics."
  type        = string
  default     = "oliver006/redis_exporter:v1.84.0"
}

variable "mysql_image" {
  description = "MySQL image used by the temporary dependency service."
  type        = string
  default     = "mysql:8.4.10"
}

variable "mongo_reset_image" {
  description = "MongoDB shell image used to reset the Atlas test database."
  type        = string
  default     = "mongo:8.0.13"
}

variable "mysql_exporter_image" {
  description = "Prometheus MySQL exporter image used to expose MySQL metrics."
  type        = string
  default     = "prom/mysqld-exporter:v0.18.0"
}

variable "prometheus_image" {
  description = "Prometheus image used to collect temporary test metrics."
  type        = string
  default     = "prom/prometheus:v3.13.1"
}

variable "prometheus_retention_time" {
  description = "Maximum time Prometheus retains metrics for one test environment."
  type        = string
  default     = "6h"
}

variable "prometheus_retention_size" {
  description = "Maximum Prometheus TSDB size for one test environment."
  type        = string
  default     = "1GB"
}

variable "grafana_image" {
  description = "Grafana image used to visualize temporary test metrics."
  type        = string
  default     = "grafana/grafana:12.3.4"
}

variable "grafana_allowed_cidr" {
  description = "Single public CIDR allowed to access the temporary Grafana task."
  type        = string
  default     = "127.0.0.1/32"

  validation {
    condition = can(cidrhost(var.grafana_allowed_cidr, 0)) && can(
      regex("^[0-9.]+/[0-9]+$", var.grafana_allowed_cidr)
    )
    error_message = "grafana_allowed_cidr must be a valid IPv4 CIDR."
  }
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

variable "k6_volume_init_image" {
  description = "Alpine image used to initialize the shared k6 results volume."
  type        = string
  default     = "public.ecr.aws/docker/library/alpine:3.22.2"
}

variable "aws_cli_image" {
  description = "Official AWS CLI image used to archive k6 result files."
  type        = string
  default     = "public.ecr.aws/aws-cli/aws-cli:2.36.3"
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
  description = "TCP port exposed by the application container."
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
  description = "Fargate CPU units for one application task."
  type        = number
  default     = 256
}

variable "task_memory" {
  description = "Fargate memory in MiB for one application task."
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
  description = "CIDR block reserved for the temporary performance-test VPC."
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
