resource "aws_cloudwatch_log_group" "wiremock_journal" {
  name              = "/llv/performance-test/${var.test_run_id}/wiremock-journal"
  retention_in_days = var.log_retention_days
}

resource "aws_ecs_task_definition" "wiremock_journal" {
  family                   = "${local.name_prefix}-wiremock-journal"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.task_execution.arn

  container_definitions = jsonencode([
    {
      name      = "wiremock-journal"
      image     = var.mock_init_image
      essential = true
      environment = [
        {
          name  = "EXPECTED_BEDROCK_ATTEMPTS"
          value = "replace-at-runtime"
        }
      ]
      entryPoint = ["/bin/sh", "-c"]
      command = [
        <<-EOT
          set -eu
          mock_url="http://mock.${aws_service_discovery_private_dns_namespace.this.name}:8080/__admin/requests/count"
          response="$(curl --fail --silent --show-error --header 'Content-Type: application/json' \
            --data '{"method":"POST","urlPathPattern":"/model/.+/converse"}' "$mock_url")"
          count="$(printf '%s' "$response" | sed -n 's/.*"count"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p')"
          if [ -z "$count" ]; then
            echo "WireMock request count response did not contain a count: $response" >&2
            exit 1
          fi
          echo "Bedrock mock HTTP attempts: $count (expected: $EXPECTED_BEDROCK_ATTEMPTS)"
          [ "$count" = "$EXPECTED_BEDROCK_ATTEMPTS" ]
        EOT
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.wiremock_journal.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  depends_on = [aws_iam_role_policy_attachment.task_execution]
}
