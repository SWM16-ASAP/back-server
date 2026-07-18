resource "aws_security_group" "alb" {
  name_prefix = "${local.name_prefix}-alb-"
  description = "Allow VPC-internal traffic to the phase-one internal ALB."
  vpc_id      = aws_vpc.this.id

  ingress {
    description = "HTTP from the test VPC"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "HTTP to phase-one tasks"
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
  description = "Allow the phase-one ALB to reach the Fargate task."
  vpc_id      = aws_vpc.this.id

  ingress {
    description     = "HTTP from the internal ALB"
    from_port       = var.container_port
    to_port         = var.container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
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
      security_groups = [aws_security_group.app.id]
    }
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
