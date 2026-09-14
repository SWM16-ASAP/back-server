# MySQL 중심 신규 시스템 재설계 계획

상태: 사용자·티켓·커스텀 콘텐츠·책·아티클 구현, 나머지 작업 단위 대기.

새 DB로 시작한다. 기존 운영 데이터 이전·무중단 전환·운영 롤백은 범위에서 제외한다.
기존 운영 환경에 적용하려면 별도 데이터 마이그레이션이 필요하다.

## MySQL로 변경할 테이블

| 작업 단위 | 기존 MongoDB 모델 | MySQL 테이블안 | 변경 내용 |
| --- | --- | --- | --- |
| 사용자 | User | users | 구현 완료. `id`를 MySQL PK·FK용 순차 ID로 사용하고, JWT·API 경계에서는 문자열로 직렬화 |
| 티켓 | UserTicket, TicketTransaction | ticket_wallets, ticket_transactions, ticket_reservations(신규) | 구현 완료. 지갑·확정 거래·예약을 분리하고 예약→확정/해제 적용 |
| 커스텀 콘텐츠 | ContentRequest, CustomContent, UserCustomContent | content_requests, custom_contents, user_custom_contents | 내부 BIGINT ID와 외부 `request_key` 분리. 콘텐츠·소유권·요청 완료·예약 확정을 SQL 트랜잭션으로 처리 |
| 책·아티클 | Book, Chapter, Article | books, chapters, articles | 구현 완료. BIGINT ID, 책·챕터 FK와 챕터 번호 유니크 제약. 본문은 MongoDB에 유지 |
| 학습 이력·보상 | DailyCompletion, UserStudyReport, FreezeTransaction | daily_completions, learning_completions, user_study_reports, freeze_transactions | 완료 내역 배열·누적 완료 ID 집합을 완료 이력으로 분리. 일별 요약·스트릭·프리즈·티켓 보상 정합성 관리 |
| 독서 진행률 | BookProgress, ArticleProgress, CustomContentProgress | book_progress, book_chapter_progress, article_progress, custom_content_progress | 사용자·콘텐츠별 진행률 관리. 챕터 진행률 배열은 별도 테이블로 분리 |
| 북마크 | WordBookmark | word_bookmarks | 사용자·단어 조합의 유일성 유지. 단어 본문과는 독립적으로 관리 |

## MongoDB에 남길 데이터

| 기존 모델 | 변경 방향 |
| --- | --- |
| Word, WordVariant, InvalidWord | 단어 조회·생성 도메인 전체를 MongoDB에 유지 |
| Chunk, ArticleChunk, CustomContentChunk | 본문 저장소로 유지하고 MySQL의 콘텐츠·챕터 ID로 연결 |
| ContentAccessLog | 업무 트랜잭션과 분리된 접근·분석 로그로 유지 |

본문은 먼저 저장·검증한 뒤 MySQL에서 결과 참조와 제공 상태를 확정한다.
두 DB를 하나의 트랜잭션으로 묶지 않으며, 미사용 본문 정리와 참조된 본문의 삭제 정책은 별도로 정한다.
티켓·프리즈 거래 내역은 분석 로그가 아니므로 MySQL에서 관리한다.

## 나머지 모델

RefreshToken, FcmToken, PushLog, Feed, FeedSource, CrawlingDsl, ContentBanner,
AppVersion, UserCategoryPreference는 우선 기존 저장 방식을 유지하고 후속 작업에서 배치를 정한다.
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
- 청크의 `customContentId`, 진행률의 `customId`, S3의 콘텐츠 경로는 MySQL 콘텐츠 ID의 문자열 표현이다. 읽기 진행률은 해당 전환 단위까지 MongoDB에 남는다.
- SQL 소유권 조건과 키워드·JSON 태그 필터를 DB에서 적용하고, 정렬에 ID를 추가해 동률 페이지 순서를 고정한다. 진행률 필터는 Mongo 기록의 ID를 SQL 조건에 전달한다. 진행 기록이 매우 많아지면 이 보조 조회 비용을 재평가한다.
- 완료·실패·진행 웹훅은 요청 행을 잠근 뒤 상태를 확인한다. 완료 재전송은 결과를 재생성하지 않으며, 완료/실패 이후 도착한 진행·실패 이벤트는 종료 상태를 덮어쓰지 않는다.
- 완료 시 콘텐츠·소유권·요청 상태·티켓 확정을 한 SQL 트랜잭션에서 커밋한다. 완료 알림은 커밋 이후 발송한다. 명시적인 실패 웹훅은 요청 실패와 예약 해제를 함께 커밋한다.
- AI 결과 파싱·이미지 이동·Mongo 본문 저장 중 오류가 나면 SQL 변경을 롤백하고 예약을 유지한다. 결과 처리 재시도는 동일한 `request_key` 웹훅 재전송으로 가능하다. 자동 재시도·장기 미완료 요청 회수는 아직 구현하지 않았다.
- 현재 완료 처리의 SQL 트랜잭션 안에 S3/Mongo I/O가 포함돼 요청 행 잠금 및 커넥션 점유 시간이 길어질 수 있다. 외부 작업을 분리하려면 결과 준비 상태와 재시도 프로토콜이 추가로 필요하다.
- MySQL 롤백은 이미 저장된 Mongo 청크·S3 객체를 삭제하지 않는다. 이때 미참조 산출물이 남을 수 있으며 자동 정리는 별도 과제다. 사용자 제공 상태는 SQL 커밋으로 결정한다.
- 통합 테스트는 실제 MySQL을 사용하며 AI/S3/알림 및 완료 처리의 Mongo 경계는 테스트 대역으로 장애를 주입한다. 실 AI 서버와의 종단 검증은 별도다.
- 여러 MySQL 테스트를 함께 실행할 때 컨텍스트가 종료된 컨테이너 주소를 재사용하던 문제는 클래스 종료 시 Spring 컨텍스트를 폐기하도록 수정했다.

