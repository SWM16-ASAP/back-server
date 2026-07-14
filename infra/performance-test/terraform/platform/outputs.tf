output "aws_region" {
  description = "AWS region used by this test run."
  value       = var.aws_region
}

output "target_group_arn" {
  description = "Target group inspected by the phase-one verification script."
  value       = aws_lb_target_group.app.arn
}
