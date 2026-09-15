# MySQL 중심 신규 시스템 재설계 계획

상태: MongoDB에 남아 있는 데이터 전체를 MySQL로 전환하는 것이 목표다. 사용자·티켓·커스텀 콘텐츠·책·아티클·학습
이력 및 보상·독서 진행률·북마크·콘텐츠 본문·피드 및 추천·설정·인증(리프레시 토큰)·FCM 토큰 전환 구현 완료. 남은
도메인(단어, 로그)은 순차적으로 전환한다.

새 DB로 시작한다. 기존 운영 데이터 이전·무중단 전환·운영 롤백은 범위에서 제외한다.
기존 운영 환경에 적용하려면 별도 데이터 마이그레이션이 필요하다.

## MySQL로 변경할 테이블

| 작업 단위 | 기존 MongoDB 모델 | MySQL 테이블안 | 변경 내용 |
| --- | --- | --- | --- |
| 사용자 | User | users | 구현 완료. `id`를 MySQL PK·FK용 순차 ID로 사용하고, JWT·API 경계에서는 문자열로 직렬화 |
| 티켓 | UserTicket, TicketTransaction | ticket_wallets, ticket_transactions, ticket_reservations(신규) | 구현 완료. 지갑·확정 거래·예약을 분리하고 예약→확정/해제 적용 |
| 커스텀 콘텐츠 | ContentRequest, CustomContent, UserCustomContent | content_requests, custom_contents, user_custom_contents | 내부 BIGINT ID와 외부 `request_key` 분리. 콘텐츠·소유권·요청 완료·예약 확정을 SQL 트랜잭션으로 처리 |
| 책·아티클 | Book, Chapter, Article | books, chapters, articles | 구현 완료. BIGINT ID, 책·챕터 FK와 챕터 번호 유니크 제약. 본문(청크)은 아래 콘텐츠 본문 항목으로 MySQL로 전환 |
| 학습 이력·보상 | DailyCompletion, UserStudyReport, FreezeTransaction | daily_completions, learning_completions, user_study_reports, freeze_transactions | 구현 완료. 완료 이력 분리, 사용자별 SQL 잠금, 일별 상태·스트릭·프리즈·티켓 보상 트랜잭션 처리 |
| 독서 진행률 | BookProgress, ArticleProgress, CustomContentProgress | book_progress, book_chapter_progress, article_progress, custom_content_progress | 사용자·콘텐츠 유니크 제약, 챕터 진행률 행 분리, 학습 보상과 SQL 트랜잭션 통합 |
| 북마크 | WordBookmark | word_bookmarks | 구현 완료. 사용자·원형 문자열 유니크 제약, SQL 검색·페이지네이션, 추가·삭제·토글 잠금 처리 |
| 콘텐츠 본문 | Chunk, ArticleChunk, CustomContentChunk | chunks, article_chunks, custom_content_chunks | 구현 완료. 책·아티클·커스텀 콘텐츠 청크를 각 콘텐츠 BIGINT FK로 연결하고, 메타데이터·본문 생성/삭제를 하나의 SQL 트랜잭션으로 통합 |
| 피드·추천 | Feed, FeedSource, UserCategoryPreference | feeds, feed_sources, user_category_preferences | 구현 완료. `Feed`·`FeedSource`의 URL 유일성은 접두 유니크 인덱스로 유지. `UserCategoryPreference.userId`는 BIGINT로 전환하고 `users` FK를 추가. 점수·카운트 맵은 JSON 컬럼으로 보존 |
| 설정 | CrawlingDsl, ContentBanner, AppVersion | crawling_dsl, content_banners, app_version | 구현 완료. `ContentBanner.content_id`는 Book/Article/CustomContent를 가리키는 다형적 참조라 FK 없이 BIGINT로만 전환(기존 학습 이력의 다형적 참조와 동일한 패턴). `AppVersion`은 단일 설정 로우로 `updated_at` 내림차순 첫 행 조회 방식을 그대로 유지 |
| 인증·알림 | RefreshToken, FcmToken | refresh_tokens, fcm_tokens | 구현 완료. 두 Mongo TTL 인덱스(즉시 만료 삭제, 90일 미갱신 삭제)를 대체하는 일일 정리 스케줄러를 신규 추가했다. `users` FK를 걸고 `userId`는 BIGINT로 전환 |

## 남은 MongoDB 데이터

| 기존 모델 | 방향 |
| --- | --- |
| Word, WordVariant, InvalidWord | 단어 조회·생성 도메인 전환 예정. 원형·변형 관계와 조회 쿼리 재설계, 기존 single-flight·실패 처리 동작 유지 필요 |
| ContentAccessLog | 조회 인덱스·보존 정책을 검토해 전환 예정. `UserCategoryPreference` 집계의 입력 소스이므로 스케줄러의 `userId` 문자열↔BIGINT 경계에 유의 |

## 나머지 모델

PushLog는 후속 작업에서 MySQL 배치를 정한다(로그 도메인, ContentAccessLog와 함께 전환).
Redis의 세션·rate limit·single-flight 역할은 변경하지 않는다.

## 변경 방법

