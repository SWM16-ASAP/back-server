# 부하 테스트 인프라

반복 가능한 부하 테스트 환경을 Terraform으로 생성하고 제거한다. 전체 계획은 [아키텍처 문서](../../docs/architecture/performance-test-infrastructure.md)를 참고한다.

## 현재 단계

1단계는 `ECS Fargate API 1대 -> Internal ALB` 배포와 제거만 검증한다. 기본 이미지는 Nginx이며, Redis, MySQL, Atlas, k6, Prometheus, Grafana는 다음 단계에서 추가한다. Word single-flight 실험에서는 API를 2대로 확장한다.

## 구조

- `terraform/platform`: AWS 리소스 정의
- `run/verify-phase-one.sh`: ALB target health 확인
- `iam/performance-test-provisioner-policy.json`: Terraform 실행 역할의 초기 권한 정책

## 실행

```bash
cd infra/performance-test/terraform/platform
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform apply

cd ../../../..
./infra/performance-test/run/verify-phase-one.sh

cd infra/performance-test/terraform/platform
terraform destroy
```

`terraform.tfvars`의 `test_run_id`는 실행마다 고유하게 설정한다. 로컬 PC에서는 Internal ALB에 직접 접근하지 않고, 검증 스크립트로 target health를 확인한다.

Terraform은 애플리케이션 역할이 아닌 별도 `PerformanceTestProvisioner` 역할로 실행한다. 로컬 자격 증명이 이 역할을 assume할 수 있다면 `deployment_role_arn`을 `terraform.tfvars`에 설정한다.
