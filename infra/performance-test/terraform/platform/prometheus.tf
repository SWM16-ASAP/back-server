locals {
  prometheus_config = <<-YAML
    global:
      scrape_interval: 15s
      evaluation_interval: 15s
      external_labels:
        test_run_id: ${var.test_run_id}

    scrape_configs:
      - job_name: prometheus
        static_configs:
          - targets: ["127.0.0.1:9090"]

      - job_name: application
        metrics_path: /actuator/prometheus
        authorization:
          type: llvk
          credentials_file: /tmp/import-api-key
        dns_sd_configs:
          - names: ["app.${aws_service_discovery_private_dns_namespace.this.name}"]
            type: A
            port: ${var.container_port}
            refresh_interval: 10s

      - job_name: redis
        static_configs:
          - targets: ["redis.${aws_service_discovery_private_dns_namespace.this.name}:9121"]

      - job_name: mysql
        static_configs:
          - targets: ["mysql.${aws_service_discovery_private_dns_namespace.this.name}:9104"]
  YAML
}

resource "aws_service_discovery_service" "prometheus" {
  name = "prometheus"

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

resource "aws_cloudwatch_log_group" "prometheus" {
  name              = "/llv/performance-test/${var.test_run_id}/prometheus"
  retention_in_days = var.log_retention_days
}

resource "aws_ecs_task_definition" "prometheus" {
  family                   = "${local.name_prefix}-prometheus"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.task_execution.arn

  container_definitions = jsonencode([
    {
      name      = "prometheus"
      image     = var.prometheus_image
      essential = true
      environmentFiles = [
        {
          type  = "s3"
          value = "${aws_s3_bucket.environment_files.arn}/${local.environment_file_keys.prometheus}"
        }
      ]
      environment = [
        {
          name  = "PROMETHEUS_CONFIG_BASE64"
          value = base64encode(local.prometheus_config)
        }
      ]
      entryPoint = ["/bin/sh", "-c"]
      command = [
        <<-EOT
          set -eu
          printf '%s' "$PROMETHEUS_CONFIG_BASE64" | base64 -d > /tmp/prometheus.yml
          printf '%s' "$IMPORT_API_KEY" > /tmp/import-api-key
          if [ "$${ATLAS_PROMETHEUS_ENABLED:-false}" = "true" ]; then
            : "$${ATLAS_PROMETHEUS_GROUP_ID:?ATLAS_PROMETHEUS_GROUP_ID is required when Atlas Prometheus is enabled}"
            : "$${ATLAS_PROMETHEUS_USERNAME:?ATLAS_PROMETHEUS_USERNAME is required when Atlas Prometheus is enabled}"
            : "$${ATLAS_PROMETHEUS_PASSWORD:?ATLAS_PROMETHEUS_PASSWORD is required when Atlas Prometheus is enabled}"
            printf '%s' "$ATLAS_PROMETHEUS_USERNAME" > /tmp/atlas-prometheus-username
            printf '%s' "$ATLAS_PROMETHEUS_PASSWORD" > /tmp/atlas-prometheus-password
            {
              printf '%s\n' '      - job_name: atlas'
              printf '%s\n' '        scrape_interval: 15s'
              printf '%s\n' '        metrics_path: /metrics'
              printf '%s\n' '        scheme: https'
              printf '%s\n' '        basic_auth:'
              printf '%s\n' '          username_file: /tmp/atlas-prometheus-username'
              printf '%s\n' '          password_file: /tmp/atlas-prometheus-password'
              printf '%s\n' '        http_sd_configs:'
              printf '%s\n' "          - url: https://cloud.mongodb.com/prometheus/v1.0/groups/$${ATLAS_PROMETHEUS_GROUP_ID}/discovery"
              printf '%s\n' '            refresh_interval: 60s'
              printf '%s\n' '            basic_auth:'
              printf '%s\n' '              username_file: /tmp/atlas-prometheus-username'
              printf '%s\n' '              password_file: /tmp/atlas-prometheus-password'
            } >> /tmp/prometheus.yml
          fi
          exec /bin/prometheus \
            --config.file=/tmp/prometheus.yml \
            --storage.tsdb.path=/tmp/prometheus-data \
            --storage.tsdb.retention.time=${var.prometheus_retention_time} \
            --storage.tsdb.retention.size=${var.prometheus_retention_size}
        EOT
      ]
      portMappings = [
        {
          containerPort = 9090
          hostPort      = 9090
          protocol      = "tcp"
        }
      ]
      healthCheck = {
        command     = ["CMD-SHELL", "wget -qO- http://127.0.0.1:9090/-/ready >/dev/null || exit 1"]
        interval    = 10
        timeout     = 5
        retries     = 5
        startPeriod = 20
      }
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.prometheus.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  depends_on = [
    aws_iam_role_policy_attachment.task_execution,
    aws_iam_role_policy.task_execution_environment_file,
  ]
}

resource "aws_ecs_service" "prometheus" {
  name                               = "${local.name_prefix}-prometheus"
  cluster                            = aws_ecs_cluster.this.id
  task_definition                    = aws_ecs_task_definition.prometheus.arn
  desired_count                      = 1
  launch_type                        = "FARGATE"
  wait_for_steady_state              = true
  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100

  network_configuration {
    subnets          = [for subnet in aws_subnet.public : subnet.id]
    security_groups  = [aws_security_group.prometheus.id]
    assign_public_ip = true
  }

  service_registries {
    registry_arn = aws_service_discovery_service.prometheus.arn
  }
}
