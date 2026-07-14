variable "aws_region" {
  description = "AWS region for the temporary test environment."
  type        = string
}

variable "deployment_role_arn" {
  description = "Optional IAM role Terraform assumes to provision the temporary test environment."
  type        = string
  default     = null
  nullable    = true
}

variable "test_run_id" {
  description = "Lowercase identifier shared by all resources created for one test run."
  type        = string

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{2,19}$", var.test_run_id))
    error_message = "test_run_id must start with a lowercase letter and contain 3 to 20 lowercase letters, numbers, or hyphens."
  }
}

variable "app_image" {
  description = "Container image used to verify the ECS and ALB deployment path."
  type        = string
  default     = "public.ecr.aws/docker/library/nginx:1.27-alpine"
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
