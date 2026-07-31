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

temporary_directory="$(mktemp -d)"
trap 'rm -rf "$temporary_directory"' EXIT

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

upload_environment_file() {
	local service="$1"
	local key_expression="$2"
	shift 2

	local destination_file="${temporary_directory}/${service}.env"
	local key
	local allowed_names="$*"
	key="$(read_terraform_value "$key_expression")"

	awk -v allowed_names="$allowed_names" '
		BEGIN {
			split(allowed_names, names, " ")
			for (position in names) allowed[names[position]] = 1
		}
		/^[A-Za-z_][A-Za-z0-9_]*=/ {
			name = $0
			sub(/=.*/, "", name)
			if (allowed[name]) print
		}
	' "$environment_file" >"$destination_file"

	aws s3 cp "$destination_file" "s3://${bucket}/${key}" \
		--region "$region" \
		--sse AES256 \
		--only-show-errors
}

upload_environment_file app 'local.environment_file_keys.app' \
	SPRING_DATA_MONGODB_URI SPRING_PROFILES_ACTIVE SPRING_DATA_MONGODB_DATABASE \
	SPRING_DATA_REDIS_HOST SPRING_DATA_REDIS_PORT RATE_LIMIT_ENABLED \
	JWT_SECRET IMPORT_API_KEY S3_REGION S3_AI_INPUT_NAME S3_AI_OUTPUT_NAME BEDROCK_ENDPOINT \
	DISCORD_SUGGESTION_WEBHOOK FCM_ENABLED SENTRY_ENABLED SENTRY_DSN
upload_environment_file mysql 'local.environment_file_keys.mysql' MYSQL_ROOT_PASSWORD
upload_environment_file mysql-exporter-init 'local.environment_file_keys.mysql_exporter_init' \
	MYSQL_ROOT_PASSWORD MYSQLD_EXPORTER_PASSWORD
upload_environment_file mysql-exporter 'local.environment_file_keys.mysql_exporter' MYSQLD_EXPORTER_PASSWORD
upload_environment_file reset-mongo 'local.environment_file_keys.reset_mongo' \
	SPRING_DATA_MONGODB_URI SPRING_DATA_MONGODB_DATABASE
upload_environment_file prometheus 'local.environment_file_keys.prometheus' \
	IMPORT_API_KEY ATLAS_PROMETHEUS_ENABLED ATLAS_PROMETHEUS_GROUP_ID \
	ATLAS_PROMETHEUS_USERNAME ATLAS_PROMETHEUS_PASSWORD
upload_environment_file grafana 'local.environment_file_keys.grafana' \
	GF_SECURITY_ADMIN_USER GF_SECURITY_ADMIN_PASSWORD
upload_environment_file k6 'local.environment_file_keys.k6' JWT_SECRET

echo "Service-specific environment files uploaded to s3://${bucket}/environment/."
