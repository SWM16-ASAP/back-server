data "aws_iam_policy_document" "ecs_task_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "task_execution" {
  name_prefix        = "${local.name_prefix}-execution-"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json
}

resource "aws_iam_role_policy_attachment" "task_execution" {
  role       = aws_iam_role.task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "task_execution_environment_file" {
  statement {
    actions   = ["s3:GetBucketLocation"]
    resources = [aws_s3_bucket.environment_files.arn]
  }

  statement {
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.environment_files.arn}/${local.environment_file_key}"]
  }
}

resource "aws_iam_role_policy" "task_execution_environment_file" {
  name   = "environment-file-read"
  role   = aws_iam_role.task_execution.id
  policy = data.aws_iam_policy_document.task_execution_environment_file.json
}

resource "aws_iam_role" "app_task" {
  name_prefix        = "${local.name_prefix}-app-"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json
}

data "aws_iam_policy_document" "app_task_ai_buckets" {
  statement {
    actions   = ["s3:GetBucketLocation"]
    resources = [for bucket in aws_s3_bucket.ai : bucket.arn]
  }

  statement {
    actions   = ["s3:PutObject"]
    resources = ["${aws_s3_bucket.ai["input"].arn}/*"]
  }

  statement {
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.ai["output"].arn]
  }

  statement {
    actions = [
      "s3:DeleteObject",
      "s3:GetObject",
      "s3:PutObject",
    ]
    resources = ["${aws_s3_bucket.ai["output"].arn}/*"]
  }
}

resource "aws_iam_role_policy" "app_task_ai_buckets" {
  name   = "ai-bucket-access"
  role   = aws_iam_role.app_task.id
  policy = data.aws_iam_policy_document.app_task_ai_buckets.json
}

resource "aws_iam_role" "grafana_task" {
  name_prefix        = "${local.name_prefix}-grafana-"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json
}

data "aws_iam_policy_document" "grafana_cloudwatch_read" {
  statement {
    actions = [
      "cloudwatch:DescribeAlarms",
      "cloudwatch:GetMetricData",
      "cloudwatch:GetMetricStatistics",
      "cloudwatch:ListMetrics",
      "ec2:DescribeRegions",
      "ec2:DescribeTags",
    ]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "grafana_cloudwatch_read" {
  name   = "cloudwatch-metrics-read"
  role   = aws_iam_role.grafana_task.id
  policy = data.aws_iam_policy_document.grafana_cloudwatch_read.json
}
