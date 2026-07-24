locals {
  grafana_datasources = <<-YAML
    apiVersion: 1
    datasources:
      - name: Prometheus
        uid: prometheus
        type: prometheus
        access: proxy
        url: http://prometheus.${aws_service_discovery_private_dns_namespace.this.name}:9090
        isDefault: true
        editable: false
      - name: CloudWatch
        uid: cloudwatch
        type: cloudwatch
        access: proxy
        editable: false
        jsonData:
          authType: default
          defaultRegion: ${var.aws_region}
  YAML

  grafana_dashboard_provider = <<-YAML
    apiVersion: 1
    providers:
      - name: performance-test
        orgId: 1
        folder: Performance Test
        type: file
        disableDeletion: true
        editable: false
        options:
          path: /tmp/dashboards
  YAML

  grafana_performance_dashboard = templatefile("${path.module}/../../grafana/performance-overview.json.tftpl", {
    app_service_name        = aws_ecs_service.app.name
    aws_region              = var.aws_region
    cluster_name            = aws_ecs_cluster.this.name
    load_balancer_dimension = aws_lb.app.arn_suffix
    target_group_dimension  = aws_lb_target_group.app.arn_suffix
    test_run_id             = var.test_run_id
  })
}

resource "aws_service_discovery_service" "grafana" {
  name = "grafana"

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

resource "aws_cloudwatch_log_group" "grafana" {
  name              = "/llv/performance-test/${var.test_run_id}/grafana"
  retention_in_days = var.log_retention_days
}

resource "aws_ecs_task_definition" "grafana" {
  family                   = "${local.name_prefix}-grafana"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.grafana_task.arn

  container_definitions = jsonencode([
    {
      name      = "grafana"
      image     = var.grafana_image
      essential = true
      environmentFiles = [
        {
          type  = "s3"
          value = "${aws_s3_bucket.environment_files.arn}/${local.environment_file_keys.grafana}"
        }
      ]
      environment = [
        {
          name  = "GF_AUTH_ANONYMOUS_ENABLED"
          value = "false"
        },
        {
          name  = "GF_DASHBOARDS_DEFAULT_HOME_DASHBOARD_PATH"
          value = "/tmp/dashboards/performance-overview.json"
        },
        {
          name  = "GF_PATHS_PROVISIONING"
          value = "/tmp/provisioning"
        },
        {
          name  = "GF_USERS_ALLOW_SIGN_UP"
          value = "false"
        },
        {
          name  = "GRAFANA_DASHBOARD_BASE64"
          value = base64encode(local.grafana_performance_dashboard)
        },
        {
          name  = "GRAFANA_DASHBOARD_PROVIDER_BASE64"
          value = base64encode(local.grafana_dashboard_provider)
        },
        {
          name  = "GRAFANA_DATASOURCES_BASE64"
          value = base64encode(local.grafana_datasources)
        }
      ]
      entryPoint = ["/bin/sh", "-c"]
      command = [
        <<-EOT
          set -eu
          mkdir -p /tmp/provisioning/datasources /tmp/provisioning/dashboards /tmp/dashboards
          printf '%s' "$GRAFANA_DATASOURCES_BASE64" | base64 -d > /tmp/provisioning/datasources/default.yml
          printf '%s' "$GRAFANA_DASHBOARD_PROVIDER_BASE64" | base64 -d > /tmp/provisioning/dashboards/default.yml
          printf '%s' "$GRAFANA_DASHBOARD_BASE64" | base64 -d > /tmp/dashboards/performance-overview.json
          exec /run.sh
        EOT
      ]
      portMappings = [
        {
          containerPort = 3000
          hostPort      = 3000
          protocol      = "tcp"
        }
      ]
      healthCheck = {
        command     = ["CMD-SHELL", "wget -qO- http://127.0.0.1:3000/api/health >/dev/null || exit 1"]
        interval    = 10
        timeout     = 5
        retries     = 5
        startPeriod = 30
      }
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.grafana.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  depends_on = [
    aws_iam_role_policy.grafana_cloudwatch_read,
    aws_iam_role_policy_attachment.task_execution,
    aws_iam_role_policy.task_execution_environment_file,
  ]
}

resource "aws_ecs_service" "grafana" {
  name                               = "${local.name_prefix}-grafana"
  cluster                            = aws_ecs_cluster.this.id
  task_definition                    = aws_ecs_task_definition.grafana.arn
  desired_count                      = 1
  launch_type                        = "FARGATE"
  wait_for_steady_state              = true
  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100

  network_configuration {
    subnets          = [for subnet in aws_subnet.public : subnet.id]
    security_groups  = [aws_security_group.grafana.id]
    assign_public_ip = true
  }

  service_registries {
    registry_arn = aws_service_discovery_service.grafana.arn
  }
}
