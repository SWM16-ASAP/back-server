# 부하 테스트 인프라

반복 가능한 부하 테스트 환경을 Terraform으로 한 번 생성하고, 상태 초기화와 k6 실행을 반복한 뒤 제거한다. 전체 계획은 [아키텍처 문서](../../docs/architecture/performance-test-infrastructure.md)를 참고한다.

## 현재 구성

API 2대, Internal ALB, Redis, MySQL, Atlas, WireMock, k6, Prometheus, Grafana를 연결했다. 인프라 세션이 유지되는 동안 `reset`과 `run`을 반복하고, 분석이 끝난 뒤 `down`으로 제거한다.

## 구조

- `terraform/platform`: AWS 리소스 정의
- `run/performance-test.sh`: 생성, 검증, 상태 확인, 제거를 담당하는 단일 실행 진입점
- `run/.env.app.example`: 테스트 앱 환경 변수 양식
- `run/publish-app-image.sh`: 현재 commit의 LLV API 이미지를 임시 ECR에 업로드
- `run/upload-app-environment.sh`: 앱 환경 파일을 임시 S3 객체로 업로드
- `run/cleanup-app-environment.sh`: S3 environment file 객체 삭제
- `run/run-k6.sh`: VPC 내부에서 기본 k6 연결 시나리오를 실행
- `run/reset-test-state.sh`: MongoDB, Redis, MySQL, WireMock 상태를 초기화
- `run/verify-environment.sh`: ALB, 의존성, 모니터링 연결을 순서대로 검증
- `seed/`: 향후 도메인 시나리오별 최소 fixture를 관리할 위치
- `k6/smoke.js`: 인프라 연결 확인용 단일 요청 시나리오
- `wiremock`: Bedrock과 Discord의 성공·지연·오류 mapping
- `iam/performance-test-provisioner-policy.json`: Terraform 실행 역할의 초기 권한 정책

## 테스트 환경 구성

- ECS task는 public subnet과 public IP를 사용하며 NAT Gateway와 EIP는 두지 않는다.
- Atlas는 사용자가 유지하는 테스트 전용 cluster를 사용한다. 테스트 직전에 IP allowlist를 `0.0.0.0/0`으로 열고 종료 직후 닫는다.
- ALB, ECS, Atlas, Redis, MySQL, S3는 실제 환경과 유사하게 사용한다.
- S3 AI input/output bucket은 Terraform이 생성·암호화하고 ECS task role에 최소 권한을 부여하며 테스트 종료 시 제거한다.
- Bedrock과 Discord는 WireMock으로 대체하고 실제 Bedrock 지연 시간은 별도의 소규모 호출로 보정한다.
- Firebase Auth는 테스트 JWT로 대체하고 FCM은 mock/no-op으로 실행한다. Sentry는 비활성화한다.
- 비밀값은 서비스별 임시 `.env`를 S3 environment file로 전달한다. Terraform은 객체 내용이 아닌 bucket, object key, IAM만 관리한다.
- 실행 스크립트가 `.env.app`에서 서비스별 임시 파일을 만들어 업로드하며 테스트 종료 시 S3 객체만 삭제한다.
- 이 단계의 완료 기준은 k6가 ALB를 통해 API를 호출하고 API 또는 전용 probe가 각 의존성 연결을 확인하는 것이다.

Redis, MySQL, WireMock은 각각 `redis`, `mysql`, `mock` 이름으로 Cloud Map에 등록된다. 전체 endpoint는 Terraform의 `dependency_endpoints` output에서 확인한다. MySQL은 비밀번호를 state에 남기지 않기 위해 임시 random root password로 시작하며, 이번 단계에서는 container health와 내부 DNS/TCP 연결만 검증한다.