- 위 작업 단위별로 모델·저장소·서비스·테스트를 함께 변경한다. 큰 단위는 여러 PR로 나눈다.
- 최초 지급·관리자 지급·스트릭 보상 등 관련 쓰기 경로도 같은 기준 저장소를 사용하도록 변경한다.
- 선차감에서 예약 방식으로 바뀌는 정책 변경은 단순 저장소 변경과 구분해 검증한다.
- 완료한 영역의 기존 MongoDB 저장 코드를 제거하고 신규 DB 초기화와 테스트 데이터를 제공한다.
- 실제 변경된 테이블·API·ID·정책 및 신규 구동 방법을 이 문서에 갱신한다. API의 ID 필드 타입은 문자열을 유지하되 사용자·콘텐츠 ID 값은 순차 숫자의 문자열, 요청 ID는 UUID `request_key`가 된다.

## MySQL 기반 구동 (첫 구현 단위)

- JPA와 MySQL Connector/J를 사용하고, 스키마는 Flyway로 관리한다.
- Docker 29 호환성을 위해 Testcontainers 전체 모듈을 1.21.4로 정렬한다. [호환 패치 릴리스](https://github.com/testcontainers/testcontainers-java/releases/tag/1.21.4)
- 별도 전환 프로필이나 직접 작성한 연결·트랜잭션 설정 클래스 없이 Spring Boot 자동 구성을 사용한다.
- `MYSQL_JDBC_URL`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`를 환경변수로 전달한다. URL은 사전에 생성한 신규 DB를 가리켜야 한다.
- `MYSQL_JDBC_URL` 예: `jdbc:mysql://localhost:3306/linglevel`. 계정·비밀번호는 저장소에 기록하지 않는다.
- Hibernate는 `validate`만 수행한다. 업무 테이블 SQL은 `src/main/resources/db/migration/mysql`에 순서대로 추가한다. V1~V5에서 `users`, `ticket_wallets`, `ticket_reservations`, `ticket_transactions`, `content_requests`, `custom_contents`, `user_custom_contents`, `books`, `chapters`, `articles`가 생성된다.
- Flyway의 `clean`과 자동 baseline은 비활성화한다. 기존 운영 DB를 초기화하거나 자동 변환하지 않는다.
- SQL 작업은 기본 `@Transactional`을 사용한다. 자동 구성된 JPA 관리자는 MongoDB 쓰기를 보호하지 않으므로 기존 MongoDB 서비스의 선언은 도메인 전환 시 검토한다.
- AI·Redis·S3 등 나머지 기존 환경 설정은 별도로 필요하다. 성능 테스트 인프라의 앱 환경변수 배선은 이번 변경에 포함하지 않는다.

Docker 실행 후 아래 명령으로 실제 MySQL 8.4.10 및 MongoDB 컨테이너에서 검증한다.

```sh
./gradlew test --tests 'com.linglevel.api.common.mysql.MysqlPersistenceIntegrationTest' --tests 'com.linglevel.api.user.ticket.repository.TicketPersistenceIntegrationTest' --tests 'com.linglevel.api.content.custom.*'
./gradlew checkFormat
```

테스트 전용 테이블로 Flyway 적용·재실행, JPA 저장·조회, 롤백, 유니크 충돌을 검증한다.
같은 Spring 테스트 컨텍스트에서 MongoDB도 연결하며, SQL 롤백이 MongoDB 쓰기를 되돌리지 않는 경계를 확인한다.
업무 스키마와 사용자·티켓·커스텀 콘텐츠를 검증한다. 운영 데이터 이전은 포함하지 않는다.

전체 테스트 및 외부 의존성을 포함한 애플리케이션 구동 검증은 별도다.

## 커스텀 콘텐츠 전환의 보장 범위

- `custom_contents.id`는 BIGINT 자동 생성값이다. 생성 요청 및 생성자 사용자 FK를 저장하며, 한 생성 요청에는 결과 콘텐츠가 최대 하나 존재한다.
- 소유권은 `user_custom_contents`에서 관리한다. 사용자·콘텐츠 조합의 유니크 제약으로 중복 소유권을 방지한다. 원본 생성 요청과 소유권을 획득한 요청은 캐시 재사용 시 다를 수 있다.
- `content_requests.result_custom_content_id`도 BIGINT FK로 전환했다. AI 입력·웹훅의 `requestId`는 계속 UUID `request_key`를 사용한다.
- 청크의 `customContentId`와 S3의 콘텐츠 경로는 MySQL 콘텐츠 ID의 문자열 표현이다. 진행률의 내부 `customId`는 BIGINT FK다.
- SQL 소유권 조건과 키워드·JSON 태그 필터를 DB에서 적용하고, 정렬에 ID를 추가해 동률 페이지 순서를 고정한다. 진행률 필터도 SQL EXISTS/NOT EXISTS 조건으로 처리한다.
- 완료·실패·진행 웹훅은 요청 행을 잠근 뒤 상태를 확인한다. 완료 재전송은 결과를 재생성하지 않으며, 완료/실패 이후 도착한 진행·실패 이벤트는 종료 상태를 덮어쓰지 않는다.
- 완료 시 콘텐츠·소유권·요청 상태·티켓 확정을 한 SQL 트랜잭션에서 커밋한다. 완료 알림은 커밋 이후 발송한다. 명시적인 실패 웹훅은 요청 실패와 예약 해제를 함께 커밋한다.
- AI 결과 파싱·이미지 이동·Mongo 본문 저장 중 오류가 나면 SQL 변경을 롤백하고 예약을 유지한다. 결과 처리 재시도는 동일한 `request_key` 웹훅 재전송으로 가능하다. 자동 재시도·장기 미완료 요청 회수는 아직 구현하지 않았다.
- 현재 완료 처리의 SQL 트랜잭션 안에 S3/Mongo I/O가 포함돼 요청 행 잠금 및 커넥션 점유 시간이 길어질 수 있다. 외부 작업을 분리하려면 결과 준비 상태와 재시도 프로토콜이 추가로 필요하다.
- MySQL 롤백은 이미 저장된 Mongo 청크·S3 객체를 삭제하지 않는다. 이때 미참조 산출물이 남을 수 있으며 자동 정리는 별도 과제다. 사용자 제공 상태는 SQL 커밋으로 결정한다.
- 통합 테스트는 실제 MySQL을 사용하며 AI/S3/알림 및 완료 처리의 Mongo 경계는 테스트 대역으로 장애를 주입한다. 실 AI 서버와의 종단 검증은 별도다.
- 여러 MySQL 테스트를 함께 실행할 때 컨텍스트가 종료된 컨테이너 주소를 재사용하던 문제는 클래스 종료 시 Spring 컨텍스트를 폐기하도록 수정했다.

## 책·아티클 전환의 보장 범위

- 책·챕터·아티클의 PK는 BIGINT 자동 생성값이다. 챕터는 책 FK를 가지며, `(book_id, chapter_number)` 유니크 제약과 양수 번호 CHECK를 적용했다.
- API 응답의 ID 타입은 문자열을 유지한다. Mongo 청크 참조와 S3 경로·알림에는 SQL ID의 문자열 표현을 사용한다. 기존 ObjectId 데이터의 변환은 포함하지 않는다.
- 번역 제목·태그·대상 언어는 카탈로그에 종속된 작은 JSON 값으로 저장한다. 독립적인 거래/참조 관계가 아니므로 별도 테이블로 나누지 않았다.
- 목록 필터·개수·페이지네이션은 SQL로 처리한다. 태그는 기존처럼 하나 이상 일치(OR), 키워드는 대소문자를 구분하지 않는 **문자열 포함 검색**이다. 정규식 검색은 지원하지 않으며 `%`, `_`를 문자 그대로 처리한다. 생성일 필터의 시간대는 UTC다.
- 생성일·카테고리 조회 인덱스를 추가하고 정렬 동률은 ID로 결정한다. JSON 태그 검색과 앞뒤 와일드카드 검색의 인덱스 효율·실제 부하 성능은 별도 측정이 필요하다.
- 진행률은 MySQL에 저장하며 목록 필터는 EXISTS/NOT EXISTS로 처리한다. 진행 이력 전체를 메모리로 읽어 SQL ID 목록으로 넘기지 않는다.
- 아티클의 진행 중 필터는 모델에 없는 `currentReadChunkNumber` 대신 현재 저장 필드인 `normalizedProgress > 0`을 사용한다. 책의 부분 챕터 읽기 판정은 유지한다.
- 가져오기 성공 시 책·챕터/아티클 메타데이터를 SQL 트랜잭션으로 확정한다. 본문 파싱·저장 실패 시 SQL은 롤백한다. 빈 본문과 책의 메타데이터/본문 챕터 수 불일치는 성공으로 처리하지 않는다.
- SQL 커밋 전에 Mongo 본문을 저장하지만 두 저장소가 원자적으로 커밋되지는 않는다. SQL 트랜잭션 안의 S3/Mongo I/O, 실패 후 외부 산출물 잔존, 관리자 삭제의 교차 저장소 부분 실패는 남은 한계다. 외부 호출 프로토콜 변경·자동 정리는 이번 단위에서 다루지 않는다.
- 아티클 청크 목록도 먼저 SQL 콘텐츠 존재 여부를 확인해 미참조 본문만으로 콘텐츠가 노출되지 않게 했다. 조회수는 SQL 원자적 증가 쿼리로 갱신한다.
- 실제 MySQL·MongoDB 컨테이너로 FK/유니크/CHECK, JSON 왕복, 필터·정렬·페이지, 가져오기·본문 연결·실패 롤백·동시 조회수 증가를 검증한다. S3·AI·이미지 변환은 테스트 대역이며 실 서비스 종단 검증은 별도다.

```sh
./gradlew test --tests 'com.linglevel.api.content.book.*' --tests 'com.linglevel.api.content.article.*' --tests 'com.linglevel.api.content.common.Catalog*'
./gradlew checkFormat
```

## 학습 이력·보상 전환의 보장 범위

- V6에서 `user_study_reports`, `daily_completions`, `learning_completions`, `freeze_transactions`를 추가한다. PK와 사용자 FK는 BIGINT다. API의 사용자·거래 ID는 문자열 표현을 유지한다.
- 리포트는 사용자당 하나, 일별 요약은 사용자·KST 날짜당 하나다. FK·유니크 제약과 잔액/집계의 음수 방지 CHECK를 적용한다. 리포트에는 낙관적 버전 검사도 둔다.
- 기존 일별 완료 배열은 `learning_completions` 행으로 분리했다. 리포트에 누적 ID 집합을 저장하지 않고 이력에서 고유 수를 조회한다. 유형과 ID를 함께 식별하므로 BOOK/ARTICLE/CUSTOM의 숫자 ID가 같아도 서로 다른 학습이다. BOOK의 학습 단위 ID는 책이 아니라 챕터다.
- 다형적인 콘텐츠 참조에는 콘텐츠 테이블 FK를 걸지 않는다. 콘텐츠를 삭제해도 학습 이력을 보존하며, 일별 요약을 명시적으로 삭제할 때만 해당 완료 이력을 함께 삭제한다.
- 변경 전에 사용자 행을 잠가 최초 리포트 생성부터 동시 학습·보상·복구를 직렬화한다. 리포트·당일 상태·최초 완료 판단은 필요한 곳에서 잠금 읽기를 사용해, 기존 트랜잭션이 먼저 조회했더라도 오래된 스냅샷만 보고 판단하지 않도록 한다.
- `updateStreak`는 보상 지급과 당일 COMPLETED 표시를 함께 커밋한다. 뒤의 `addCompletedContent` 호출까지 기다리지 않으므로 같은 날 동시 요청에도 스트릭/티켓/프리즈 보상은 한 번만 지급한다. 기존 진행률 서비스의 외부 SQL 트랜잭션 안에서 학습 시간·완료 이력·보상은 함께 롤백된다.
- 반복 읽기는 기존 정책대로 이력을 추가하고 totalCompletionCount를 늘린다. 동일 콘텐츠의 최초 완료 집계만 한 번 증가한다. 요청/읽기 세션 ID에 의한 전송 재시도 멱등성은 아직 제공하지 않는다.
- 누락일 처리에서는 스케줄러가 전달한 리포트 대신 잠근 최신 리포트를 사용하며, 프리즈 거래·일별 상태·잔액을 같은 트랜잭션에 저장한다. 스케줄러가 처리 후 오래된 객체를 다시 저장하는 경로는 제거했다.
- 프리즈의 `created_at`은 처리 시각, `effective_date`는 적용 대상 학습 날짜다. 자정 이후 처리한 전날 프리즈도 올바르게 알림 판정에 포함한다. 복구 보상이 보유 한도를 초과하면 초과분 조정 거래를 남겨 잔액 변화와 이력이 맞도록 한다.
- 캘린더 날짜 범위는 양 끝을 포함하고, 프리즈/티켓 거래 시간 범위는 시작 포함·종료 제외다. 기존 캘린더의 누락된 상태·스트릭 수 보정은 쓰기 트랜잭션과 사용자 잠금 아래 유지한다.
- 같은 사용자의 학습·복구는 대기할 수 있다. 다른 SQL 작업과의 데드락/타임아웃 자동 재시도, 대규모 복구·캘린더 backfill 최적화는 이번 범위 밖이다.
- 진행률도 SQL 트랜잭션에 포함되며 읽기 세션은 Redis다. SQL 롤백이 소비한 Redis 세션·발행한 이벤트까지 되돌리지는 않는다. 해당 경계의 재시도/복구는 별도 과제이며 종단 멱등성을 보장한다고 해석하지 않는다.
- 실제 MySQL에서 동시 첫 리포트 생성, 최초 콘텐츠 집계, 일별 보상, 누적 학습 시간, 프리즈 재처리, 복구 보상, 제약 조건, 보상 실패 시 롤백을 검증한다. 이전 Mongo 기반 backfill 테스트도 MySQL로 전환했다.

```sh
./gradlew test --tests 'com.linglevel.api.streak.*'
./gradlew checkFormat
```

## 독서 진행률 전환의 보장 범위

- V7에서 `book_progress`, `book_chapter_progress`, `article_progress`, `custom_content_progress`를 추가한다. PK는 BIGINT 자동 증가, 사용자·콘텐츠 참조는 BIGINT FK다. Mongo 청크 ID만 문자열로 유지한다.
- 사용자·콘텐츠당 진행률은 하나다. 챕터 진행률은 책 진행률에 종속된 별도 행이며 `(book_progress_id, chapter_number)`가 유일하다. 진행률 삭제 시 자식 챕터 행도 삭제한다. 콘텐츠의 물리 삭제는 진행률로 전파되지만 학습 완료 이력은 별도로 보존한다.
- 진행률 갱신·삭제는 사용자 행을 먼저 잠근 뒤 진행률을 잠금 조회한다. 최초 행 생성과 서로 다른 챕터 동시 갱신을 직렬화하며 기존 학습·보상 경로와 잠금 순서를 맞춘다. 진행률 엔티티에도 버전 검사를 둔다.
- 진행률·학습 시간·완료 이력·스트릭·티켓/프리즈 보상은 같은 SQL 트랜잭션으로 커밋/롤백한다. 완료 여부는 마지막 청크 도달로, 학습 보상은 유효한 읽기 시간 30초 이상으로 판단하는 기존 정책을 유지한다.
- 책의 재독으로 기존 챕터 완료 상태와 최초 완료 시각을 지우지 않는다. 책의 완료 비율은 이번 챕터 완료까지 반영한 뒤 응답·저장한다. 현재 위치는 뒤로 이동할 수 있지만 최대 도달 위치는 유지한다.
- `GET /progress`는 세 유형 모두 저장하지 않는다. 책은 미시작 0% 응답, 아티클·커스텀은 기존 첫 청크 초기 응답을 유지하되 ID·갱신 시각은 null이다. 후자의 조회만으로 진행 중 목록에 추가되던 부작용을 제거했다.
- 책·챕터·아티클·커스텀 목록의 진행 상태 필터는 SQL EXISTS/NOT EXISTS를 사용한다. 책의 부분 챕터 읽기 판정과 기존 아티클·커스텀의 미시작 조건(진행률 행 없음)을 유지한다.
- SQL 트랜잭션 중 Mongo 청크 조회와 Redis 세션 처리가 남아 있어 커넥션·잠금 점유 비용이 있다. Redis 세션 소비 및 이미 발행한 접근 이벤트는 SQL 롤백 대상이 아니다. 세션 재시도 멱등성, 장애 복구, 관리자 교차 저장소 삭제의 원자성은 이번 범위 밖이다.
- 실제 MySQL 테스트에 동시 최초 생성·최대 진도·챕터 완료 보존, FK/유니크, 조회 무저장, 삭제 전파, 보상 실패 및 SQL flush 이후 전체 롤백을 포함한다. 이 테스트의 Mongo 조회·Redis 세션 경계는 대역이며 실 외부 시스템의 종단 보장은 검증하지 않는다.

```sh
./gradlew test --tests 'com.linglevel.api.content.*' --tests 'com.linglevel.api.streak.*' --tests 'com.linglevel.api.user.repository.*' --tests 'com.linglevel.api.user.ticket.*' --tests 'com.linglevel.api.common.mysql.*'
./gradlew checkFormat
```

## 북마크 전환의 보장 범위

- V8에서 `word_bookmarks`를 추가한다. PK와 사용자 FK는 BIGINT이며 API 북마크 ID는 숫자의 문자열 표현이다. 단어 ID 기반 토글의 입력은 계속 Mongo Word의 ObjectId다.
- 사용자·원형 문자열 조합이 유일하다. 단어 문자열의 유일성은 binary collation으로 기존 Mongo의 정확한 문자열 식별을 유지한다. 목록 검색만 대소문자를 무시한다.
- 북마크는 Mongo 단어 문서의 존재 여부에 종속되지 않는다. SQL에서 저장된 원형을 직접 검색·페이지 처리해 기존 Mongo 검색 1,000개 제한과 Mongo 장애 시 목록 조회 의존성을 제거했다. 검색은 문자 그대로의 포함 검색이며 `%`, `_`는 와일드카드로 취급하지 않는다.
- 최신순은 `bookmarked_at DESC, id DESC`다. 같은 시각의 페이지 순서를 고정한다. 사용자별 최신 목록 인덱스는 추가했지만 포함 검색의 인덱스 효율과 부하 성능은 별도 측정이 필요하다.
- AI/원형 조회를 담당하는 `BookmarkService`와 SQL 변경 전용 `BookmarkWriter`를 분리한다. 기본 API 경로는 원형 조회를 마친 뒤 SQL 트랜잭션을 시작한다. 외부 호출 실패 시 북마크는 변경하지 않는다.
- Writer는 사용자 행을 먼저 잠근 후 북마크를 잠금 조회한다. 동시 추가는 하나만 성공하고 나머지는 기존 중복 오류(409)가 된다. 토글은 호출마다 한 번씩 반전한다. 토글은 재시도 멱등 API가 아니며 네트워크 재전송도 별도 반전으로 취급한다.
- 삭제는 입력 문자열을 우선하고, 없으면 variant의 원형 후보를 순서대로 확인하는 정책을 유지한다. 사용자별 잠금은 학습 등 다른 사용자 행 잠금과도 경합할 수 있다.
- Facade는 `NOT_SUPPORTED`로 호출자 트랜잭션을 중단하고 Writer는 자체 SQL 트랜잭션을 사용한다. 따라서 상위 호출자의 롤백으로 이미 완료한 북마크가 되돌아가지 않는다. 사용자 잠금을 이미 잡은 트랜잭션에서 Facade를 중첩 호출하는 용도로 사용하지 않는다.
- 기존 관리자 `reset-and-normalize`의 북마크 갱신도 Writer를 통하도록 변경했다. 중복 여부 확인·삭제를 같은 트랜잭션으로 처리하며 SQL 제약 위반 후 같은 트랜잭션에서 복구하지 않는다. 이 API는 **Mongo 단어 전체 삭제와 AI 재생성을 포함하는 기존 운영 도구**이며, 이번 작업에서 실행하거나 안전한 데이터 마이그레이션 도구로 확장하지 않았다.
- MySQL 컨테이너에서 동시 추가·토글, FK/유니크, 정확한 단어 구분, 검색·동률 페이지, SQL 롤백, 정규화 중복 처리를 검증한다. AI/Mongo는 대역이며 실 AI 비용·서비스 장애 복구는 범위 밖이다.

```sh
./gradlew test --tests 'com.linglevel.api.bookmark.*' --tests 'com.linglevel.api.word.service.WordServiceTest'
./gradlew checkFormat
```

## 콘텐츠 본문 전환의 보장 범위

- V9에서 `chunks`, `article_chunks`, `custom_content_chunks`를 추가한다. PK는 BIGINT 자동 증가다. 세 청크 모두 부모 참조(`chapter_id`/`article_id`/`custom_id`)가 이미 MySQL BIGINT의 문자열 표현이었으므로 값 이관 없이 컬럼 타입만 BIGINT로 바꾸고 FK(`ON DELETE CASCADE`)를 추가했다.
- 챕터·아티클 청크는 `(부모_id, difficulty_level, chunk_number)` 유니크 제약과 `chunk_number > 0` CHECK를 가진다. 커스텀 콘텐츠 청크는 `(custom_id, difficulty_level, chapter_num, chunk_num)` 유니크 제약을 가지며 기존 Mongo의 `isDeleted`/`deletedAt` 소프트 삭제 필드와 동작을 그대로 이식했다 — 다만 이를 `true`로 설정하는 코드는 이번에도 추가하지 않았다(기존에도 없던 갭이며 커스텀 콘텐츠 삭제 정책은 별도 과제로 남긴다).
- 책·아티클 가져오기, 커스텀 콘텐츠 생성 완료 각각에서 메타데이터(책/챕터, 아티클, 커스텀 콘텐츠)와 본문 청크 저장이 이제 같은 MySQL 트랜잭션 안에서 실제로 원자적이다. 기존에는 청크가 별도 저장소(Mongo)여서 트랜잭션이 메타데이터에만 적용됐지만, 이번 전환으로 청크 저장 실패 시 메타데이터도 함께 롤백된다.
- 같은 이유로 관리자 삭제(`AdminService.deleteBook`/`deleteArticle`)의 청크 삭제도 이제 FK `ON DELETE CASCADE`로 이중 보장되며, 존재하지 않는 부모를 가리키는 청크를 저장하는 것 자체가 DB 제약으로 차단된다 — 이전에 앱 레벨에서 방어하던 "미참조 청크가 공개 목록에 노출되지 않는지" 검증은 이제 애초에 그런 행이 생성될 수 없으므로 제약 위반 검증으로 대체했다.
- 학습 진행률 서비스(`ProgressService`, `ArticleProgressService`, `CustomContentReadingProgressService`)의 청크당 총 개수 조회는 청크 엔티티에서 이미 얻은 BIGINT 부모 ID를 그대로 사용하도록 통일했다. 문자열 부모 ID를 받는 오버로드는 그 외 호출부(카탈로그 목록·조회수 갱신 등)에서 계속 쓰인다.
- API 응답의 청크 ID는 계속 문자열이다(`BIGINT.toString()`). 진행률 엔티티의 `chunk_id` 컬럼은 이번에도 손대지 않았고 여전히 청크 ID의 문자열 표현을 담는 일반 VARCHAR 컬럼이다(FK 아님) — 진행률 도메인에 FK를 추가할지는 별도 논의가 필요하다.
- 실제 MySQL 컨테이너로 FK/유니크/CHECK, 메타데이터·본문 동시 롤백(S3 실패, 챕터 수 불일치, 빈 본문), 조회수 동시 증가, 진행률·스트릭·보상과의 트랜잭션 통합을 검증한다. AI/S3/알림은 테스트 대역이다.

```sh
./gradlew test --tests 'com.linglevel.api.content.book.*' --tests 'com.linglevel.api.content.article.*' --tests 'com.linglevel.api.content.common.*' --tests 'com.linglevel.api.admin.*' --tests 'com.linglevel.api.content.custom.*' --tests 'com.linglevel.api.streak.*'
./gradlew checkFormat
```

## 피드·추천 전환의 보장 범위

- V10에서 `feeds`, `feed_sources`, `user_category_preferences`를 추가한다. PK는 BIGINT 자동 증가다. `Feed`와 `FeedSource`는 서로 ID로 연결되지 않고(크롤링 시점에 필드만 복사), `CustomContent.originUrl`과 `Feed.url`도 여전히 URL 문자열 동등 비교로만 연결된다 — 이번 전환에서 새 FK 관계를 만들지 않았다.
- `feeds.url`/`feed_sources.url`은 `VARCHAR(2048)`이라 MySQL InnoDB 유니크 인덱스 키 길이 한도를 넘는다. 기존 `custom_contents.origin_url(255)` 접두 인덱스 관례를 따라 `UNIQUE (url(255))` 접두 유니크 인덱스로 유일성을 보장한다. 254자를 넘는 URL 두 개가 앞 255자까지 완전히 같을 경우에만 이론적으로 오탐지할 수 있으나, 실제 RSS 기사/영상 URL 길이 분포에서는 무시할 수준이다.
- `UserCategoryPreference.userId`는 `String`에서 `BIGINT`로, `users(id)` FK와 유니크 제약을 추가했다. `ContentAccessLog`(아직 MongoDB, 이번 전환 범위 밖)의 `userId`는 계속 문자열이므로, `UserPreferenceAggregationScheduler`는 로그에서 읽은 문자열 `userId`를 리포지토리 조회/저장 경계에서 `Long.valueOf`로 변환한다 — 다른 이미 전환된 도메인과 동일한 경계 처리 방식이다.
- `categoryScores`(`Map<ContentCategory, Double>`), `rawAccessCounts`(`Map<ContentCategory, Integer>`)는 JSON 컬럼으로 보존한다. 사용되지 않던 `tagScores` 필드(작성도 조회도 하는 코드가 없었음)는 이번에 제거했다.
- `Feed.viewCount` 증가는 기존에 3곳(`FeedService.getFeed`, `CustomContentChunkService`의 URL 조인, `ContentAccessEventListener`의 평균 읽기시간 갱신)에서 **읽기→+1→저장**으로 처리되어 동시 요청 시 증가분이 유실될 수 있었다. `FeedRepository.incrementViewCount`/`incrementViewCountByUrl`(`@Modifying @Query` 원자적 벌크 UPDATE)을 추가하고 조회수 증가 경로 두 곳(직접 조회, URL 매칭)을 여기로 옮겼다. `ContentAccessEventListener`의 평균 읽기시간 계산은 현재 조회수를 읽어 가중 평균을 구해야 해서 읽기·수정·저장 방식을 유지한다.
- 위 작업 중 기존 Book/Article/CustomContent의 `incrementViewCount`가 동적 필터 쿼리(`CatalogQuery`/QueryDSL 스타일)와 같은 `*RepositoryImpl`에 `EntityManager.createQuery` 기반으로 얹혀 있던 것을 발견해, 세 곳 모두 리포지토리 인터페이스의 `@Modifying @Query` 메서드로 옮기고 `*RepositoryCustom`/`*RepositoryImpl`에서는 제거했다. 동적 WHERE 절 조립이 실제로 필요한 카탈로그 필터링 메서드(`findBooksWithFilters` 등)만 `EntityManager` 기반으로 남겼다 — 단순 원자적 증가 같은 정적 쿼리에는 `@Modifying @Query`가 더 적합한 표준 Spring Data 방식이기 때문이다.
- `FeedService`의 목록 조회는 기존처럼 `findByDeletedFalse()`로 전체를 메모리에 올린 뒤 필터·정렬·페이지네이션을 자바에서 수행한다 — 이번 전환은 저장소만 교체했고, SQL 기반 필터링으로의 전환은 범위에 포함하지 않았다.
- 실제 MySQL 컨테이너로 URL 접두 유니크 제약, `user_category_preferences`의 FK/유니크 제약, JSON 왕복, 조회수 원자적 증가를 검증한다. RSS 파싱·크롤링 로직 자체는 외부 네트워크에 의존하는 기존 `@Tag("external")` 테스트로 남겨두고 이번 검증에 포함하지 않는다.

```sh
./gradlew test --tests 'com.linglevel.api.content.feed.*' --tests 'com.linglevel.api.content.recommendation.*' --tests 'com.linglevel.api.admin.*' --tests 'com.linglevel.api.content.custom.*'
./gradlew checkFormat
```

## 설정 전환의 보장 범위

- V11에서 `crawling_dsl`, `content_banners`, `app_version`을 추가한다. PK는 BIGINT 자동 증가다.
- `crawling_dsl.domain`은 유니크 제약을 유지한다(도메인 문자열은 URL보다 훨씬 짧아 접두 인덱스 없이 전체 컬럼에 유니크 제약을 걸 수 있다). 조회는 여전히 도메인 문자열로만 하며 `id`로 조회하는 API/내부 호출은 없다.
- `content_banners.content_id`는 Book/Article/CustomContent 중 하나를 가리키는 다형적 참조라 특정 테이블에 FK를 걸 수 없다 — 학습 이력의 다형적 콘텐츠 참조와 동일한 이유로 FK 없는 BIGINT 컬럼으로 두었다. `content_type` 컬럼으로 어느 테이블인지 구분하는 기존 방식을 그대로 유지한다.
- 배너의 `content_title`/`content_author`/`content_cover_image_url`/`content_reading_time`은 생성 시점에 `ContentInfoProviderFactory`가 조회한 값을 저장하는 **스냅샷**이며, 이번 전환에서도 라이브 조인으로 바꾸지 않았다 — 원본 콘텐츠가 나중에 바뀌어도 배너는 갱신되지 않는 기존 동작을 그대로 유지한다.
- `getNextDisplayOrder`는 기존에 국가별 배너를 내림차순으로 전부 불러온 뒤 첫 번째 값을 쓰는 방식이었다. `findFirstByCountryCodeOrderByDisplayOrderDesc`(LIMIT 1)로 바꿔 동일한 결과를 더 적은 데이터로 얻도록 했다 — 조회 결과 자체는 이전과 같다.
- 배너의 `(country_code, display_order)` 중복 여부는 기존처럼 생성 시점의 `existsBy` 확인으로만 방지하며, 이번 전환에서 DB 유니크 제약을 새로 걸지는 않았다. 동시에 두 배너를 같은 순서로 생성하면 지금처럼 중복이 발생할 수 있는 기존 한계를 그대로 남겨둔다 — 조회수 증가와 달리 저빈도 관리자 작업이라 이번 범위에서 강화하지 않았다.
- `app_version`은 여러 행이 쌓일 수 있는 단일 설정 테이블 구조를 그대로 유지한다(사실상 "가장 최근에 수정된 행"이 곧 현재 설정). `findTopByOrderByUpdatedAtDesc`는 JPA에서도 동일한 메서드명으로 동작해 리포지토리 시그니처 변경이 필요 없었다.
- API가 이미 `id`를 노출하지 않는 `AppVersion`을 제외하고, `CrawlingDsl`/`ContentBanner`의 응답 `id`는 문자열 표현을 유지한다.
- 실제 MySQL 컨테이너로 `crawling_dsl` 도메인 유니크 제약, 국가별 표시순서 조회, 활성 배너 정렬, `app_version` 최신 행 조회를 검증한다. 이 세 도메인은 기존에 저장소/서비스/컨트롤러 테스트가 전혀 없었으므로 이번에 리포지토리 수준 테스트를 새로 추가했다 — 서비스/컨트롤러 계층 테스트는 이번 저장소 전환 범위 밖이다.

```sh
./gradlew test --tests 'com.linglevel.api.common.config.*' --tests 'com.linglevel.api.crawling.*' --tests 'com.linglevel.api.content.feed.filter.filters.ContentCrawlabilityFilterTest'
./gradlew checkFormat
```

## 인증·알림 전환의 보장 범위

- V12에서 `refresh_tokens`, `fcm_tokens`를 추가한다. PK는 BIGINT 자동 증가이며 둘 다 `users` FK를 가진다.
- Mongo에서는 `RefreshToken.expiresAt`에 즉시 만료 TTL 인덱스(`expireAfter = "0s"`)를, `FcmToken.updatedAt`에 90일 TTL 인덱스를 걸어 만료 데이터를 백그라운드에서 자동 삭제했다. MySQL에는 TTL 인덱스가 없으므로 이를 대체하는 **일일 정리 스케줄러**를 신규 추가했다(`RefreshTokenCleanupScheduler`, `FcmTokenCleanupScheduler`, 매일 03:00 KST, 기존 `UserPreferenceAggregationScheduler`의 로그 정리 배치와 동일한 스타일). 만료 리프레시 토큰은 `RefreshToken.isExpired()` 읽기 시점 확인도 기존처럼 유지해 정리 배치가 지연되더라도 즉시 거부된다.
- `FcmToken` 정리는 기존 TTL과 동일하게 `isActive` 값과 무관하게 `updated_at` 기준 90일이 지나면 삭제한다 — 90일간 앱을 재실행하지 않은 **활성** 토큰도 삭제될 수 있는 기존 동작을 그대로 옮겼다. 활성 토큰만 남기고 싶다면 별도 정책 변경이 필요하며, 이번 전환에서는 다루지 않았다.
- `UsersService.deleteUser`는 계정 삭제 시 FCM 토큰은 비활성화하지만(`fcmTokenService.deactivateAllTokens`) 리프레시 토큰은 명시적으로 삭제하지 않는 기존 동작을 그대로 유지한다 — 다음 토큰 갱신 시도에서 `refreshAccessToken`이 `user.getDeleted()`를 확인해 그 시점에 지연 삭제되며, 이 경계는 이번 전환 범위에서 다루지 않는다.
- `RefreshTokenService.refreshAccessToken`이 회전 시 기존 토큰 행을 무효화하지 않는 기존 동작(리프레시할 때마다 새 토큰이 추가되고 이전 토큰은 만료 전까지 계속 유효), `AuthService.logout`이 FCM 토큰은 건드리지 않는 기존 동작(기기별 FCM 비활성화 메서드 `deactivateTokenByDevice`가 실제로는 어디서도 호출되지 않음)도 모두 기존 그대로 유지했다 — 저장소 전환과 무관한 기존 로직/갭이므로 이번 작업에서 변경하지 않았다.
- `RefreshToken`/`FcmToken`/`AuthService`/`RefreshTokenService`/`AuthController`는 이번 전환 이전에 테스트가 전혀 없었다. 리포지토리 수준 영속성 테스트(유니크 제약, FK, 정리 쿼리)만 새로 추가했고, 서비스/컨트롤러 계층 테스트 보강은 저장소 전환 범위 밖으로 남겨둔다.

```sh
./gradlew test --tests 'com.linglevel.api.common.auth.*' --tests 'com.linglevel.api.fcm.*' --tests 'com.linglevel.api.streak.scheduler.*' --tests 'com.linglevel.api.admin.*'
./gradlew checkFormat
```

## 관련 문서

- [시스템 컨텍스트](overview.md)
- [기존 MongoDB 선택 배경](../decisions/007-choose-mongodb-for-early-flexibility.md)
