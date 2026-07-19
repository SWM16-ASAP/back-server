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
