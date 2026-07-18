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
