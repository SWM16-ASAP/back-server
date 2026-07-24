#!/usr/bin/env bash

set -euo pipefail

platform_dir="${1:-infra/performance-test/terraform/platform}"
region="$(terraform -chdir="$platform_dir" output -raw aws_region)"
target_group_arn="$(terraform -chdir="$platform_dir" output -raw target_group_arn)"
expected_target_count="$(terraform -chdir="$platform_dir" output -raw app_desired_count)"
cluster_arn="$(terraform -chdir="$platform_dir" output -raw ecs_cluster_arn)"
task_definition_arn="$(terraform -chdir="$platform_dir" output -raw dependency_probe_task_definition_arn)"
subnet_id="$(terraform -chdir="$platform_dir" output -raw k6_subnet_id)"
security_group_id="$(terraform -chdir="$platform_dir" output -raw dependency_probe_security_group_id)"

for attempt in $(seq 1 30); do
	healthy_target_count="$(aws elbv2 describe-target-health \
		--region "$region" \
		--target-group-arn "$target_group_arn" \
		--query "length(TargetHealthDescriptions[?TargetHealth.State=='healthy'])" \
		--output text)"

	if [[ "$healthy_target_count" == "$expected_target_count" ]]; then
		echo "ALB verification passed: ${healthy_target_count}/${expected_target_count} targets are healthy."
		break
	fi

	if [[ "$attempt" == "30" ]]; then
		echo "ALB verification failed: expected ${expected_target_count} healthy targets." >&2
		exit 1
	fi

	echo "Waiting for healthy ALB targets (attempt ${attempt}/30, healthy: ${healthy_target_count}/${expected_target_count})..."
	sleep 10
done

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
	echo "Failed to start the dependency probe task." >&2
	exit 1
fi

echo "Waiting for dependency probe task: ${task_arn}"
aws ecs wait tasks-stopped --region "$region" --cluster "$cluster_arn" --tasks "$task_arn"

exit_code="$(aws ecs describe-tasks \
	--region "$region" \
	--cluster "$cluster_arn" \
	--tasks "$task_arn" \
	--query 'tasks[0].containers[?name==`dependency-probe`].exitCode | [0]' \
	--output text)"

if [[ "$exit_code" != "0" ]]; then
	echo "Dependency probe failed with exit code ${exit_code}." >&2
	exit 1
fi

echo "Environment verification passed."
