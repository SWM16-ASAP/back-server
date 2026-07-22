#!/usr/bin/env bash

set -euo pipefail

platform_dir="${1:-infra/performance-test/terraform/platform}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
region="$(terraform -chdir="$platform_dir" output -raw aws_region)"
cluster_arn="$(terraform -chdir="$platform_dir" output -raw ecs_cluster_arn)"
task_definition_arn="$(terraform -chdir="$platform_dir" output -raw dependency_probe_task_definition_arn)"
subnet_id="$(terraform -chdir="$platform_dir" output -raw k6_subnet_id)"
security_group_id="$(terraform -chdir="$platform_dir" output -raw dependency_probe_security_group_id)"

"${script_dir}/verify-phase-one.sh" "$platform_dir"

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

"${script_dir}/run-k6-smoke.sh" "$platform_dir"
echo "Phase two verification passed."
