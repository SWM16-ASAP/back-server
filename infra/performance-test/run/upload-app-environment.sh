#!/usr/bin/env bash

set -euo pipefail

platform_dir="${1:-infra/performance-test/terraform/platform}"
environment_file="infra/performance-test/run/.env.app"

if [[ ! -f "$environment_file" ]]; then
	echo "Application environment file not found: ${environment_file}" >&2
	exit 1
fi

region="$(terraform -chdir="$platform_dir" output -raw aws_region)"
bucket="$(terraform -chdir="$platform_dir" output -raw environment_file_bucket)"
key="$(terraform -chdir="$platform_dir" output -raw app_environment_file_key)"

aws s3 cp "$environment_file" "s3://${bucket}/${key}" \
	--region "$region" \
	--sse AES256 \
	--only-show-errors

echo "Application environment file uploaded to s3://${bucket}/${key}."
