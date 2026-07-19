#!/usr/bin/env bash

set -euo pipefail

platform_dir="${1:-infra/performance-test/terraform/platform}"
region="$(terraform -chdir="$platform_dir" output -raw aws_region)"
cluster_arn="$(terraform -chdir="$platform_dir" output -raw ecs_cluster_arn)"
task_definition_arn="$(terraform -chdir="$platform_dir" output -raw k6_task_definition_arn)"
subnet_id="$(terraform -chdir="$platform_dir" output -raw k6_subnet_id)"
security_group_id="$(terraform -chdir="$platform_dir" output -raw k6_security_group_id)"

task_arn="$(aws ecs run-task \
	--region "$region" \
	--cluster "$cluster_arn" \
	--task-definition "$task_definition_arn" \
	--launch-type FARGATE \
	--network-configuration \
	"awsvpcConfiguration={subnets=[${subnet_id}],securityGroups=[${security_group_id}],assignPublicIp=ENABLED}" \
	--query 'tasks[0].taskArn' \
	--output text)"

if [[ -z "$task_arn" || "$task_arn" == "None" ]]; then
	echo "Failed to start the k6 smoke task." >&2
	exit 1
fi

echo "Waiting for k6 smoke task: ${task_arn}"
aws ecs wait tasks-stopped --region "$region" --cluster "$cluster_arn" --tasks "$task_arn"

exit_code="$(aws ecs describe-tasks \
	--region "$region" \
	--cluster "$cluster_arn" \
	--tasks "$task_arn" \
	--query 'tasks[0].containers[?name==`k6`].exitCode | [0]' \
	--output text)"

if [[ "$exit_code" != "0" ]]; then
	stop_reason="$(aws ecs describe-tasks \
		--region "$region" \
		--cluster "$cluster_arn" \
		--tasks "$task_arn" \
		--query 'tasks[0].stoppedReason' \
		--output text)"
	echo "k6 smoke task failed with exit code ${exit_code}: ${stop_reason}" >&2
	exit 1
fi

echo "k6 smoke task passed."
