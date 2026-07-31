#!/usr/bin/env bash

set -euo pipefail

platform_dir="${1:-infra/performance-test/terraform/platform}"
test_run_id="${2:-}"
scenario_name="${3:-}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd "${script_dir}/../../.." && pwd)"

if [[ -z "$test_run_id" ]]; then
	echo "A k6 run ID is required." >&2
	exit 1
fi

if [[ ! "$test_run_id" =~ ^[a-z][a-z0-9-]{2,39}$ ]]; then
	echo "k6 run ID must start with a lowercase letter and contain 3 to 40 lowercase letters, numbers, or hyphens." >&2
	exit 1
fi

if [[ ! "$scenario_name" =~ ^[a-z0-9][a-z0-9._-]*\.js$ ]]; then
	echo "k6 scenario must be a JavaScript file name under infra/performance-test/k6." >&2
	exit 1
fi

scenario_file="${repository_root}/infra/performance-test/k6/${scenario_name}"
if [[ ! -f "$scenario_file" ]]; then
	echo "k6 scenario file not found: ${scenario_file}" >&2
	exit 1
fi

region="$(terraform -chdir="$platform_dir" output -raw aws_region)"
cluster_arn="$(terraform -chdir="$platform_dir" output -raw ecs_cluster_arn)"
task_definition_arn="$(terraform -chdir="$platform_dir" output -raw k6_task_definition_arn)"
subnet_id="$(terraform -chdir="$platform_dir" output -raw k6_subnet_id)"
security_group_id="$(terraform -chdir="$platform_dir" output -raw k6_security_group_id)"
scenario_bucket="$(terraform -chdir="$platform_dir" output -raw environment_file_bucket)"
scenario_key="$(terraform -chdir="$platform_dir" output -raw k6_scenario_key)"

k6_family="$(aws ecs describe-task-definition \
	--region "$region" \
	--task-definition "$task_definition_arn" \
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
	echo "A k6 task is already running. Wait for it to finish before starting another run." >&2
	exit 1
fi

if command -v shasum >/dev/null 2>&1; then
	scenario_sha256="$(shasum -a 256 "$scenario_file" | awk '{print $1}')"
elif command -v sha256sum >/dev/null 2>&1; then
	scenario_sha256="$(sha256sum "$scenario_file" | awk '{print $1}')"
else
	echo "A SHA-256 command is required: shasum or sha256sum." >&2
	exit 1
fi

aws s3 cp "$scenario_file" "s3://${scenario_bucket}/${scenario_key}" \
	--region "$region" \
	--sse AES256 \
	--only-show-errors

echo "Uploaded k6 scenario: ${scenario_name} (${scenario_sha256})"

task_arn="$(aws ecs run-task \
	--region "$region" \
	--cluster "$cluster_arn" \
	--task-definition "$task_definition_arn" \
	--launch-type FARGATE \
	--network-configuration \
	"awsvpcConfiguration={subnets=[${subnet_id}],securityGroups=[${security_group_id}],assignPublicIp=ENABLED}" \
	--overrides "{\"containerOverrides\":[{\"name\":\"k6\",\"environment\":[{\"name\":\"TEST_RUN_ID\",\"value\":\"${test_run_id}\"},{\"name\":\"K6_SCENARIO_NAME\",\"value\":\"${scenario_name}\"},{\"name\":\"K6_SCRIPT_SHA256\",\"value\":\"${scenario_sha256}\"}]},{\"name\":\"result-uploader\",\"environment\":[{\"name\":\"TEST_RUN_ID\",\"value\":\"${test_run_id}\"}]}]}" \
	--query 'tasks[0].taskArn' \
	--output text)"

if [[ -z "$task_arn" || "$task_arn" == "None" ]]; then
	echo "Failed to start the k6 task." >&2
	exit 1
fi

echo "Waiting for k6 task: ${task_arn}"
aws ecs wait tasks-stopped --region "$region" --cluster "$cluster_arn" --tasks "$task_arn"

exit_code="$(aws ecs describe-tasks \
	--region "$region" \
	--cluster "$cluster_arn" \
	--tasks "$task_arn" \
	--query 'tasks[0].containers[?name==`k6`].exitCode | [0]' \
	--output text)"

uploader_exit_code="$(aws ecs describe-tasks \
	--region "$region" \
	--cluster "$cluster_arn" \
	--tasks "$task_arn" \
	--query 'tasks[0].containers[?name==`result-uploader`].exitCode | [0]' \
	--output text)"

if [[ "$exit_code" != "0" ]]; then
	stop_reason="$(aws ecs describe-tasks \
		--region "$region" \
		--cluster "$cluster_arn" \
		--tasks "$task_arn" \
		--query 'tasks[0].stoppedReason' \
		--output text)"
	echo "k6 task failed with exit code ${exit_code}: ${stop_reason}" >&2
	exit 1
fi

if [[ "$uploader_exit_code" != "0" ]]; then
	echo "k6 result upload failed with exit code ${uploader_exit_code}." >&2
	exit 1
fi

results_bucket="$(terraform -chdir="$platform_dir" output -raw results_bucket)"
session_id="$(terraform -chdir="$platform_dir" output -raw test_run_id)"
echo "k6 task passed (${scenario_name}). Results: s3://${results_bucket}/test-sessions/${session_id}/runs/${test_run_id}/"
