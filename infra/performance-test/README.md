# 부하 테스트 인프라

반복 가능한 부하 테스트 환경을 Terraform으로 생성하고 제거한다. 전체 계획은 [아키텍처 문서](../../docs/architecture/performance-test-infrastructure.md)를 참고한다.

## 현재 단계

1단계의 `ECS Fargate API 1대 -> Internal ALB` 배포와 제거 검증을 마쳤다. 2단계에서는 API를 2대로 확장하고 Redis, MySQL, Atlas, external API mock, k6를 연결한다. Prometheus와 Grafana는 3단계에서 추가한다.

## 구조

- `terraform/platform`: AWS 리소스 정의
- `run/performance-test.sh`: 생성, 검증, 상태 확인, 제거를 담당하는 단일 실행 진입점
- `run/verify-phase-one.sh`: ALB target health 확인
- `run/.env.app.example`: 테스트 앱 환경 변수 양식
- `run/publish-app-image.sh`: 현재 commit의 LLV API 이미지를 임시 ECR에 업로드
- `run/upload-app-environment.sh`: 앱 환경 파일을 임시 S3 객체로 업로드
- `run/cleanup-app-environment.sh`: S3 객체와 로컬 환경 파일 삭제
- `run/run-k6-smoke.sh`: VPC 내부에서 ALB health endpoint 호출
- `run/verify-phase-two.sh`: ALB, 의존성, k6 연결을 순서대로 검증
- `k6/smoke.js`: 인프라 연결 확인용 단일 요청 시나리오
- `wiremock`: Bedrock과 Discord의 성공·지연·오류 mapping
- `iam/performance-test-provisioner-policy.json`: Terraform 실행 역할의 초기 권한 정책

## 2단계 계획

- ECS task는 public subnet과 public IP를 사용하며 NAT Gateway와 EIP는 두지 않는다.
- Atlas는 사용자가 유지하는 테스트 전용 cluster를 사용한다. 테스트 직전에 IP allowlist를 `0.0.0.0/0`으로 열고 종료 직후 닫는다.
- ALB, ECS, Atlas, Redis, MySQL, S3는 실제 환경과 유사하게 사용한다.
- S3 AI input/output bucket은 Terraform이 생성·암호화하고 ECS task role에 최소 권한을 부여하며 테스트 종료 시 제거한다.
- Bedrock과 Discord는 WireMock으로 대체하고 실제 Bedrock 지연 시간은 별도의 소규모 호출로 보정한다.
- Firebase Auth는 테스트 JWT로 대체하고 FCM은 mock/no-op으로 실행한다. Sentry는 비활성화한다.
- 비밀값은 서비스별 임시 `.env`를 S3 environment file로 전달한다. Terraform은 객체 내용이 아닌 bucket, object key, IAM만 관리한다.
- 실행 스크립트가 `.env`를 업로드하며 테스트 종료 시 S3 객체와 로컬 파일을 모두 삭제한다.
- 이 단계의 완료 기준은 k6가 ALB를 통해 API를 호출하고 API 또는 전용 probe가 각 의존성 연결을 확인하는 것이다.

Redis, MySQL, WireMock은 각각 `redis`, `mysql`, `mock` 이름으로 Cloud Map에 등록된다. 전체 endpoint는 Terraform의 `dependency_endpoints` output에서 확인한다. MySQL은 비밀번호를 state에 남기지 않기 위해 임시 random root password로 시작하며, 이번 단계에서는 container health와 내부 DNS/TCP 연결만 검증한다.

WireMock은 task 시작 시 초기화 sidecar가 Admin API로 mapping을 등록한다. `terraform.tfvars`의 `mock_scenario`를 `success`, `delay`, `error` 중 하나로 설정해 Bedrock과 Discord 응답을 통제한다. `success`의 기본 지연은 800ms이며 별도 Bedrock 실호출 결과에 맞춰 fixture를 보정한다.

## 관측 계획

3단계에서 Prometheus가 아래 exporter의 `/metrics`를 수집하고 Grafana에서 API 지표와 함께 조회한다.

- `mysqld_exporter`: connection, query rate, InnoDB, lock, I/O 지표
- `redis_exporter`: memory, client, command rate, hit/miss, eviction 지표

exporter의 읽기 전용 DB 계정도 서비스별 S3 environment file로 주입한다. 느린 쿼리 원문과 Redis command 인자는 metric label로 수집하지 않고 DB 로그와 profiler에서 확인한다.

## 실행 준비

먼저 로컬 AWS profile이 `PerformanceTestProvisioner` 역할을 assume하도록 설정한다. 이 설정은 저장소가 아닌 `~/.aws/config`에 둔다.

```ini
[profile llv-performance-test]
role_arn = arn:aws:iam::<account-id>:role/PerformanceTestProvisioner
source_profile = llv-sso
region = ap-northeast-2
```

SSO를 source profile로 사용한다면 최초 한 번 설정하고, credential이 만료됐을 때 다시 로그인한다.

