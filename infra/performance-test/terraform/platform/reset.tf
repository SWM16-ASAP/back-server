resource "aws_cloudwatch_log_group" "reset" {
  for_each = toset(["mongo", "redis", "mysql", "mock"])

  name              = "/llv/performance-test/${var.test_run_id}/reset-${each.key}"
  retention_in_days = var.log_retention_days
}

resource "aws_ecs_task_definition" "reset_mongo" {
  family                   = "${local.name_prefix}-reset-mongo"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.task_execution.arn

  container_definitions = jsonencode([
    {
      name      = "reset-mongo"
      image     = var.mongo_reset_image
      essential = true
      environmentFiles = [
        {
          type  = "s3"
          value = "${aws_s3_bucket.environment_files.arn}/${local.environment_file_keys.reset_mongo}"
        }
      ]
      entryPoint = ["/bin/sh", "-c"]
      command = [
        <<-EOT
          set -eu
          if [ "$SPRING_DATA_MONGODB_DATABASE" != "llv_performance_test" ]; then
            echo "Refusing to reset a non-test MongoDB database." >&2
            exit 1
          fi
          mongosh "$SPRING_DATA_MONGODB_URI" --quiet \
            --eval "const database = db.getSiblingDB('$SPRING_DATA_MONGODB_DATABASE'); database.getCollectionNames().filter(name => !name.startsWith('system.')).forEach(name => database.getCollection(name).deleteMany({}));"
        EOT
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.reset["mongo"].name
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

resource "aws_ecs_task_definition" "reset_redis" {
  family                   = "${local.name_prefix}-reset-redis"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.task_execution.arn

  container_definitions = jsonencode([
    {
      name       = "reset-redis"
      image      = var.redis_image
      essential  = true
      entryPoint = ["/bin/sh", "-c"]
      command    = ["set -eu; redis-cli -h redis.${aws_service_discovery_private_dns_namespace.this.name} FLUSHDB"]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.reset["redis"].name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  depends_on = [aws_iam_role_policy_attachment.task_execution]
}

resource "aws_ecs_task_definition" "reset_mysql" {
  family                   = "${local.name_prefix}-reset-mysql"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.task_execution.arn

  container_definitions = jsonencode([
    {
      name      = "reset-mysql"
      image     = var.mysql_image
      essential = true
      environment = [
        {
          name  = "MYSQL_DATABASE"
          value = "llv_performance_test"
        }
      ]
      environmentFiles = [
        {
          type  = "s3"
          value = "${aws_s3_bucket.environment_files.arn}/${local.environment_file_keys.mysql}"
        }
      ]
      entryPoint = ["/bin/sh", "-c"]
      command = [
        <<-EOT
          set -eu
          mysql --host=mysql.${aws_service_discovery_private_dns_namespace.this.name} \
            --user=root --password="$MYSQL_ROOT_PASSWORD" \
            --execute="DROP DATABASE IF EXISTS \`$MYSQL_DATABASE\`; CREATE DATABASE \`$MYSQL_DATABASE\`;"
        EOT
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.reset["mysql"].name
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

resource "aws_ecs_task_definition" "reset_mock" {
  family                   = "${local.name_prefix}-reset-mock"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.task_execution.arn

  container_definitions = jsonencode([
    {
      name      = "reset-mock"
      image     = var.mock_init_image
      essential = true
      environment = [
        {
          name  = "WIREMOCK_MAPPINGS_BASE64"
          value = base64encode(local.wiremock_mappings)
        }
      ]
      entryPoint = ["/bin/sh", "-c"]
      command = [
        "set -eu; mock_url=http://mock.${aws_service_discovery_private_dns_namespace.this.name}:8080/__admin; curl --fail --silent --show-error --request DELETE \"$mock_url/mappings\"; curl --fail --silent --show-error --request DELETE \"$mock_url/requests\"; printf '%s' \"$WIREMOCK_MAPPINGS_BASE64\" | base64 -d | curl --fail --silent --show-error --header 'Content-Type: application/json' --data-binary @- \"$mock_url/mappings/import\""
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.reset["mock"].name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  depends_on = [aws_iam_role_policy_attachment.task_execution]
}
