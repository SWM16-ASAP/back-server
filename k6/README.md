# K6 Performance Testing

`k6` 디렉토리는 정량 부하 테스트를 `부하 프로필` 중심으로 관리한다.
요청 이름보다 `baseline`, `load`, `stress`, `mixed-load` 같은 실험 목적이 먼저 보이도록 두고, 실제 엔드포인트는 환경변수로 주입한다.

## 현재 구조

```text
k6/
├── docker-compose.yml
├── README.md
├── seed/
│   └── ...
└── scripts/
    ├── baseline.js
    ├── load.js
    ├── stress.js
    ├── mixed-load.js
    └── common/
        ├── endpoints.js
        ├── http.js
        ├── profiles.js
        └── summary.js
```

## 전제 조건

- 애플리케이션이 로컬에서 실행 중이어야 한다.
- MongoDB 에는 seed 데이터가 들어 있어야 한다.
- 테스트용 사용자는 `X-Test-Username` 으로 인증 가능해야 한다.
- 시드 생성은 [README.md](/Users/solfe/Desktop/WORK/llv/llv-api/k6/seed/README.md)를 따른다.
- 기본 `BASE_URL` 은 `http://host.docker.internal:8080` 이다.

## 기본 엔드포인트 이름

- `books.default_list`
- `books.progress_filter`
- `books.pagination`

새 엔드포인트를 추가할 때는 `k6/scripts/common/endpoints.js` 에 등록한다.

## 실행 예시

### Baseline
낮은 부하로 기준 응답시간과 기본 안정성을 확인한다.

```bash
docker compose -f k6/docker-compose.yml run --rm \
  -e ENDPOINT_NAME=books.default_list \
  -e TEST_USERNAME=k6seed-user-02 \
  k6 run /scripts/baseline.js
```

### Sustained Load
일반적인 목표 부하를 일정 시간 유지하면서 지속 성능을 본다.

```bash
docker compose -f k6/docker-compose.yml run --rm \
  -e ENDPOINT_NAME=books.progress_filter \
  -e TEST_USERNAME=k6seed-user-02 \
  -e TARGET_VUS=20 \
  k6 run /scripts/load.js
```

### Stress
일반 부하보다 더 높은 요청량으로 밀어 한계 구간과 급격한 성능 저하 지점을 찾는다.

```bash
docker compose -f k6/docker-compose.yml run --rm \
  -e ENDPOINT_NAME=books.pagination \
  -e TEST_USERNAME=k6seed-user-02 \
  -e TARGET_VUS=20 \
  -e STRESS_TARGET_VUS=80 \
  k6 run /scripts/stress.js
```

### Mixed Load
여러 요청을 비율대로 섞어서 실제 사용 패턴에 가까운 혼합 부하를 본다.

```bash
docker compose -f k6/docker-compose.yml run --rm \
  -e ENDPOINT_NAMES=books.default_list,books.progress_filter,books.pagination \
  -e ENDPOINT_WEIGHTS=7,2,1 \
  -e TEST_USERNAME=k6seed-user-02 \
  k6 run /scripts/mixed-load.js
```

### Custom Endpoint
등록되지 않은 엔드포인트를 직접 지정해서 같은 부하 프로필로 측정한다.

```bash
docker compose -f k6/docker-compose.yml run --rm \
  -e ENDPOINT_PATH=/api/v1/books \
  -e ENDPOINT_TAG=books \
  -e ENDPOINT_EXPECTS_ARRAY_AT=content \
  -e TEST_USERNAME=k6seed-user-02 \
  k6 run /scripts/load.js
```

## 자주 바꾸는 환경변수

- `BASE_URL`
- `TEST_USERNAME`
- `ENDPOINT_NAME`
- `ENDPOINT_NAMES`
- `ENDPOINT_WEIGHTS`
- `TARGET_VUS`
- `STRESS_TARGET_VUS`
- `THINK_TIME`

부하 프로필별 세부 duration 은 `baseline.js`, `load.js`, `stress.js` 가 참조하는 `k6/scripts/common/profiles.js` 환경변수로 조정할 수 있다.

## 관리 원칙

- 테스트 파일은 요청 중심이 아니라 부하 프로필 중심으로 둔다.
- 엔드포인트는 `ENDPOINT_NAME` 또는 `ENDPOINT_PATH` 로 주입한다.
- 여러 요청을 묶고 싶을 때는 `mixed-load.js` 에서 엔드포인트 모듈을 조합한다.
- 대조군 비교 시에는 같은 seed prefix, 같은 사용자, 같은 정렬 기준을 고정한다.

## 결과 확인

- 콘솔 실시간 출력
- `/reports` 아래 JSON 결과
- 필요하면 Grafana / InfluxDB 연동