```bash
# 최초 한 번
aws configure sso --profile llv-sso

# SSO credential이 만료된 경우
aws sso login --profile llv-sso
```

runner에 `--profile`을 전달하면 별도로 `AWS_PROFILE`을 export할 필요가 없다. 이미 필요한 권한을 가진 다른 profile이 있다면 `llv-performance-test` 대신 해당 이름을 전달할 수 있다.

`terraform.tfvars`의 `test_run_id`는 실행마다 고유하게 설정한다. runner는 `app_image_tag`를 현재 commit SHA로 갱신한다. 환경 파일과 앱 이미지를 업로드하기 전에는 전체 `terraform apply`를 실행하지 않는다.

Terraform과 검증 스크립트는 같은 `AWS_PROFILE`을 사용한다. CI에서는 GitHub OIDC가 `PerformanceTestProvisioner`의 임시 credential을 제공한다.

## 2단계 환경 파일

ECS를 포함한 전체 Terraform 적용 전에 `infra/performance-test/run/.env.app`에 테스트 전용 환경 변수를 작성하고 업로드한다. 이 파일은 Git에서 제외되며 객체 내용도 Terraform state에 기록되지 않는다.

사용자가 직접 입력할 값은 Atlas 테스트 계정의 `SPRING_DATA_MONGODB_URI`다. JWT, import key, MySQL root·exporter 비밀번호와 Grafana 관리자 비밀번호는 테스트 전용 값으로 생성한다. `JWT_SECRET`은 `openssl rand -base64 32`처럼 32바이트 이상의 키를 Base64로 인코딩하고, 나머지 비밀번호는 `openssl rand -hex 24`로 생성할 수 있다. MySQL exporter 계정은 임시 DB가 시작될 때 조회 권한만 부여되며 비밀번호는 Terraform state에 기록되지 않는다. 업로드 스크립트는 Terraform output을 읽어 Redis와 WireMock 주소, S3 input/output bucket 값을 환경 파일에 자동 반영한다.

Grafana는 임시 public IP로 접근하되 `terraform.tfvars`의 `grafana_allowed_cidr`에 지정한 IPv4 CIDR만 허용한다. 기본값 `127.0.0.1/32`로는 외부에서 접근할 수 없으므로 테스트를 실행할 컴퓨터의 공인 IP를 `/32`로 설정한다. `0.0.0.0/0`은 사용하지 않는다.

업로드 스크립트는 S3 bucket, public access block, 암호화 설정만 먼저 적용한 후 환경 파일을 업로드한다. 업로드가 끝나야 전체 인프라를 적용한다.

```bash
./infra/performance-test/run/performance-test.sh up --profile llv-performance-test
```

runner는 필요한 로컬 설정 파일이 없으면 example을 복사하고 입력할 값만 안내한 뒤 종료한다. 값을 작성하고 같은 명령을 다시 실행하면 Terraform 초기화, 이미지 게시, 환경 파일 업로드, 전체 apply와 연결 검증을 순서대로 수행한다. `--yes`를 추가한 경우에만 Terraform 승인 질문을 생략한다.

실행 중인 환경은 다음 명령으로 다시 검증하거나 상태를 확인한다.

```bash
./infra/performance-test/run/performance-test.sh verify --profile llv-performance-test
./infra/performance-test/run/performance-test.sh status --profile llv-performance-test
```

검증 스크립트는 설정된 수의 앱 target health, Redis·MySQL의 내부 DNS/TCP 연결, 각 DB exporter의 `up` 메트릭, Prometheus scrape target, Grafana health, WireMock mapping, k6의 ALB 요청을 순서대로 확인한다. probe와 smoke task는 상시 실행되는 ECS service가 아니며 검증할 때 각각 한 번 실행된다. 실제 Word 부하 시나리오는 이 연결 확인 이후 별도 task 실행 설정으로 추가한다.

`up`과 `status`는 현재 Grafana task의 URL을 출력한다. Grafana에는 Prometheus와 CloudWatch datasource가 자동 등록되며 기본 대시보드에서 API RPS·p95/p99·5xx, JVM heap, Tomcat thread, Redis, MySQL, ECS와 ALB 지표를 조회한다. CloudWatch 지표는 수집 주기 때문에 테스트 시작 직후 잠시 비어 있을 수 있다.

테스트 종료 후에는 인프라를 제거하기 전에 환경 파일을 먼저 정리한다. 원격 객체 삭제가 실패하더라도 로컬 `.env.app`은 삭제되며, 실패한 S3 객체는 `terraform destroy`의 `force_destroy`로 다시 정리한다.

```bash
./infra/performance-test/run/performance-test.sh down --profile llv-performance-test
```

`down`은 환경 파일 정리 후 Terraform 리소스를 제거하고 state가 비었는지 확인한다. `--yes`를 추가한 경우에만 destroy 승인 질문을 생략한다. Atlas IP allowlist는 runner가 관리하지 않으므로 테스트 직전에 열고 종료 후 직접 닫는다.
