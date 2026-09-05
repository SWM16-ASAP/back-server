locals {
  probe_internal_ports = merge(
    { for name, service in local.dependency_services : name => service.port },
    {
      grafana        = 3000
      mysql_exporter = 9104
      otel_collector = 13133
      prometheus     = 9090
      redis_exporter = 9121
      tempo          = 3200
    }
  )
}

resource "aws_security_group" "alb" {
  name_prefix = "${local.name_prefix}-alb-"
  description = "Allow VPC-internal traffic to the internal ALB."
  vpc_id      = aws_vpc.this.id

  ingress {
    description = "HTTP from the test VPC"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "HTTP to application tasks"
    from_port   = var.container_port
    to_port     = var.container_port
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  tags = {
    Name = "${local.name_prefix}-alb"
  }
}

resource "aws_security_group" "app" {
  name_prefix = "${local.name_prefix}-app-"
  description = "Allow the internal ALB to reach application tasks."
  vpc_id      = aws_vpc.this.id

  ingress {
    description     = "HTTP from the internal ALB"
    from_port       = var.container_port
    to_port         = var.container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  ingress {
    description     = "Application metrics from Prometheus"
    from_port       = var.container_port
    to_port         = var.container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.prometheus.id]
  }

  egress {
    description = "HTTPS for public image retrieval"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "MongoDB Atlas database traffic"
    from_port   = 27015
    to_port     = 27017
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "DNS to the VPC resolver"
    from_port   = 53
    to_port     = 53
    protocol    = "udp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "TCP DNS fallback to the VPC resolver"
    from_port   = 53
    to_port     = 53
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  dynamic "egress" {
    for_each = local.dependency_services

    content {
      description = "${egress.key} dependency traffic"
      from_port   = egress.value.port
      to_port     = egress.value.port
      protocol    = "tcp"
      cidr_blocks = [var.vpc_cidr]
    }
  }

  egress {
    description = "OTLP traces to the OpenTelemetry Collector"
    from_port   = 4318
    to_port     = 4318
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  tags = {
    Name = "${local.name_prefix}-app"
  }
}

resource "aws_security_group" "dependencies" {
  name_prefix = "${local.name_prefix}-dependencies-"
  description = "Allow only application tasks to reach temporary dependency services."
  vpc_id      = aws_vpc.this.id

  dynamic "ingress" {
    for_each = local.dependency_services

    content {
      description     = "${ingress.key} from application tasks"
      from_port       = ingress.value.port
      to_port         = ingress.value.port
      protocol        = "tcp"
      security_groups = [aws_security_group.app.id, aws_security_group.probe.id]
    }
  }

  ingress {
    description     = "Redis metrics from verification and monitoring tasks"
    from_port       = 9121
    to_port         = 9121
    protocol        = "tcp"
    security_groups = [aws_security_group.probe.id, aws_security_group.prometheus.id]
  }

  ingress {
    description     = "MySQL metrics from verification and monitoring tasks"
    from_port       = 9104
    to_port         = 9104
    protocol        = "tcp"
    security_groups = [aws_security_group.probe.id, aws_security_group.prometheus.id]
  }

  egress {
    description = "HTTPS for image pulls and AWS APIs"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "DNS to the VPC resolver"
    from_port   = 53
    to_port     = 53
    protocol    = "udp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "TCP DNS fallback to the VPC resolver"
    from_port   = 53
    to_port     = 53
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  tags = {
    Name = "${local.name_prefix}-dependencies"
  }
}

resource "aws_security_group" "prometheus" {
  name_prefix = "${local.name_prefix}-prometheus-"
  description = "Allow internal metric collection and verification."
  vpc_id      = aws_vpc.this.id

  ingress {
    description = "Prometheus API from verification tasks in the test VPC"
    from_port   = 9090
    to_port     = 9090
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "Application and exporter metrics"
    from_port   = 1
    to_port     = 65535
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "HTTPS for image pulls and AWS APIs"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "DNS to the VPC resolver"
    from_port   = 53
    to_port     = 53
    protocol    = "udp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "TCP DNS fallback to the VPC resolver"
    from_port   = 53
    to_port     = 53
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  tags = {
    Name = "${local.name_prefix}-prometheus"
  }
}

resource "aws_security_group" "grafana" {
  name_prefix = "${local.name_prefix}-grafana-"
  description = "Allow temporary Grafana access and datasource queries."
  vpc_id      = aws_vpc.this.id

  ingress {
    description = "Grafana from the configured viewer CIDR"
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = [var.grafana_allowed_cidr]
  }

  ingress {
    description = "Grafana health check from the test VPC"
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "Prometheus datasource"
    from_port   = 9090
    to_port     = 9090
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "Tempo datasource"
    from_port   = 3200
    to_port     = 3200
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "HTTPS for image pulls and CloudWatch APIs"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "DNS to the VPC resolver"
    from_port   = 53
    to_port     = 53
    protocol    = "udp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "TCP DNS fallback to the VPC resolver"
    from_port   = 53
    to_port     = 53
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  tags = {
    Name = "${local.name_prefix}-grafana"
  }
}

resource "aws_security_group" "otel_collector" {
  name_prefix = "${local.name_prefix}-otel-collector-"
  description = "Allow application traces and Collector health verification."
  vpc_id      = aws_vpc.this.id

  ingress {
    description     = "OTLP HTTP traces from application tasks"
    from_port       = 4318
    to_port         = 4318
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  ingress {
    description     = "Collector health from verification tasks"
    from_port       = 13133
    to_port         = 13133
    protocol        = "tcp"
    security_groups = [aws_security_group.probe.id]
  }

  egress {
    description = "OTLP traces to Tempo"
    from_port   = 4317
    to_port     = 4317
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "HTTPS for image pulls and AWS APIs"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "DNS to the VPC resolver"
    from_port   = 53
    to_port     = 53
    protocol    = "udp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "TCP DNS fallback to the VPC resolver"
    from_port   = 53
    to_port     = 53
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  tags = {
    Name = "${local.name_prefix}-otel-collector"
  }
}

resource "aws_security_group" "tempo" {
  name_prefix = "${local.name_prefix}-tempo-"
  description = "Allow Collector ingestion and Tempo API queries."
  vpc_id      = aws_vpc.this.id

  ingress {
    description     = "OTLP gRPC traces from the OpenTelemetry Collector"
    from_port       = 4317
    to_port         = 4317
    protocol        = "tcp"
    security_groups = [aws_security_group.otel_collector.id]
  }

  ingress {
    description = "Tempo API from Grafana and verification tasks"
    from_port   = 3200
    to_port     = 3200
    protocol    = "tcp"
    security_groups = [
      aws_security_group.grafana.id,
      aws_security_group.probe.id,
    ]
  }

  egress {
    description = "HTTPS for image pulls and AWS APIs"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "DNS to the VPC resolver"
    from_port   = 53
    to_port     = 53
    protocol    = "udp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "TCP DNS fallback to the VPC resolver"
    from_port   = 53
    to_port     = 53
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  tags = {
    Name = "${local.name_prefix}-tempo"
  }
}

resource "aws_security_group" "probe" {
  name_prefix = "${local.name_prefix}-probe-"
  description = "Allow the dependency probe to reach test services and monitoring endpoints."
  vpc_id      = aws_vpc.this.id

  dynamic "egress" {
    for_each = local.probe_internal_ports

    content {
      description = "${egress.key} probe traffic"
      from_port   = egress.value
      to_port     = egress.value
      protocol    = "tcp"
      cidr_blocks = [var.vpc_cidr]
    }
  }

  egress {
    description = "MongoDB Atlas database traffic"
    from_port   = 27015
    to_port     = 27017
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "HTTPS for image pulls and AWS APIs"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "DNS to the VPC resolver"
    from_port   = 53
    to_port     = 53
    protocol    = "udp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "TCP DNS fallback to the VPC resolver"
    from_port   = 53
    to_port     = 53
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  tags = {
    Name = "${local.name_prefix}-probe"
  }
}

resource "aws_security_group" "k6" {
  name_prefix = "${local.name_prefix}-k6-"
  description = "Allow the one-off k6 task to reach the internal ALB."
  vpc_id      = aws_vpc.this.id

  egress {
    description = "HTTP to the internal ALB"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "Prometheus remote write in the test VPC"
    from_port   = 9090
    to_port     = 9090
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "HTTPS for image pulls and AWS APIs"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "DNS to the VPC resolver"
    from_port   = 53
    to_port     = 53
    protocol    = "udp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "TCP DNS fallback to the VPC resolver"
    from_port   = 53
    to_port     = 53
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  tags = {
    Name = "${local.name_prefix}-k6"
  }
}
