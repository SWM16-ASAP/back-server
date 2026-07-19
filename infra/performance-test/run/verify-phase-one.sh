#!/usr/bin/env bash

set -euo pipefail

platform_dir="${1:-infra/performance-test/terraform/platform}"
region="$(terraform -chdir="$platform_dir" output -raw aws_region)"
target_group_arn="$(terraform -chdir="$platform_dir" output -raw target_group_arn)"
expected_target_count="$(terraform -chdir="$platform_dir" output -raw app_desired_count)"

for attempt in $(seq 1 30); do
	healthy_target_count="$(aws elbv2 describe-target-health \
		--region "$region" \
		--target-group-arn "$target_group_arn" \
		--query "length(TargetHealthDescriptions[?TargetHealth.State=='healthy'])" \
		--output text)"

	if [[ "$healthy_target_count" == "$expected_target_count" ]]; then
		echo "ALB verification passed: ${healthy_target_count}/${expected_target_count} targets are healthy."
		exit 0
	fi

	echo "Waiting for healthy ALB targets (attempt ${attempt}/30, healthy: ${healthy_target_count}/${expected_target_count})..."
	sleep 10
done

echo "ALB verification failed: expected ${expected_target_count} healthy targets." >&2
exit 1
