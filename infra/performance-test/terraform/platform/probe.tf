resource "aws_cloudwatch_log_group" "dependency_probe" {
  name              = "/llv/performance-test/${var.test_run_id}/dependency-probe"
  retention_in_days = var.log_retention_days
}

resource "aws_ecs_task_definition" "dependency_probe" {
  family                   = "${local.name_prefix}-dependency-probe"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.task_execution.arn

  container_definitions = jsonencode([
    {
      name       = "dependency-probe"
      image      = var.dependency_probe_image
      essential  = true
      entryPoint = ["/bin/sh", "-c"]
      command = [
        <<-EOT
          set -eu
          probe_tcp() {
            name="$1"
            host="$2"
            port="$3"
            for attempt in $(seq 1 30); do
              if nc -z -w 2 "$host" "$port"; then
                echo "$name connection passed: $host:$port"
                return 0
              fi
              sleep 2
            done
            echo "$name connection failed: $host:$port" >&2
            return 1
          }

          probe_tcp "Redis" "redis.${aws_service_discovery_private_dns_namespace.this.name}" 6379
          probe_tcp "MySQL" "mysql.${aws_service_discovery_private_dns_namespace.this.name}" 3306

          probe_metric() {
            name="$1"
            url="$2"
            metric="$3"
            for attempt in $(seq 1 30); do
              if wget -qO /tmp/metrics "$url" && grep -q "^$metric 1" /tmp/metrics; then
                echo "$name metrics passed: $url"
                return 0
              fi
              sleep 2
            done
            echo "$name metrics failed: $url" >&2
            return 1
          }

          probe_metric "Redis exporter" \
            "http://redis.${aws_service_discovery_private_dns_namespace.this.name}:9121/metrics" \
            "redis_up"
          probe_metric "MySQL exporter" \
            "http://mysql.${aws_service_discovery_private_dns_namespace.this.name}:9104/metrics" \
            "mysql_up"

          prometheus_targets_url="http://prometheus.${aws_service_discovery_private_dns_namespace.this.name}:9090/api/v1/targets"
          for attempt in $(seq 1 30); do
            if wget -qO /tmp/prometheus-targets.json "$prometheus_targets_url"; then
              healthy_targets="$(grep -o '\"health\":\"up\"' /tmp/prometheus-targets.json | wc -l | tr -d ' ' || true)"
              unhealthy_targets="$(grep -o '\"health\":\"down\"' /tmp/prometheus-targets.json | wc -l | tr -d ' ' || true)"
              if [ "$healthy_targets" -ge 5 ] && [ "$unhealthy_targets" -eq 0 ]; then
                echo "Prometheus targets passed: $healthy_targets healthy targets"
                break
              fi
            fi
            if [ "$attempt" -eq 30 ]; then
              echo "Prometheus targets failed: $prometheus_targets_url" >&2
              cat /tmp/prometheus-targets.json >&2 || true
              exit 1
            fi
            sleep 2
          done

          grafana_health_url="http://grafana.${aws_service_discovery_private_dns_namespace.this.name}:3000/api/health"
          for attempt in $(seq 1 30); do
            if wget -qO /tmp/grafana-health.json "$grafana_health_url" && \
                grep -q '"database":"ok"' /tmp/grafana-health.json; then
              echo "Grafana health passed: $grafana_health_url"
              break
            fi
            if [ "$attempt" -eq 30 ]; then
              echo "Grafana health failed: $grafana_health_url" >&2
              exit 1
            fi
            sleep 2
          done

          mock_url="http://mock.${aws_service_discovery_private_dns_namespace.this.name}:8080/__admin/mappings"
          for attempt in $(seq 1 30); do
            if wget -qO /tmp/mappings.json "$mock_url" && \
                grep -q 'bedrock-converse' /tmp/mappings.json && \
                grep -q 'discord-' /tmp/mappings.json; then
              echo "WireMock mappings passed: $mock_url"
              exit 0
            fi
            sleep 2
          done
          echo "WireMock mappings failed: $mock_url" >&2
          exit 1
        EOT
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.dependency_probe.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  depends_on = [aws_iam_role_policy_attachment.task_execution]
}
