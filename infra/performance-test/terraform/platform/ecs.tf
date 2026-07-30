resource "aws_cloudwatch_log_group" "app" {
  name              = "/llv/performance-test/${var.test_run_id}/app"
  retention_in_days = var.log_retention_days
}

resource "aws_ecs_cluster" "this" {
  name = "${local.name_prefix}-cluster"
}

resource "aws_service_discovery_service" "app" {
  name = "app"

  dns_config {
    namespace_id   = aws_service_discovery_private_dns_namespace.this.id
    routing_policy = "MULTIVALUE"

    dns_records {
      ttl  = 10
      type = "A"
    }
  }

  health_check_custom_config {
    failure_threshold = 1
  }
}

resource "aws_ecs_task_definition" "app" {
  family                   = "${local.name_prefix}-app"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.app_task.arn

  volume {
    name = "heap-dumps"
  }

  container_definitions = jsonencode([
    {
      name      = "app"
      image     = "${aws_ecr_repository.app.repository_url}:${var.app_image_tag}"
      essential = false
      environmentFiles = [
        {
          type  = "s3"
          value = "${aws_s3_bucket.environment_files.arn}/${local.environment_file_keys.app}"
        }
      ]
      environment = [
        {
          name  = "JAVA_TOOL_OPTIONS"
          value = "-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/heap-dumps -XX:+ExitOnOutOfMemoryError"
        },
        {
          name  = "HEAP_DUMP_EXIT_MARKER_PATH"
          value = "/heap-dumps/.application-exited"
        },
        {
          name  = "WORD_SINGLE_FLIGHT_ENABLED"
          value = tostring(var.word_single_flight_enabled)
        }
      ]
      mountPoints = [
        {
          sourceVolume  = "heap-dumps"
          containerPath = "/heap-dumps"
          readOnly      = false
        }
      ]
      portMappings = [
        {
          containerPort = var.container_port
          hostPort      = var.container_port
          protocol      = "tcp"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.app.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    },
    {
      name      = "heap-dump-uploader"
      image     = var.aws_cli_image
      essential = true
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
          sourceVolume  = "heap-dumps"
          containerPath = "/heap-dumps"
          readOnly      = true
        }
      ]
      entryPoint = ["/bin/sh", "-c"]
      command = [
        <<-EOT
          set -eu
          marker=/heap-dumps/.application-exited
          while [ ! -f "$marker" ]; do
            sleep 2
          done

          heap_dump="$(find /heap-dumps -maxdepth 1 -type f -name '*.hprof' -print -quit)"
          if [ -n "$heap_dump" ]; then
            uploaded_at="$(date -u +%Y%m%dT%H%M%SZ)"
            aws s3 cp "$heap_dump" \
              "s3://$RESULT_BUCKET/test-sessions/$TEST_RUN_ID/heap-dumps/$uploaded_at/$(basename "$heap_dump")" \
              --only-show-errors
          fi

          exit "$(cat "$marker")"
        EOT
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.app.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "heap-dump-uploader"
        }
      }
    }
  ])

  depends_on = [
    aws_iam_role_policy_attachment.task_execution,
    aws_iam_role_policy.task_execution_environment_file,
    aws_iam_role_policy.app_task_heap_dump_write,
  ]
}

resource "aws_ecs_service" "app" {
  name                               = "${local.name_prefix}-app"
  cluster                            = aws_ecs_cluster.this.id
  task_definition                    = aws_ecs_task_definition.app.arn
  desired_count                      = var.app_desired_count
  launch_type                        = "FARGATE"
  health_check_grace_period_seconds  = var.health_check_grace_period_seconds
  wait_for_steady_state              = true
  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  network_configuration {
    subnets          = [for subnet in aws_subnet.public : subnet.id]
    security_groups  = [aws_security_group.app.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = "app"
    container_port   = var.container_port
  }

  service_registries {
    registry_arn = aws_service_discovery_service.app.arn
  }

  depends_on = [aws_lb_listener.http]
}
