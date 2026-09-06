# 부하 테스트 환경에 OpenTelemetry Java Agent 기반 tracing 도입

## 문제

Prometheus와 Grafana 메트릭으로 CPU, JVM thread, connection pool과 전체 요청 지연은 확인할 수 있었지만, 느린 요청 하나가 HTTP 처리, MongoDB, Redis 또는 외부 I/O 중 어디에서 시간을 소비했는지 요청 단위로 분해하기 어려웠다. 모든 후보 메서드에 계측 코드를 먼저 추가하면 비즈니스 코드 침투와 유지 비용이 커지고, 실제 병목을 찾기 전에 측정 지점을 추측하게 된다.

## 선택

성능 테스트용 Docker image에만 OpenTelemetry Java Agent를 포함하고, `OpenTelemetry Java Agent -> OpenTelemetry Collector -> Tempo -> Grafana` 경로로 trace를 수집한다. Micrometer, Prometheus와 CloudWatch 기반 메트릭·로그 경로는 변경하지 않는다.

## 이유

Java Agent의 자동 계측으로 애플리케이션 코드를 변경하지 않고 HTTP 서버, MongoDB, Redis와 지원되는 클라이언트 라이브러리 경계의 span을 먼저 확보할 수 있다. Collector를 앱과 Tempo 사이에 두면 앱은 표준 OTLP endpoint만 알고, batch와 memory limit 같은 전달 정책은 수집 계층에서 관리할 수 있다. Tempo는 기존 Grafana에서 trace를 탐색할 수 있고, 테스트 세션에 필요한 단기 저장을 단일 인스턴스로 간단히 구성할 수 있다.

Agent는 임의의 모든 내부 메서드를 자동 측정하지 않는다. 자동 span으로 병목 계층을 좁히고도 경계가 부족한 경우에만 custom span을 추가한다. 기본 sample ratio는 진단을 위해 `1.0`으로 두되 Terraform 변수로 조정 가능하게 해 trace 수집 부하를 별도로 비교할 수 있게 한다.

## 검증

- production Docker target에는 Agent가 없고 performance-test target에만 포함되는 것을 image 수준에서 확인했다.
- Collector와 Tempo 공식 image로 각 설정 파일을 검증했다.
- 로컬 격리 네트워크에서 OTLP/HTTP로 Collector에 보낸 trace를 Tempo API에서 같은 trace ID로 조회했다.
- ECS 환경 검증 task가 Collector와 Tempo 상태, 실제 `llv-api` trace 검색을 확인하도록 했다.
- Terraform configuration validation을 통과했다.

## 결과와 남은 이슈

부하 테스트 중 전체 메트릭의 이상 구간에서 느린 개별 요청으로 내려가 지원 라이브러리별 지연을 확인할 수 있다. Trace는 Tempo task의 로컬 임시 저장소에만 유지되므로 task 교체나 환경 제거 뒤에는 남지 않는다. 장기 보존, trace와 metric exemplar 연결, custom span은 실제 분석 필요가 확인될 때 후속 범위로 다룬다.

## 연관 이슈 및 PR

- 관련 이슈: 없음
- 관련 PR: 현재 PR
