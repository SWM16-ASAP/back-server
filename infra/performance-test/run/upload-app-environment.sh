#!/usr/bin/env bash

set -euo pipefail

platform_dir="${1:-infra/performance-test/terraform/platform}"
environment_file="infra/performance-test/run/.env.app"

read_terraform_value() {
	local expression="$1"
	local value
	value="$(printf '%s\n' "$expression" | terraform -chdir="$platform_dir" console)"
	value="${value#\"}"
	value="${value%\"}"
	printf '%s' "$value"
}

if [[ ! -f "$environment_file" ]]; then
	echo "Application environment file not found: ${environment_file}" >&2
	exit 1
fi

terraform -chdir="$platform_dir" apply \
	-target=aws_s3_bucket.environment_files \
	-target=aws_s3_bucket_public_access_block.environment_files \
	-target=aws_s3_bucket_server_side_encryption_configuration.environment_files \
	-target=aws_s3_bucket.ai \
	-target=aws_s3_bucket_public_access_block.ai \
	-target=aws_s3_bucket_server_side_encryption_configuration.ai

region="$(read_terraform_value 'var.aws_region')"
test_run_id="$(read_terraform_value 'var.test_run_id')"
bucket="$(terraform -chdir="$platform_dir" output -raw environment_file_bucket)"
key="$(read_terraform_value 'local.environment_file_key')"
ai_input_bucket="$(terraform -chdir="$platform_dir" output -raw ai_input_bucket)"
ai_output_bucket="$(terraform -chdir="$platform_dir" output -raw ai_output_bucket)"

replace_environment_value() {
	local name="$1"
	local value="$2"
	local temporary_file
	temporary_file="$(mktemp)"

	awk -v name="$name" -v value="$value" '
		index($0, name "=") == 1 { print name "=" value; found = 1; next }
		{ print }
		END { if (!found) print name "=" value }
	' "$environment_file" >"$temporary_file"

	mv "$temporary_file" "$environment_file"
	chmod 600 "$environment_file"
}

replace_environment_value "S3_AI_INPUT_NAME" "$ai_input_bucket"
replace_environment_value "S3_AI_OUTPUT_NAME" "$ai_output_bucket"
replace_environment_value "SPRING_DATA_REDIS_HOST" "redis.${test_run_id}.llvpt.local"
replace_environment_value "BEDROCK_ENDPOINT" "http://mock.${test_run_id}.llvpt.local:8080"
replace_environment_value "DISCORD_SUGGESTION_WEBHOOK" \
	"http://mock.${test_run_id}.llvpt.local:8080/discord/webhook"

aws s3 cp "$environment_file" "s3://${bucket}/${key}" \
	--region "$region" \
	--sse AES256 \
	--only-show-errors

echo "Application environment file uploaded to s3://${bucket}/${key}."
