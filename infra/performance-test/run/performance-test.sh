#!/usr/bin/env bash

set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd "${script_dir}/../../.." && pwd)"
platform_dir="${repository_root}/infra/performance-test/terraform/platform"
environment_file="${repository_root}/infra/performance-test/run/.env.app"
environment_example="${environment_file}.example"
terraform_vars="${platform_dir}/terraform.tfvars"
terraform_vars_example="${terraform_vars}.example"
current_step="startup"
auto_approve=false
created_local_files=false

usage() {
	cat <<'EOF'
Usage: performance-test.sh <command> [options]

Commands:
  up       Create the phase-two environment and verify connectivity.
  verify   Verify an existing phase-two environment.
  status   Show Terraform resources, outputs, and ALB target health.
  down     Remove the environment and confirm Terraform state is empty.

Options:
  --profile <name>  Use the named AWS CLI profile.
  --yes             Skip Terraform approval prompts for up or down.
  -h, --help        Show this help message.
EOF
}

fail() {
	echo "Error: $*" >&2
	exit 1
}

on_error() {
	local exit_code=$?
	trap - ERR
	echo "Failed during: ${current_step}" >&2
	echo "Inspect the environment with: ${BASH_SOURCE[0]} status${AWS_PROFILE:+ --profile ${AWS_PROFILE}}" >&2
	echo "Remove it with: ${BASH_SOURCE[0]} down${AWS_PROFILE:+ --profile ${AWS_PROFILE}}" >&2
	exit "$exit_code"
}

trap on_error ERR

require_command() {
	local command_name="$1"
	command -v "$command_name" >/dev/null 2>&1 || fail "Required command is not installed: ${command_name}"
}

run_step() {
	current_step="$1"
	shift
	echo
	echo "==> ${current_step}"
	"$@"
}

read_environment_value() {
	local name="$1"
	local line
	line="$(grep -E "^${name}=" "$environment_file" | tail -n 1 || true)"
	printf '%s' "${line#*=}"
}

