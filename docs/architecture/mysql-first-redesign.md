# MySQL 중심 신규 시스템 재설계 계획

상태: 설계 초안, 구현 전. 아래 테이블명은 제안이며 상세 컬럼은 구현 전에 정한다.

새 DB로 시작한다. 기존 운영 데이터 이전·무중단 전환·운영 롤백은 범위에서 제외한다.
기존 운영 환경에 적용하려면 별도 데이터 마이그레이션이 필요하다.

## MySQL로 변경할 테이블

| 작업 단위 | 기존 MongoDB 모델 | MySQL 테이블안 | 변경 내용 |
| --- | --- | --- | --- |
| 사용자 | User | users | 사용자 기준 정보를 저장하고 다른 업무 테이블에서 참조 |
| 티켓 | UserTicket, TicketTransaction | ticket_wallets, ticket_transactions, ticket_reservations(신규) | 지갑·거래 내역·예약 분리. 생성 요청과 예약을 연결하고 예약→확정/해제 적용 |
| 커스텀 콘텐츠 | ContentRequest, CustomContent, UserCustomContent | content_requests, custom_contents, user_custom_contents | 요청·메타데이터·소유권 관리. 소유권 부여와 티켓 소비 확정을 함께 처리 |
| 책·아티클 | Book, Chapter, Article | books, chapters, articles | 카탈로그·챕터 구조를 관계형으로 관리하고 본문은 별도 참조 |
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
- 실제 변경된 테이블·API·ID·정책 및 신규 구동 방법을 이 문서에 갱신한다. 현재 API·ID 변경 여부는 미정이다.

## 관련 문서

- [시스템 컨텍스트](overview.md)
- [기존 MongoDB 선택 배경](../decisions/007-choose-mongodb-for-early-flexibility.md)
