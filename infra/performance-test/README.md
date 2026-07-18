# 부하 테스트 인프라

반복 가능한 부하 테스트 환경을 Terraform으로 생성하고 제거한다. 전체 계획은 [아키텍처 문서](../../docs/architecture/performance-test-infrastructure.md)를 참고한다.

## 현재 단계

1단계의 `ECS Fargate API 1대 -> Internal ALB` 배포와 제거 검증을 마쳤다. 2단계에서는 API를 2대로 확장하고 Redis, MySQL, Atlas, external API mock, k6를 연결한다. Prometheus와 Grafana는 3단계에서 추가한다.

## 구조

- `terraform/platform`: AWS 리소스 정의
- `run/verify-phase-one.sh`: ALB target health 확인
- `run/.env.app.example`: 테스트 앱 환경 변수 양식
- `run/upload-app-environment.sh`: 앱 환경 파일을 임시 S3 객체로 업로드
- `run/cleanup-app-environment.sh`: S3 객체와 로컬 환경 파일 삭제
- `iam/performance-test-provisioner-policy.json`: Terraform 실행 역할의 초기 권한 정책

## 2단계 계획

- ECS task는 public subnet과 public IP를 사용하며 NAT Gateway와 EIP는 두지 않는다.
- Atlas는 사용자가 유지하는 테스트 전용 cluster를 사용한다. 테스트 직전에 IP allowlist를 `0.0.0.0/0`으로 열고 종료 직후 닫는다.
- 비밀값은 서비스별 임시 `.env`를 S3 environment file로 전달한다. Terraform은 객체 내용이 아닌 bucket, object key, IAM만 관리한다.
- 실행 스크립트가 `.env`를 업로드하며 테스트 종료 시 S3 객체와 로컬 파일을 모두 삭제한다.
- 이 단계의 완료 기준은 k6가 ALB를 통해 API를 호출하고 API 또는 전용 probe가 각 의존성 연결을 확인하는 것이다.

Redis, MySQL, WireMock은 각각 `redis`, `mysql`, `mock` 이름으로 Cloud Map에 등록된다. 전체 endpoint는 Terraform의 `dependency_endpoints` output에서 확인한다. MySQL은 비밀번호를 state에 남기지 않기 위해 임시 random root password로 시작하며, 이번 단계에서는 container health와 내부 DNS/TCP 연결만 검증한다.

## 관측 계획

3단계에서 Prometheus가 아래 exporter의 `/metrics`를 수집하고 Grafana에서 API 지표와 함께 조회한다.

- `mysqld_exporter`: connection, query rate, InnoDB, lock, I/O 지표
- `redis_exporter`: memory, client, command rate, hit/miss, eviction 지표

exporter의 읽기 전용 DB 계정도 서비스별 S3 environment file로 주입한다. 느린 쿼리 원문과 Redis command 인자는 metric label로 수집하지 않고 DB 로그와 profiler에서 확인한다.

## 1단계 실행

먼저 로컬 AWS profile이 `PerformanceTestProvisioner` 역할을 assume하도록 설정한다. 이 설정은 저장소가 아닌 `~/.aws/config`에 둔다.

```ini
[profile llv-performance-test]
role_arn = arn:aws:iam::<account-id>:role/PerformanceTestProvisioner
source_profile = llv-sso
region = ap-northeast-2
```

```bash
# 최초 1회
aws configure sso --profile llv-sso

aws sso login --profile llv-sso
export AWS_PROFILE=llv-performance-test

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

Terraform과 검증 스크립트는 같은 `AWS_PROFILE`을 사용한다. CI에서는 GitHub OIDC가 `PerformanceTestProvisioner`의 임시 credential을 제공한다.

## 2단계 환경 파일

ECS를 포함한 전체 Terraform 적용 전에 `infra/performance-test/run/.env.app`에 테스트 전용 환경 변수를 작성하고 업로드한다. 이 파일은 Git에서 제외되며 객체 내용도 Terraform state에 기록되지 않는다.

업로드 스크립트는 S3 bucket, public access block, 암호화 설정만 먼저 적용한 후 환경 파일을 업로드한다. 업로드가 끝나야 전체 인프라를 적용한다.

```bash
cp infra/performance-test/run/.env.app.example infra/performance-test/run/.env.app
chmod 600 infra/performance-test/run/.env.app
./infra/performance-test/run/upload-app-environment.sh

terraform -chdir=infra/performance-test/terraform/platform apply
```

테스트 종료 후에는 인프라를 제거하기 전에 환경 파일을 먼저 정리한다. 원격 객체 삭제가 실패하더라도 로컬 `.env.app`은 삭제되며, 실패한 S3 객체는 `terraform destroy`의 `force_destroy`로 다시 정리한다.

```bash
./infra/performance-test/run/cleanup-app-environment.sh

cd infra/performance-test/terraform/platform
terraform destroy
```
