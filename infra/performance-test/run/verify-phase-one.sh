#!/usr/bin/env bash

set -euo pipefail

platform_dir="${1:-infra/performance-test/terraform/platform}"
region="$(terraform -chdir="$platform_dir" output -raw aws_region)"
target_group_arn="$(terraform -chdir="$platform_dir" output -raw target_group_arn)"

for attempt in $(seq 1 30); do
	state="$(aws elbv2 describe-target-health \
		--region "$region" \
		--target-group-arn "$target_group_arn" \
		--query 'TargetHealthDescriptions[0].TargetHealth.State' \
		--output text)"

	if [[ "$state" == "healthy" ]]; then
		echo "Phase one verification passed: ALB target is healthy."
		exit 0
	fi

	echo "Waiting for healthy ALB target (attempt ${attempt}/30, state: ${state})..."
	sleep 10
done

echo "Phase one verification failed: ALB target did not become healthy." >&2
exit 1
