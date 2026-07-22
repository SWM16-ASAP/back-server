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
  task_role_arn            = aws_iam_role.k6_task.arn

  volume {
    name = "k6-results"
  }

  container_definitions = jsonencode([
    {
      name      = "k6-results-init"
      image     = var.k6_volume_init_image
      essential = false
      user      = "0"
      mountPoints = [
        {
          sourceVolume  = "k6-results"
          containerPath = "/results"
          readOnly      = false
        }
      ]
      entryPoint = ["/bin/sh", "-c"]
      command    = ["chown 12345:12345 /results"]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.k6.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "results-init"
        }
      }
    },
    {
      name      = "k6"
      image     = var.k6_image
      essential = false
      dependsOn = [
        {
          containerName = "k6-results-init"
          condition     = "SUCCESS"
        }
      ]
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
        },
        {
          name  = "TEST_RUN_ID"
          value = var.test_run_id
        },
        {
          name  = "GIT_SHA"
          value = var.app_image_tag
        },
        {
          name  = "APP_TASK_CPU"
          value = tostring(var.task_cpu)
        },
        {
          name  = "APP_TASK_MEMORY"
          value = tostring(var.task_memory)
        }
      ]
      mountPoints = [
        {
          sourceVolume  = "k6-results"
          containerPath = "/results"
          readOnly      = false
        }
      ]
      entryPoint = ["/bin/sh", "-c"]
      command = [
        <<-EOT
          set -eu
          printf '%s' "$K6_SCRIPT_BASE64" | base64 -d > /tmp/smoke.js
          printf '{"test_run_id":"%s","git_sha":"%s","app_task_cpu":%s,"app_task_memory_mib":%s,"started_at":"%s"}\n' \
            "$TEST_RUN_ID" "$GIT_SHA" "$APP_TASK_CPU" "$APP_TASK_MEMORY" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
            > /results/context.json
          exec k6 run \
            --summary-export=/results/summary.json \
            --out json=/results/metrics.json \
            /tmp/smoke.js
        EOT
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.k6.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    },
    {
      name      = "result-uploader"
      image     = var.aws_cli_image
      essential = true
      dependsOn = [
        {
          containerName = "k6"
          condition     = "COMPLETE"
        }
      ]
      environment = [
        {
          name  = "AWS_REGION"
          value = var.aws_region
        },
        {
          name  = "RESULT_BUCKET"
          value = aws_s3_bucket.results.id
        },
        {
          name  = "TEST_RUN_ID"
          value = var.test_run_id
        }
      ]
      mountPoints = [
        {
          sourceVolume  = "k6-results"
          containerPath = "/results"
          readOnly      = true
        }
      ]
      entryPoint = ["/bin/sh", "-c"]
      command = [
        <<-EOT
          set -eu
          execution_time="$(date -u +%Y%m%dT%H%M%SZ)"
          aws s3 cp /results \
            "s3://$RESULT_BUCKET/test-runs/$TEST_RUN_ID/k6/$execution_time/" \
            --recursive \
            --only-show-errors
        EOT
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.k6.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "result-uploader"
        }
      }
    }
  ])

  depends_on = [
    aws_iam_role_policy.k6_results_write,
    aws_iam_role_policy_attachment.task_execution,
  ]
}
