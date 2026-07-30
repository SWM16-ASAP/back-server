#!/usr/bin/env bash

set -euo pipefail

platform_dir="${1:-infra/performance-test/terraform/platform}"
expected_attempts="${2:-1}"

if [[ ! "$expected_attempts" =~ ^[1-9][0-9]*$ ]]; then
	echo "Expected Bedrock attempts must be a positive integer." >&2
	exit 1
fi

region="$(terraform -chdir="$platform_dir" output -raw aws_region)"
cluster_arn="$(terraform -chdir="$platform_dir" output -raw ecs_cluster_arn)"
subnet_id="$(terraform -chdir="$platform_dir" output -raw k6_subnet_id)"
security_group_id="$(terraform -chdir="$platform_dir" output -raw dependency_probe_security_group_id)"
task_definition_arn="$(terraform -chdir="$platform_dir" output -raw wiremock_journal_task_definition_arn)"

task_arn="$(aws ecs run-task \
	--region "$region" \
	--cluster "$cluster_arn" \
	--task-definition "$task_definition_arn" \
	--launch-type FARGATE \
	--network-configuration "awsvpcConfiguration={subnets=[${subnet_id}],securityGroups=[${security_group_id}],assignPublicIp=ENABLED}" \
	--overrides "{\"containerOverrides\":[{\"name\":\"wiremock-journal\",\"environment\":[{\"name\":\"EXPECTED_BEDROCK_ATTEMPTS\",\"value\":\"${expected_attempts}\"}]}]}" \
	--query 'tasks[0].taskArn' \
	--output text)"

if [[ -z "$task_arn" || "$task_arn" == "None" ]]; then
	echo "Failed to start the WireMock journal task." >&2
	exit 1
fi

echo "Waiting for WireMock journal task: ${task_arn}"
aws ecs wait tasks-stopped --region "$region" --cluster "$cluster_arn" --tasks "$task_arn"

exit_code="$(aws ecs describe-tasks \
	--region "$region" \
	--cluster "$cluster_arn" \
	--tasks "$task_arn" \
	--query 'tasks[0].containers[?name==`wiremock-journal`].exitCode | [0]' \
	--output text)"

if [[ "$exit_code" != "0" ]]; then
	echo "WireMock journal verification failed with exit code ${exit_code}." >&2
	exit 1
fi
