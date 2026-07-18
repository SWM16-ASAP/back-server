#!/usr/bin/env bash

set -euo pipefail

platform_dir="${1:-infra/performance-test/terraform/platform}"
environment_file="infra/performance-test/run/.env.app"

if [[ ! -f "$environment_file" ]]; then
	echo "Application environment file not found: ${environment_file}" >&2
	exit 1
fi

terraform -chdir="$platform_dir" apply \
	-target=aws_s3_bucket.environment_files \
	-target=aws_s3_bucket_public_access_block.environment_files \
	-target=aws_s3_bucket_server_side_encryption_configuration.environment_files

region="$(terraform -chdir="$platform_dir" output -raw aws_region)"
bucket="$(terraform -chdir="$platform_dir" output -raw environment_file_bucket)"
key="$(terraform -chdir="$platform_dir" output -raw app_environment_file_key)"

aws s3 cp "$environment_file" "s3://${bucket}/${key}" \
	--region "$region" \
	--sse AES256 \
	--only-show-errors

echo "Application environment file uploaded to s3://${bucket}/${key}."
