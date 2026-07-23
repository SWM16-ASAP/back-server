#!/usr/bin/env bash

set -euo pipefail

platform_dir="${1:-infra/performance-test/terraform/platform}"

region="$(terraform -chdir="$platform_dir" output -raw aws_region)"
bucket="$(terraform -chdir="$platform_dir" output -raw environment_file_bucket)"

if ! aws s3 rm "s3://${bucket}/environment/" --recursive --region "$region" --only-show-errors; then
	echo "Failed to delete service environment files from S3." >&2
	exit 1
fi

echo "Service environment files removed from S3. Local .env.app was retained."
