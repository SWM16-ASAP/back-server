# Word Single-Flight 분산 안정화 보고서 (Redlock 적용)

## 문제

`word` 동적 AI 생성 경로에서 멀티 인스턴스 동시 요청 시 중복 AI 호출 및 저장 경합 가능성이 확인되었다.
기존 단일 락 기반 흐름은 `lockTtlMs` 만료, follower 조기 timeout, Redis 토폴로지 차이(replica/read-write split) 조건에서 일관성 저하 위험이 있었다.

## 선택

single-flight 조정 경로를 Redisson 기반으로 전환하고, Redlock 경로를 기본값으로 채택했다.
동시에 노드 설정 이상 상황에서는 단일 락 폴백 경로로 기동하도록 fail-safe 동작을 추가했다.
또한 follower 에러 처리와 락 만료 시맨틱을 보정해 단기 장애 증폭 가능성을 낮췄다.

## 이유

우선순위는 단기간 내 운영 리스크 완화와 기동 안전성 확보로 설정했다.
`fencing token` 기반 모델은 저장소/다운스트림 검증 지점 추가와 토큰 단조성 보장 설계가 필요해 즉시 적용 범위에서 제외했다.
또한 본 건의 핵심 목표가 AI 요청 수 절감인데, fencing token은 stale write 방지에는 유효해도 AI 중복 호출 자체를 차단하지는 못한다.
이에 따라 1차 조치는 duplicate-call 완화와 fail-safe 확보에 집중하고, 엄격 정합성 요구는 후속 과제로 분리했다.

## 검증

- [WordSingleFlightRedisCoordinator.java](../../src/main/java/com/linglevel/api/word/service/singleflight/WordSingleFlightRedisCoordinator.java) 기준으로 락 경로가 Redisson 기반으로 전환된 것을 확인했다.
- follower timeout 및 leader 실패 전파 시맨틱 보정 사항을 코드 단위로 확인했다.
- 로컬 3노드 Redis 환경에서 Redlock 경로와 단일 락 폴백 경로를 테스트로 검증했다.
- 변경 사항은 PR 단위로 분리해 검증했다: `#328`(분산 안정화), `#330`(만료/에러 시맨틱 보정), `#331`(Redlock + 폴백 검증).

## 결과와 남은 이슈

- 동일 요청 동시 처리 구간에서 중복 AI 호출 가능성이 감소했고, 설정 오류 시에도 fail-safe 기동이 가능해졌다.
- single-flight 안정화 범위 내 목표는 충족되었다.
- 남은 이슈는 별도 후속 과제로 관리한다.
  - 저장 경합 복구 표준화(`DuplicateKeyException` 캐치 후 재조회)
  - 비동기 Job/체크포인트 기반 대량 생성 파이프라인 전환
  - 엄격 정합성 요구 시 `fencing token`/CP 락 저장소 재검토

## 연관 이슈 및 PR

- 관련 이슈: 없음
- 관련 PR: [#328](https://github.com/SWM16-ASAP/back-server/pull/328), [#330](https://github.com/SWM16-ASAP/back-server/pull/330), [#331](https://github.com/SWM16-ASAP/back-server/pull/331)
