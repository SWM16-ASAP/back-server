resource "aws_service_discovery_private_dns_namespace" "this" {
  name        = "${var.test_run_id}.llvpt.local"
  description = "Private service discovery namespace for one performance test run."
  vpc         = aws_vpc.this.id
}

resource "aws_service_discovery_service" "dependency" {
  for_each = local.dependency_services

  name = each.key

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

resource "aws_cloudwatch_log_group" "dependency" {
  for_each = local.dependency_services

  name              = "/llv/performance-test/${var.test_run_id}/${each.key}"
  retention_in_days = var.log_retention_days
}

resource "aws_ecs_task_definition" "dependency" {
  for_each = local.dependency_services

  family                   = "${local.name_prefix}-${each.key}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = each.value.cpu
  memory                   = each.value.memory
  execution_role_arn       = aws_iam_role.task_execution.arn

  container_definitions = jsonencode(concat(
    [
      merge(
        {
          name        = each.key
          image       = each.value.image
          essential   = true
          environment = each.value.environment
          portMappings = [
            {
              containerPort = each.value.port
              hostPort      = each.value.port
              protocol      = "tcp"
            }
          ]
          logConfiguration = {
            logDriver = "awslogs"
            options = {
              awslogs-group         = aws_cloudwatch_log_group.dependency[each.key].name
              awslogs-region        = var.aws_region
              awslogs-stream-prefix = "ecs"
            }
          }
        },
        each.value.health_check == null ? {} : { healthCheck = each.value.health_check },
        each.key == "mysql" ? {
          environmentFiles = [
            {
              type  = "s3"
              value = "${aws_s3_bucket.environment_files.arn}/${local.environment_file_keys.mysql}"
            }
          ]
        } : {}
      )
    ],
    each.key == "redis" ? [
      {
        name      = "redis-exporter"
        image     = var.redis_exporter_image
        essential = true
        dependsOn = [
          {
            containerName = "redis"
            condition     = "HEALTHY"
          }
        ]
        environment = [
          {
            name  = "REDIS_ADDR"
            value = "redis://127.0.0.1:6379"
          }
        ]
        portMappings = [
          {
            containerPort = 9121
            hostPort      = 9121
            protocol      = "tcp"
          }
        ]
        logConfiguration = {
          logDriver = "awslogs"
          options = {
            awslogs-group         = aws_cloudwatch_log_group.dependency[each.key].name
            awslogs-region        = var.aws_region
            awslogs-stream-prefix = "redis-exporter"
          }
        }
      }
    ] : [],
    each.key == "mysql" ? [
      {
        name      = "mysql-exporter-init"
        image     = var.mysql_image
        essential = false
        dependsOn = [
          {
            containerName = "mysql"
            condition     = "HEALTHY"
          }
        ]
        environmentFiles = [
          {
            type  = "s3"
            value = "${aws_s3_bucket.environment_files.arn}/${local.environment_file_keys.mysql_exporter_init}"
          }
        ]
        entryPoint = ["/bin/sh", "-c"]
        command = [
          <<-EOT
            set -eu
            mysql --host=127.0.0.1 --user=root --password="$MYSQL_ROOT_PASSWORD" <<SQL
            CREATE USER IF NOT EXISTS 'exporter'@'%' IDENTIFIED BY '$MYSQLD_EXPORTER_PASSWORD';
            GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'%';
            SQL
          EOT
        ]
        logConfiguration = {
          logDriver = "awslogs"
          options = {
            awslogs-group         = aws_cloudwatch_log_group.dependency[each.key].name
            awslogs-region        = var.aws_region
            awslogs-stream-prefix = "mysql-exporter-init"
          }
        }
      }
    ] : [],
    each.key == "mysql" ? [
      {
        name      = "mysql-exporter"
        image     = var.mysql_exporter_image
        essential = true
        dependsOn = [
          {
            containerName = "mysql-exporter-init"
            condition     = "SUCCESS"
          }
        ]
        environmentFiles = [
          {
            type  = "s3"
            value = "${aws_s3_bucket.environment_files.arn}/${local.environment_file_keys.mysql_exporter}"
          }
        ]
        command = [
          "--mysqld.address=127.0.0.1:3306",
          "--mysqld.username=exporter"
        ]
        portMappings = [
          {
            containerPort = 9104
            hostPort      = 9104
            protocol      = "tcp"
          }
        ]
        logConfiguration = {
          logDriver = "awslogs"
          options = {
            awslogs-group         = aws_cloudwatch_log_group.dependency[each.key].name
            awslogs-region        = var.aws_region
            awslogs-stream-prefix = "mysql-exporter"
          }
        }
      }
    ] : [],
    each.key == "mock" ? [
      {
        name      = "mock-init"
        image     = var.mock_init_image
        essential = false
        dependsOn = [
          {
            containerName = "mock"
            condition     = "START"
          }
        ]
        environment = [
          {
            name  = "WIREMOCK_MAPPINGS_BASE64"
            value = base64encode(local.wiremock_mappings)
          }
        ]
        entryPoint = ["/bin/sh", "-c"]
        command = [
          "until curl --fail --silent http://localhost:8080/__admin/mappings >/dev/null; do sleep 1; done; printf '%s' \"$WIREMOCK_MAPPINGS_BASE64\" | base64 -d | curl --fail --silent --show-error --header 'Content-Type: application/json' --data-binary @- http://localhost:8080/__admin/mappings/import"
        ]
        logConfiguration = {
          logDriver = "awslogs"
          options = {
            awslogs-group         = aws_cloudwatch_log_group.dependency[each.key].name
            awslogs-region        = var.aws_region
            awslogs-stream-prefix = "mock-init"
          }
        }
      }
    ] : []
  ))

  depends_on = [
    aws_iam_role_policy_attachment.task_execution,
    aws_iam_role_policy.task_execution_environment_file,
  ]
}

resource "aws_ecs_service" "dependency" {
  for_each = local.dependency_services

  name                               = "${local.name_prefix}-${each.key}"
  cluster                            = aws_ecs_cluster.this.id
  task_definition                    = aws_ecs_task_definition.dependency[each.key].arn
  desired_count                      = 1
  launch_type                        = "FARGATE"
  wait_for_steady_state              = true
  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100

  network_configuration {
    subnets          = [for subnet in aws_subnet.public : subnet.id]
    security_groups  = [aws_security_group.dependencies.id]
    assign_public_ip = true
  }

  service_registries {
    registry_arn = aws_service_discovery_service.dependency[each.key].arn
  }
}
