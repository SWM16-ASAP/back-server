locals {
  tempo_config = file("${path.module}/../../otel/tempo.yaml")
  otel_collector_config = templatefile("${path.module}/../../otel/collector.yaml.tftpl", {
    tempo_endpoint = "tempo.${aws_service_discovery_private_dns_namespace.this.name}:4317"
  })
}

resource "aws_service_discovery_service" "tempo" {
  name = "tempo"

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

resource "aws_service_discovery_service" "otel_collector" {
  name = "otel-collector"

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

resource "aws_cloudwatch_log_group" "tempo" {
  name              = "/llv/performance-test/${var.test_run_id}/tempo"
  retention_in_days = var.log_retention_days
}

resource "aws_cloudwatch_log_group" "otel_collector" {
  name              = "/llv/performance-test/${var.test_run_id}/otel-collector"
  retention_in_days = var.log_retention_days
}

resource "aws_ecs_task_definition" "tempo" {
  family                   = "${local.name_prefix}-tempo"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.task_execution.arn

  volume {
    name = "config"
  }

  container_definitions = jsonencode([
    {
      name       = "tempo-config-init"
      image      = var.observability_config_init_image
      essential  = false
      entryPoint = ["/bin/sh", "-c"]
      command = [
        "set -eu; printf '%s' \"$TEMPO_CONFIG_BASE64\" | base64 -d > /config/tempo.yaml"
      ]
      environment = [
        {
          name  = "TEMPO_CONFIG_BASE64"
          value = base64encode(local.tempo_config)
        }
      ]
      mountPoints = [
        {
          sourceVolume  = "config"
          containerPath = "/config"
          readOnly      = false
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.tempo.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "config-init"
        }
      }
    },
    {
      name      = "tempo"
      image     = var.tempo_image
      essential = true
      dependsOn = [
        {
          containerName = "tempo-config-init"
          condition     = "SUCCESS"
        }
      ]
      command = ["-config.file=/config/tempo.yaml"]
      mountPoints = [
        {
          sourceVolume  = "config"
          containerPath = "/config"
          readOnly      = true
        }
      ]
      portMappings = [
        {
          containerPort = 3200
          hostPort      = 3200
          protocol      = "tcp"
        },
        {
          containerPort = 4317
          hostPort      = 4317
          protocol      = "tcp"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.tempo.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  depends_on = [aws_iam_role_policy_attachment.task_execution]
}

resource "aws_ecs_task_definition" "otel_collector" {
  family                   = "${local.name_prefix}-otel-collector"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.task_execution.arn

  volume {
    name = "config"
  }

  container_definitions = jsonencode([
    {
      name       = "otel-collector-config-init"
      image      = var.observability_config_init_image
      essential  = false
      entryPoint = ["/bin/sh", "-c"]
      command = [
        "set -eu; printf '%s' \"$OTELCOL_CONFIG_BASE64\" | base64 -d > /config/collector.yaml"
      ]
      environment = [
        {
          name  = "OTELCOL_CONFIG_BASE64"
          value = base64encode(local.otel_collector_config)
        }
      ]
      mountPoints = [
        {
          sourceVolume  = "config"
          containerPath = "/config"
          readOnly      = false
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.otel_collector.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "config-init"
        }
      }
    },
    {
      name      = "otel-collector"
      image     = var.otel_collector_image
      essential = true
      dependsOn = [
        {
          containerName = "otel-collector-config-init"
          condition     = "SUCCESS"
        }
      ]
      command = ["--config=/config/collector.yaml"]
      mountPoints = [
        {
          sourceVolume  = "config"
          containerPath = "/config"
          readOnly      = true
        }
      ]
      portMappings = [
        {
          containerPort = 4318
          hostPort      = 4318
          protocol      = "tcp"
        },
        {
          containerPort = 13133
          hostPort      = 13133
          protocol      = "tcp"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.otel_collector.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  depends_on = [aws_iam_role_policy_attachment.task_execution]
}

resource "aws_ecs_service" "tempo" {
  name                               = "${local.name_prefix}-tempo"
  cluster                            = aws_ecs_cluster.this.id
  task_definition                    = aws_ecs_task_definition.tempo.arn
  desired_count                      = 1
  launch_type                        = "FARGATE"
  wait_for_steady_state              = true
  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100

  network_configuration {
    subnets          = [for subnet in aws_subnet.public : subnet.id]
    security_groups  = [aws_security_group.tempo.id]
    assign_public_ip = true
  }

  service_registries {
    registry_arn = aws_service_discovery_service.tempo.arn
  }
}

resource "aws_ecs_service" "otel_collector" {
  name                               = "${local.name_prefix}-otel-collector"
  cluster                            = aws_ecs_cluster.this.id
  task_definition                    = aws_ecs_task_definition.otel_collector.arn
  desired_count                      = 1
  launch_type                        = "FARGATE"
  wait_for_steady_state              = true
  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100

  network_configuration {
    subnets          = [for subnet in aws_subnet.public : subnet.id]
    security_groups  = [aws_security_group.otel_collector.id]
    assign_public_ip = true
  }

  service_registries {
    registry_arn = aws_service_discovery_service.otel_collector.arn
  }

  depends_on = [aws_ecs_service.tempo]
}
