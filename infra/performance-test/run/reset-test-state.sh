#!/usr/bin/env bash

set -euo pipefail

platform_dir="${1:-infra/performance-test/terraform/platform}"
region="$(terraform -chdir="$platform_dir" output -raw aws_region)"
cluster_arn="$(terraform -chdir="$platform_dir" output -raw ecs_cluster_arn)"
subnet_id="$(terraform -chdir="$platform_dir" output -raw k6_subnet_id)"
security_group_id="$(terraform -chdir="$platform_dir" output -raw dependency_probe_security_group_id)"
k6_task_definition_arn="$(terraform -chdir="$platform_dir" output -raw k6_task_definition_arn)"

k6_family="$(aws ecs describe-task-definition \
	--region "$region" \
	--task-definition "$k6_task_definition_arn" \
	--query 'taskDefinition.family' \
	--output text)"
running_k6_tasks="$(aws ecs list-tasks \
	--region "$region" \
	--cluster "$cluster_arn" \
	--family "$k6_family" \
	--desired-status RUNNING \
	--query 'taskArns' \
	--output text)"

if [[ -n "$running_k6_tasks" && "$running_k6_tasks" != "None" ]]; then
	echo "A k6 task is running. Wait for it to finish before resetting state." >&2
	exit 1
fi

run_reset_task() {
	local name="$1"
	local task_definition_arn="$2"
	local task_arn
	local exit_code

	task_arn="$(aws ecs run-task \
		--region "$region" \
		--cluster "$cluster_arn" \
		--task-definition "$task_definition_arn" \
		--launch-type FARGATE \
		--network-configuration "awsvpcConfiguration={subnets=[${subnet_id}],securityGroups=[${security_group_id}],assignPublicIp=ENABLED}" \
		--query 'tasks[0].taskArn' \
		--output text)"

	if [[ -z "$task_arn" || "$task_arn" == "None" ]]; then
		echo "Failed to start ${name} reset task." >&2
		exit 1
	fi

	echo "Waiting for ${name} reset task: ${task_arn}"
	aws ecs wait tasks-stopped --region "$region" --cluster "$cluster_arn" --tasks "$task_arn"
	exit_code="$(aws ecs describe-tasks \
		--region "$region" \
		--cluster "$cluster_arn" \
		--tasks "$task_arn" \
		--query 'tasks[0].containers[0].exitCode' \
		--output text)"

	if [[ "$exit_code" != "0" ]]; then
		echo "${name} reset task failed with exit code ${exit_code}." >&2
		exit 1
	fi
}

for name in mongo redis mysql mock; do
	task_definition_arn="$(terraform -chdir="$platform_dir" output -raw "reset_${name}_task_definition_arn")"
	run_reset_task "$name" "$task_definition_arn"
done

echo "Test state reset completed."