## 책·아티클 전환의 보장 범위

- 책·챕터·아티클의 PK는 BIGINT 자동 생성값이다. 챕터는 책 FK를 가지며, `(book_id, chapter_number)` 유니크 제약과 양수 번호 CHECK를 적용했다.
- API 응답의 ID 타입은 문자열을 유지한다. Mongo 청크·진행률 참조와 S3 경로·알림에는 SQL ID의 문자열 표현을 사용한다. 기존 ObjectId 데이터의 변환은 포함하지 않는다.
- 번역 제목·태그·대상 언어는 카탈로그에 종속된 작은 JSON 값으로 저장한다. 독립적인 거래/참조 관계가 아니므로 별도 테이블로 나누지 않았다.
- 목록 필터·개수·페이지네이션은 SQL로 처리한다. 태그는 기존처럼 하나 이상 일치(OR), 키워드는 대소문자를 구분하지 않는 **문자열 포함 검색**이다. 정규식 검색은 지원하지 않으며 `%`, `_`를 문자 그대로 처리한다. 생성일 필터의 시간대는 UTC다.
- 생성일·카테고리 조회 인덱스를 추가하고 정렬 동률은 ID로 결정한다. JSON 태그 검색과 앞뒤 와일드카드 검색의 인덱스 효율·실제 부하 성능은 별도 측정이 필요하다.
- 진행률은 아직 MongoDB에 있다. 사용자 진행 기록에서 ID를 읽어 SQL의 IN/NOT IN 조건으로 전달하되 전체 카탈로그를 메모리로 불러오지 않는다. 대량 진행 이력의 비용은 진행률 SQL 전환 때 개선한다.
- 아티클의 진행 중 필터는 모델에 없는 `currentReadChunkNumber` 대신 현재 저장 필드인 `normalizedProgress > 0`을 사용한다. 책의 부분 챕터 읽기 판정은 유지한다.
- 가져오기 성공 시 책·챕터/아티클 메타데이터를 SQL 트랜잭션으로 확정한다. 본문 파싱·저장 실패 시 SQL은 롤백한다. 빈 본문과 책의 메타데이터/본문 챕터 수 불일치는 성공으로 처리하지 않는다.
- SQL 커밋 전에 Mongo 본문을 저장하지만 두 저장소가 원자적으로 커밋되지는 않는다. SQL 트랜잭션 안의 S3/Mongo I/O, 실패 후 외부 산출물 잔존, 관리자 삭제의 교차 저장소 부분 실패는 남은 한계다. 외부 호출 프로토콜 변경·자동 정리는 이번 단위에서 다루지 않는다.
- 아티클 청크 목록도 먼저 SQL 콘텐츠 존재 여부를 확인해 미참조 본문만으로 콘텐츠가 노출되지 않게 했다. 조회수는 SQL 원자적 증가 쿼리로 갱신한다.
- 실제 MySQL·MongoDB 컨테이너로 FK/유니크/CHECK, JSON 왕복, 필터·정렬·페이지, 가져오기·본문 연결·실패 롤백·동시 조회수 증가를 검증한다. S3·AI·이미지 변환은 테스트 대역이며 실 서비스 종단 검증은 별도다.

```sh
./gradlew test --tests 'com.linglevel.api.content.book.*' --tests 'com.linglevel.api.content.article.*' --tests 'com.linglevel.api.content.common.Catalog*'
./gradlew checkFormat
```

## 관련 문서

- [시스템 컨텍스트](overview.md)
- [기존 MongoDB 선택 배경](../decisions/007-choose-mongodb-for-early-flexibility.md)
