# Word Single-Flight 분산 안정화 보고서 (RLock 표준화)

## 문제

`word` 동적 AI 생성 경로에서 멀티 인스턴스 동시 요청 시 중복 AI 호출 및 저장 경합 가능성이 확인되었다.
기존 단일 락 기반 흐름은 `lockTtlMs` 만료, follower 조기 timeout, Redis 토폴로지 차이(replica/read-write split) 조건에서 일관성 저하 위험이 있었다.

## 선택

single-flight 조정 경로를 Redisson 기반으로 전환하고, 운영 표준은 `RLock + 12초 고정 lease TTL`로 확정했다.
또한 follower 에러 처리와 락 만료 시맨틱을 보정해 단기 장애 증폭 가능성을 낮췄다.

## 이유

우선순위는 단기간 내 운영 리스크 완화와 기동 안전성 확보로 설정했다.
AWS Bedrock 동기 추론 API(Converse/InvokeModel)에는 멱등키가 없어 호출 단계 중복 제거를 플랫폼에 위임하기 어려웠다.
`fencing token` 기반 모델은 저장소/다운스트림 검증 지점 추가와 토큰 단조성 보장 설계가 필요해 즉시 적용 범위에서 제외했다.
또한 본 건의 핵심 목표가 AI 요청 수 절감인데, fencing token은 stale write 방지에는 유효해도 AI 중복 호출 자체를 차단하지는 못한다.
Bedrock 호출 제한 8초에 저장·완료 전파 여유를 더한 12초 lease를 사용한다. TTL 만료 후 중복 호출은 허용하되, 저장 경합이 요청 실패로 이어지지 않도록 `Word`와 `WordVariant`의 `DuplicateKeyException`을 잡은 뒤 unique key로 이미 확정된 결과를 재조회한다. 무한 갱신에 따른 orphan lock 장기 점유를 피하기 위해 watchdog은 사용하지 않는다.

## 검증

- [WordSingleFlightRedisCoordinator.java](../../src/main/java/com/linglevel/api/word/service/WordSingleFlightRedisCoordinator.java) 기준으로 락 경로가 Redisson `RLock` 기반으로 표준화된 것을 확인했다.
- lock 획득 시 명시적 lease TTL을 전달해 watchdog을 비활성화한 것을 코드와 단위 테스트로 확인했다.
- follower timeout 및 leader 실패 전파 시맨틱 보정 사항을 코드 단위로 확인했다.
- `Word`와 `WordVariant`의 동시 insert 충돌에서 duplicate 예외 대신 기존 결과를 반환하는 단위 테스트를 추가했다.
- 두 인스턴스 동시 요청에서 single-flight 1회 실행, leader 실패 전파, timeout fallback을 테스트로 검증했다.
- 변경 사항은 PR 단위로 분리해 검증했다: `#328`(분산 안정화), `#330`(만료/에러 시맨틱 보정), `#331`(Redlock + 폴백 검증).

## 결과와 남은 이슈

- 동일 요청 동시 처리 구간에서 중복 AI 호출 가능성이 감소했고, 설정 오류 시에도 fail-safe 기동이 가능해졌다.
- single-flight 안정화 범위 내 목표는 충족되었다.
- 남은 이슈는 별도 후속 과제로 관리한다.
  - 비동기 Job/체크포인트 기반 대량 생성 파이프라인 전환
  - 엄격 정합성 요구 시 `fencing token`/CP 락 저장소 재검토

## 연관 이슈 및 PR

- 관련 이슈: 없음
- 관련 PR: [#328](https://github.com/SWM16-ASAP/back-server/pull/328), [#330](https://github.com/SWM16-ASAP/back-server/pull/330), [#331](https://github.com/SWM16-ASAP/back-server/pull/331)