validate_jwt_secret() {
	local secret="$1"
	local unpadded_secret
	local padding_length
	local decoded_length

	if [[ ! "$secret" =~ ^[A-Za-z0-9+/]+={0,2}$ ]]; then
		fail "JWT_SECRET must be a Base64-encoded HMAC key."
	fi

	unpadded_secret="${secret%%=*}"
	padding_length=$(( ${#secret} - ${#unpadded_secret} ))
	if (( ${#unpadded_secret} % 4 == 1 )) || \
		(( padding_length > 0 && ${#secret} % 4 != 0 )); then
		fail "JWT_SECRET must be valid Base64."
	fi

	decoded_length=$(( ${#unpadded_secret} * 6 / 8 ))
	if (( decoded_length < 32 )); then
		fail "JWT_SECRET must decode to at least 32 bytes for HMAC-SHA."
	fi
}

prepare_local_files() {
	if [[ ! -f "$terraform_vars" ]]; then
		cp "$terraform_vars_example" "$terraform_vars"
		echo "Created ${terraform_vars#${repository_root}/}."
		created_local_files=true
	fi

	if [[ ! -f "$environment_file" ]]; then
		cp "$environment_example" "$environment_file"
		chmod 600 "$environment_file"
		echo "Created ${environment_file#${repository_root}/}."
		created_local_files=true
	fi
}

verify_clean_worktree() {
	local changes
	changes="$(git -C "$repository_root" status --porcelain --untracked-files=normal)"
	if [[ -n "$changes" ]]; then
		echo "Uncommitted files:" >&2
		echo "$changes" >&2
		fail "Commit or remove local changes before publishing a commit-tagged image."
	fi
}

set_application_image_tag() {
	local image_tag
	local temporary_file
	image_tag="$(git -C "$repository_root" rev-parse --short=12 HEAD)"
	temporary_file="$(mktemp)"

	awk -v image_tag="$image_tag" '
		/^app_image_tag[[:space:]]*=/ { print "app_image_tag = \"" image_tag "\""; found = 1; next }
		{ print }
		END { if (!found) print "app_image_tag = \"" image_tag "\"" }
	' "$terraform_vars" >"$temporary_file"

	mv "$temporary_file" "$terraform_vars"
	echo "Application image tag: ${image_tag}"
}


explain_created_local_files() {
	if [[ "$created_local_files" == true ]]; then
		cat <<EOF
Fill the generated files and run the command again.
- terraform.tfvars: use a unique test_run_id if the default can conflict
- .env.app: SPRING_DATA_MONGODB_URI, JWT_SECRET, and IMPORT_API_KEY
EOF
		exit 2
	fi
}

validate_local_files() {
	local name
	local value
	for name in SPRING_DATA_MONGODB_URI JWT_SECRET IMPORT_API_KEY MYSQL_ROOT_PASSWORD MYSQLD_EXPORTER_PASSWORD; do
		value="$(read_environment_value "$name")"
		if [[ -z "$value" || "$value" == *replace-me* || "$value" == replace-with-generated-* ]]; then
			fail "Set ${name} in ${environment_file#${repository_root}/}."
		fi
	done

	validate_jwt_secret "$(read_environment_value JWT_SECRET)"
}

configure_aws_profile() {
	local requested_profile="$1"

	if [[ -n "$requested_profile" ]]; then
		if ! aws configure list-profiles | grep -Fxq "$requested_profile"; then
			echo "Available AWS profiles:" >&2
			aws configure list-profiles >&2
			fail "AWS profile does not exist: ${requested_profile}"
		fi
		export AWS_PROFILE="$requested_profile"
	fi
}

verify_aws_identity() {
	local identity
	if ! identity="$(aws sts get-caller-identity --query '[Account,Arn]' --output text 2>&1)"; then
		fail "AWS authentication failed${AWS_PROFILE:+ for profile ${AWS_PROFILE}}: ${identity}"
	fi
	echo "AWS identity: ${identity}"
}

initialize_terraform() {
	terraform -chdir="$platform_dir" init
	terraform -chdir="$platform_dir" validate
}

preflight() {
	local command="$1"
	current_step="Validate local tools and AWS authentication"
	require_command aws
	require_command terraform
	configure_aws_profile "$profile"
	verify_aws_identity

	if [[ "$command" == "up" ]]; then
		local gradle_output
		require_command docker
		require_command git
		[[ -x "${repository_root}/gradlew" ]] || fail "Gradle wrapper is not executable."
		if ! gradle_output="$("${repository_root}/gradlew" --version 2>&1)"; then
			fail "Gradle wrapper cannot run: ${gradle_output}"
		fi
		if ! docker info >/dev/null 2>&1; then
			fail "Docker is not running."
		fi
		verify_clean_worktree
		prepare_local_files
		set_application_image_tag
		explain_created_local_files
		validate_local_files
	elif [[ "$command" == "down" && ! -f "$terraform_vars" ]]; then
		fail "Terraform variables not found: ${terraform_vars#${repository_root}/}"
	fi

	initialize_terraform
}

terraform_state_exists() {
	local state
	if ! state="$(terraform -chdir="$platform_dir" state list)"; then
		fail "Failed to read Terraform state."
	fi
	[[ -n "$state" ]]
}

run_up() {
	preflight up
	echo
	echo "Terraform will use targeted applies only to bootstrap ECR and S3 before the full apply."
	echo "Ensure the Atlas test allowlist is open for this temporary public-IP environment."

	if [[ "$auto_approve" == true ]]; then
		export TF_CLI_ARGS_apply="${TF_CLI_ARGS_apply:+${TF_CLI_ARGS_apply} }-auto-approve"
	fi

	run_step "Publish application image" "${script_dir}/publish-app-image.sh" "$platform_dir"
	run_step "Upload application environment" "${script_dir}/upload-app-environment.sh" "$platform_dir"
	run_step "Create phase-two infrastructure" terraform -chdir="$platform_dir" apply
	run_step "Verify phase-two connectivity" "${script_dir}/verify-phase-two.sh" "$platform_dir"

	echo
	echo "Phase-two environment is ready."
}

run_verify() {
	preflight verify
	terraform_state_exists || fail "No Terraform-managed performance-test environment exists."
	run_step "Verify phase-two connectivity" "${script_dir}/verify-phase-two.sh" "$platform_dir"
}

run_status() {
	preflight status

	if ! terraform_state_exists; then
		echo "No Terraform-managed performance-test resources exist."
		return
	fi

	echo
	echo "Managed resources:"
	terraform -chdir="$platform_dir" state list

	echo
	echo "Outputs:"
	terraform -chdir="$platform_dir" output

	local target_group_arn
	if target_group_arn="$(terraform -chdir="$platform_dir" output -raw target_group_arn 2>/dev/null)" && \
		[[ -n "$target_group_arn" ]]; then
		local region
		region="$(terraform -chdir="$platform_dir" output -raw aws_region)"
		echo
		echo "ALB target health:"
		aws elbv2 describe-target-health \
			--region "$region" \
			--target-group-arn "$target_group_arn" \
			--query 'TargetHealthDescriptions[].{target:Target.Id,state:TargetHealth.State,reason:TargetHealth.Reason}' \
			--output table
	else
		echo
		echo "ALB target group has not been created yet."
	fi
}

run_down() {
	preflight down

	if ! terraform_state_exists; then
		rm -f "$environment_file"
		echo "No Terraform-managed performance-test resources exist."
		return
	fi

	current_step="Remove application environment file"
	if ! "${script_dir}/cleanup-app-environment.sh" "$platform_dir"; then
		echo "Environment-file cleanup failed; Terraform destroy will continue." >&2
	fi

	current_step="Destroy phase-two infrastructure"
	if [[ "$auto_approve" == true ]]; then
		terraform -chdir="$platform_dir" destroy -auto-approve
	else
		terraform -chdir="$platform_dir" destroy
	fi

	current_step="Confirm Terraform state is empty"
	if terraform_state_exists; then
		echo "Resources remain in Terraform state:" >&2
		terraform -chdir="$platform_dir" state list >&2
		return 1
	fi

	echo
	echo "Phase-two environment was removed. Close the temporary Atlas IP allowlist."
}

command_name="${1:-}"
if [[ -z "$command_name" ]]; then
	usage
	exit 1
fi
shift

profile=""
while [[ $# -gt 0 ]]; do
	case "$1" in
		--profile)
			[[ $# -ge 2 ]] || fail "--profile requires a value."
			profile="$2"
			shift 2
			;;
		--yes)
			auto_approve=true
			shift
			;;
		-h | --help)
			usage
			exit 0
			;;
		*)
			fail "Unknown option: $1"
			;;
	esac
done

cd "$repository_root"

case "$command_name" in
	up)
		run_up
		;;
	verify)
		run_verify
		;;
	status)
		run_status
		;;
	down)
		run_down
		;;
	-h | --help | help)
		usage
		;;
	*)
		usage >&2
		fail "Unknown command: ${command_name}"
		;;
esac