WireMock은 task 시작 시 초기화 sidecar가 Admin API로 mapping을 등록한다. `terraform.tfvars`의 `mock_scenario`를 `success`, `delay`, `error`, `recorded` 중 하나로 설정해 Bedrock과 Discord 응답을 통제한다. `success`는 `rabbit` 단어의 유효한 실호출 표본을 2,520ms 지연으로 반복 반환한다. `recorded`는 `wiremock/bedrock-recordings.json`의 100개 응답을 기록된 지연시간과 함께 순차 재생하고, 마지막 표본 뒤에는 첫 표본부터 다시 재생한다. `reset`은 재생 순서를 초기화한다.

## 관측 계획

Prometheus는 아래 exporter의 `/metrics`를 수집하고 Grafana에서 API 지표와 함께 조회한다.

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

`terraform.tfvars`의 `test_run_id`는 인프라 세션마다 고유하게 설정한다. runner는 `app_image_tag`를 현재 commit SHA로 갱신한다. 환경 파일과 앱 이미지를 업로드하기 전에는 전체 `terraform apply`를 실행하지 않는다.

Terraform과 검증 스크립트는 같은 `AWS_PROFILE`을 사용한다. CI에서는 GitHub OIDC가 `PerformanceTestProvisioner`의 임시 credential을 제공한다.

## 환경 파일

ECS를 포함한 전체 Terraform 적용 전에 `infra/performance-test/run/.env.app`에 테스트 전용 환경 변수를 작성하고 업로드한다. 이 파일은 Git에서 제외되며 객체 내용도 Terraform state에 기록되지 않는다.

사용자가 직접 입력할 값은 Atlas 테스트 계정의 `SPRING_DATA_MONGODB_URI`다. JWT, import key, MySQL root·exporter 비밀번호와 Grafana 관리자 비밀번호는 테스트 전용 값으로 생성한다. `JWT_SECRET`은 `openssl rand -base64 32`처럼 32바이트 이상의 키를 Base64로 인코딩한다. `MYSQLD_EXPORTER_PASSWORD`는 초기화 SQL에 안전하게 전달하기 위해 `openssl rand -hex 24` 형식의 24바이트 이상 hex 값으로 설정하며, 나머지 비밀번호도 같은 방식으로 생성할 수 있다. MySQL exporter 계정은 임시 DB가 시작될 때 조회 권한만 부여되며 비밀번호는 Terraform state에 기록되지 않는다. 업로드 스크립트는 Terraform output을 읽어 Redis와 WireMock 주소, S3 input/output bucket 값을 환경 파일에 자동 반영한다.

Grafana는 임시 public IP로 접근하되 `terraform.tfvars`의 `grafana_allowed_cidr`에 지정한 IPv4 CIDR만 허용한다. runner는 `up` 실행 시 AWS checkip으로 현재 공인 IPv4를 조회해 `<IP>/32`로 자동 갱신한다. 기본값 `127.0.0.1/32`은 runner를 거치지 않고 Terraform을 직접 실행할 때만 남는 안전한 기본값이다. `0.0.0.0/0`은 사용하지 않는다.

### Atlas Prometheus 연동

Atlas 지표는 선택 사항이다. Atlas M10+ 클러스터에서 Prometheus integration을 생성한 뒤 `.env.app`에 `ATLAS_PROMETHEUS_ENABLED=true`, Atlas Project ID, integration username/password를 입력하면 Prometheus가 HTTP service discovery로 scrape한다. 이 자격 증명은 Prometheus environment file에만 전달된다.

Atlas access list에는 Prometheus task의 public IP만 허용하고 `0.0.0.0/0` 항목은 제거해야 한다. Atlas는 `/0` allowlist가 있으면 Prometheus integration을 비활성화한다. task IP가 바뀌는 테스트 세션마다 allowlist를 갱신한 후 Grafana의 MongoDB 패널과 Prometheus target 상태를 확인한다.

업로드 스크립트는 S3 bucket, public access block, 암호화 설정만 먼저 적용한 후 환경 파일을 업로드한다. 업로드가 끝나야 전체 인프라를 적용한다.

