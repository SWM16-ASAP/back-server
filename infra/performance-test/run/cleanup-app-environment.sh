#!/usr/bin/env bash

set -euo pipefail

platform_dir="${1:-infra/performance-test/terraform/platform}"
environment_file="infra/performance-test/run/.env.app"

trap 'rm -f "$environment_file"' EXIT

region="$(terraform -chdir="$platform_dir" output -raw aws_region)"
bucket="$(terraform -chdir="$platform_dir" output -raw environment_file_bucket)"
key="$(terraform -chdir="$platform_dir" output -raw app_environment_file_key)"

if ! aws s3 rm "s3://${bucket}/${key}" --region "$region" --only-show-errors; then
	echo "Failed to delete s3://${bucket}/${key}; the local environment file was removed." >&2
	exit 1
fi

echo "Application environment file removed from S3 and local storage."
