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

  volume {
    name = "k6-scenario"
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
        },
        {
          containerName = "k6-scenario-download"
          condition     = "SUCCESS"
        }
      ]
      environmentFiles = [
        {
          type  = "s3"
          value = "${aws_s3_bucket.environment_files.arn}/${local.environment_file_keys.k6}"
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
          name  = "K6_PROMETHEUS_RW_SERVER_URL"
          value = "http://prometheus.${aws_service_discovery_private_dns_namespace.this.name}:9090/api/v1/write"
        },
        {
          name  = "K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM"
          value = "true"
        },
        {
          name  = "TEST_RUN_ID"
          value = "replace-at-runtime"
        },
        {
          name  = "K6_SCENARIO_NAME"
          value = "replace-at-runtime"
        },
        {
          name  = "K6_SCRIPT_SHA256"
          value = "replace-at-runtime"
        },
        {
          name  = "K6_SCENARIO_KEY"
          value = local.k6_scenario_key
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
        },
        {
          sourceVolume  = "k6-scenario"
          containerPath = "/scenario"
          readOnly      = true
        }
      ]
      entryPoint = ["/bin/sh", "-c"]
      command = [
        <<-EOT
          set -eu
          printf '{"test_run_id":"%s","scenario":"%s","script_key":"%s","script_sha256":"%s","git_sha":"%s","app_task_cpu":%s,"app_task_memory_mib":%s,"started_at":"%s"}\n' \
            "$TEST_RUN_ID" "$K6_SCENARIO_NAME" "$K6_SCENARIO_KEY" "$K6_SCRIPT_SHA256" "$GIT_SHA" "$APP_TASK_CPU" "$APP_TASK_MEMORY" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
            > /results/context.json
          exec k6 run \
            --summary-export=/results/summary.json \
            --out json=/results/metrics.json \
            --out experimental-prometheus-rw \
            --tag testid="$TEST_RUN_ID" \
            /scenario/scenario.js
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
      name      = "k6-scenario-download"
      image     = var.aws_cli_image
      essential = false
      environment = [
        {
          name  = "AWS_REGION"
          value = var.aws_region
        },
        {
          name  = "K6_SCENARIO_BUCKET"
          value = aws_s3_bucket.environment_files.id
        },
        {
          name  = "K6_SCENARIO_KEY"
          value = local.k6_scenario_key
        }
      ]
      mountPoints = [
        {
          sourceVolume  = "k6-scenario"
          containerPath = "/scenario"
          readOnly      = false
        }
      ]
      entryPoint = ["/bin/sh", "-c"]
      command = [
        "set -eu; aws s3 cp \"s3://$K6_SCENARIO_BUCKET/$K6_SCENARIO_KEY\" /scenario/scenario.js --only-show-errors"
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.k6.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "scenario-download"
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
          value = "replace-at-runtime"
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
            "s3://$RESULT_BUCKET/test-sessions/${var.test_run_id}/runs/$TEST_RUN_ID/k6/$execution_time/" \
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
    aws_iam_role_policy.k6_scenario_read,
    aws_iam_role_policy_attachment.task_execution,
    aws_iam_role_policy.task_execution_environment_file,
  ]
}
