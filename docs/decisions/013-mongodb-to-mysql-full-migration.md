# MongoDB 중심 저장소를 MySQL 단일 축으로 완전 전환

## 문제

007번 문서에서 서비스 초기에는 요구사항 변화 속도를 우선해 MongoDB를 주 저장소로 고정했다.
하지만 서비스가 자리를 잡으면서 그 트레이드오프가 뒤집혔다: 콘텐츠 임포트(메타데이터+본문), 학습 진행률 갱신, 단어 동시 저장 경합처럼 여러 문서를 한 번에 일관되게 바꿔야 하는 작업이 계속 늘었는데, MongoDB의 문서 단위 원자성만으로는 이걸 안전하게 보장할 방법이 없었다. 이미 사용자·티켓·콘텐츠 메타데이터 같은 핵심 도메인은 MySQL로 옮겨져 있어 저장소가 두 축으로 쪼개진 상태였고, 그 경계에 걸친 기능일수록 트랜잭션 경계가 애매해지는 문제가 반복됐다.

## 선택

콘텐츠 본문(Chunk 계열), 피드/추천, 설정(크롤링 DSL·배너·앱 버전), 인증/알림(RefreshToken·FcmToken), 단어(Word·WordVariant·InvalidWord), 로그(ContentAccessLog·PushLog) 순으로 남은 6개 도메인을 도메인 단위로 하나씩 MySQL/JPA로 옮겼다. 사용자·티켓·콘텐츠 메타데이터·학습 이력·북마크·읽기 진행률 등 앞서 이미 전환된 도메인과 합쳐, 업무 데이터 전체를 MySQL 하나로 통일했다. MongoDB 의존성 자체(`spring-boot-starter-data-mongodb`, `MongoConfig`)는 나중에 다시 필요해질 가능성을 남겨두기 위해 제거하지 않고 그대로 두기로 했다 — 완전히 새 저장소로 갈아탄 것과 저장소 자체를 걷어내는 것은 별개의 결정이라고 판단했다.

## 이유

- 콘텐츠 임포트, 학습 진행률, 단어 동시 저장 같은 작업은 여러 로우를 한 트랜잭션으로 묶어야 실패 시 부분 반영을 막을 수 있다. MySQL/JPA의 `@Transactional`은 이걸 기본으로 제공하지만, MongoDB는 문서 하나를 넘어가는 순간부터 애플리케이션이 직접 보상 로직을 짜야 했다.
- 사용자·티켓·콘텐츠 메타데이터가 이미 MySQL에 있었기 때문에, 나머지를 MongoDB에 남겨두는 것 자체가 저장소를 하나 더 유지하는 고정비용이었다. 완전히 합치면 그 비용이 사라진다.
- AI 분석 결과처럼 구조가 유동적인 데이터는 MongoDB의 장점이었지만, MySQL 8의 JSON 컬럼(`@JdbcTypeCode(SqlTypes.JSON)`)으로도 스키마 마이그레이션 없이 필드를 추가할 수 있어 이 장점 대부분을 유지한 채로 옮길 수 있었다.
- TTL 인덱스, `DuplicateKeyException` 복구처럼 MongoDB 전용 기능에 의존하던 코드는 반대로 이식 리스크였다 — 이걸 미루기보다 지금 정면으로 옮기는 편이 나중에 더 커진 코드베이스에서 옮기는 것보다 쌌다.

## 검증

- 도메인마다 실제 MySQL(Testcontainers) 기반 영속성 통합 테스트를 새로 추가해, 유니크 제약·FK·낙관적 락·정리 스케줄러 쿼리가 목(mock) 테스트로는 가려지는 문제를 잡아냈다. 대표적으로 [WordPersistenceIntegrationTest.java](../../src/test/java/com/linglevel/api/word/service/WordPersistenceIntegrationTest.java), [LogPersistenceIntegrationTest.java](../../src/test/java/com/linglevel/api/common/log/LogPersistenceIntegrationTest.java), [ContentAccessLogCleanupTransactionTest.java](../../src/test/java/com/linglevel/api/common/log/ContentAccessLogCleanupTransactionTest.java).
- 전체 전환 완료 후 `grep`으로 `src/main/java`에 `@Document`/`extends MongoRepository`가 하나도 남지 않았음을 확인했다.
- `./gradlew clean test`(447개 테스트, 실패 0) / `./gradlew checkFormat`로 전체 회귀를 반복 검증했다.
- 전환 직후 진행한 코드 리뷰에서 실제 버그 3건을 잡아 같은 PR에서 수정했다: 단어 중복 저장 복구가 `EntityManager.clear()`만으로는 rollback-only 트랜잭션을 되돌리지 못해 커밋 시점에 실패하던 문제, 정리 스케줄러 4개가 `deleteByXBefore()` 파생 쿼리에 자체 트랜잭션이 없어 매번 조용히 실패하던 문제, `feeds`/`feed_sources`의 `UNIQUE(url(255))` 접두 인덱스가 서로 다른 URL을 중복으로 오판하던 문제. 자세한 내용은 [MySQL 재설계 문서](../architecture/mysql-first-redesign.md)에 기록했다.

## 결과와 남은 이슈

- 업무 데이터가 MySQL 하나로 통일되면서, 콘텐츠 임포트·학습 진행률·단어 저장처럼 여러 로우를 한 번에 바꾸는 작업이 실제 트랜잭션 원자성을 얻었다.
- MongoDB 전용 기능(TTL 인덱스)을 대체한 정리 스케줄러들은 정책을 그대로 복사하는 대신, 완료 후 재검토해 실제로 맞는 동작인지 다시 판단했다(예: FcmToken 정리가 로그인 중인 사용자의 토큰까지 지우던 것을 활성 토큰 제외로 수정, PR [#365](https://github.com/SWM16-ASAP/back-server/pull/365)).
- MongoDB 의존성은 의도적으로 남겨뒀다 — `MongoConfig`는 감사할 문서가 없어 사실상 무동작이고, `build.gradle`의 Mongo 관련 의존성도 그대로다. 필요 없다고 확정되면 별도로 제거를 재검토한다.
- JSON 컬럼으로 옮긴 필드(`Word.meanings`/`relatedForms`, `Feed.tags` 등)는 Hibernate 6의 기본 Jackson 매퍼가 Spring Boot의 `fail-on-unknown-properties=false` 설정과 별개로 동작해, 필드 구조가 나중에 바뀌면 예전 로우를 읽을 때 역직렬화 예외가 날 수 있는 리스크가 남아 있다 — 아직 실제로 발생하지는 않았고 이번 전환 범위에서 다루지 않았다.
- `CatalogQuery`/`CustomContentRepositoryImpl`이 태그를 `JSON_CONTAINS`로 필터링하는 부분은 인덱스를 타지 못한다 — 트래픽이 늘어 병목이 확인되면 그때 조인 테이블로 정규화할 후보다.

## 연관 이슈 및 PR

- 관련 이슈: 없음
- 관련 PR: [#364](https://github.com/SWM16-ASAP/back-server/pull/364) (사용자·티켓·콘텐츠 메타데이터부터 로그까지 전체 도메인 전환), [#365](https://github.com/SWM16-ASAP/back-server/pull/365) (전환 후 코드 리뷰로 찾은 FcmToken 정리 정책 후속 수정)
