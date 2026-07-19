resource "aws_cloudwatch_log_group" "k6" {
  name              = "/llv/performance-test/${var.test_run_id}/k6"
  retention_in_days = var.log_retention_days
}

resource "aws_ecs_task_definition" "k6" {
  family                   = "${local.name_prefix}-k6"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.k6_task_cpu
  memory                   = var.k6_task_memory
  execution_role_arn       = aws_iam_role.task_execution.arn

  container_definitions = jsonencode([
    {
      name      = "k6"
      image     = var.k6_image
      essential = true
      environment = [
        {
          name  = "BASE_URL"
          value = "http://${aws_lb.app.dns_name}"
        },
        {
          name  = "HEALTH_PATH"
          value = var.health_check_path
        },
        {
          name  = "K6_SCRIPT_BASE64"
          value = base64encode(local.k6_smoke_script)
        }
      ]
      entryPoint = ["/bin/sh", "-c"]
      command = [
        "set -eu; printf '%s' \"$K6_SCRIPT_BASE64\" | base64 -d > /tmp/smoke.js; exec k6 run /tmp/smoke.js"
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.k6.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  depends_on = [aws_iam_role_policy_attachment.task_execution]
}