```bash
./infra/performance-test/run/performance-test.sh up --profile llv-performance-test
```

runner는 필요한 로컬 설정 파일이 없으면 example을 복사하고 입력할 값만 안내한 뒤 종료한다. 값을 작성하고 같은 명령을 다시 실행하면 Terraform 초기화, 이미지 게시, 환경 파일 업로드, 전체 apply, 연결 검증, 최초 상태 초기화를 순서대로 수행한다. `--yes`를 추가한 경우에만 Terraform 승인 질문을 생략한다.

실행 중인 환경은 다음 명령으로 다시 검증하거나 상태를 확인한다.

```bash
./infra/performance-test/run/performance-test.sh verify --profile llv-performance-test
./infra/performance-test/run/performance-test.sh status --profile llv-performance-test
```

검증 스크립트는 설정된 수의 앱 target health, Redis·MySQL의 내부 DNS/TCP 연결, 각 DB exporter의 `up` 메트릭, Prometheus scrape target, Grafana health, WireMock mapping을 순서대로 확인한다. probe와 k6 task는 상시 실행되는 ECS service가 아니다. k6 기본 시나리오는 동일한 신규 `rabbit` 조회를 10개 VU가 한 번씩 동시에 호출해 single-flight를 검증한다.

`up`과 `status`는 현재 Grafana task의 URL을 출력한다. Grafana에는 Prometheus와 CloudWatch datasource가 자동 등록된다. 기본 대시보드에서는 API RPS·p50/p95/p99·HTTP 결과, JVM heap·GC·thread, ECS CPU·memory를 조회하고, Redis·MySQL·MongoDB·ALB 세부 지표는 `Dependency and Platform` 대시보드에서 조회한다. CloudWatch 지표는 수집 주기 때문에 테스트 시작 직후 잠시 비어 있을 수 있다.

k6 task는 실행 맥락, 집계 summary, timestamp가 포함된 raw metric을 세션 전용 S3 bucket에 업로드한다. `run`은 인프라를 다시 적용하지 않고 k6 task만 실행한다. 실행 ID를 생략하면 UTC timestamp 기반 ID를 생성하며, 직접 지정할 수도 있다. 동일 세션에서는 동시에 하나의 k6 task만 실행할 수 있다.

```bash
./infra/performance-test/run/performance-test.sh run --profile llv-performance-test
./infra/performance-test/run/performance-test.sh run --run-id word-single-flight-a --profile llv-performance-test
./infra/performance-test/run/performance-test.sh reset --profile llv-performance-test
```

`reset`은 실행 중인 k6 task가 없을 때만 MongoDB, Redis, MySQL을 비우고 WireMock request journal과 mapping을 기준 시나리오로 되돌린다. Word single-flight 실행 전에는 기본 `success` profile로 적용한 뒤 `reset`을 실행해 빈 저장소 상태에서 시작한다.

결과는 `test-sessions/<test_run_id>/runs/<k6-run-id>` 경로에 세션 동안만 유지되며, `down`에서 결과 bucket과 함께 삭제된다. 수치 분석은 세션이 유지되는 동안 Grafana에서 수행한다.

테스트 종료 후에는 인프라를 제거하기 전에 S3 environment file 객체를 먼저 정리한다. 실패한 S3 객체는 `terraform destroy`의 `force_destroy`로 다시 정리한다. 로컬 `.env.app`은 Git에서 제외하고 권한 `600`으로 유지해 다음 테스트에서 재사용한다.

```bash
./infra/performance-test/run/performance-test.sh down --profile llv-performance-test
```

`down`은 환경 파일 정리 후 Terraform 리소스를 제거하고 state가 비었는지 확인한다. `--yes`를 추가한 경우에만 destroy 승인 질문을 생략한다. Atlas IP allowlist는 runner가 관리하지 않으므로 테스트 직전에 열고 종료 후 직접 닫는다.
