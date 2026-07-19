#!/usr/bin/env bash

set -euo pipefail

platform_dir="${1:-infra/performance-test/terraform/platform}"

read_terraform_value() {
	local expression="$1"
	local value
	value="$(printf '%s\n' "$expression" | terraform -chdir="$platform_dir" console)"
	value="${value#\"}"
	value="${value%\"}"
	printf '%s' "$value"
}

terraform -chdir="$platform_dir" apply -target=aws_ecr_repository.app

region="$(read_terraform_value 'var.aws_region')"
image_tag="$(read_terraform_value 'var.app_image_tag')"
repository_url="$(terraform -chdir="$platform_dir" output -raw app_ecr_repository_url)"
registry="${repository_url%%/*}"
repository_name="${repository_url#*/}"

existing_tag="$(aws ecr list-images \
	--region "$region" \
	--repository-name "$repository_name" \
	--filter tagStatus=TAGGED \
	--query "imageIds[?imageTag == '${image_tag}'].imageTag | [0]" \
	--output text)"

if [[ "$existing_tag" == "$image_tag" ]]; then
	echo "Application image already exists at ${repository_url}:${image_tag}; reusing it."
	exit 0
fi

./gradlew bootJar
aws ecr get-login-password --region "$region" | \
	docker login --username AWS --password-stdin "$registry"
docker buildx build \
	--platform linux/amd64 \
	--tag "${repository_url}:${image_tag}" \
	--push \
	.

echo "Application image pushed to ${repository_url}:${image_tag}."
