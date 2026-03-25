# DSL 기반 크롤링 규칙 관리 구조 도입

관련 근거: [#100](https://github.com/SWM16-ASAP/back-server/pull/100), [#120](https://github.com/SWM16-ASAP/back-server/pull/120), [#284](https://github.com/SWM16-ASAP/back-server/pull/284)

## 문제

특정 사이트를 크롤링해 콘텐츠를 재가공하는 구조에서는 사이트 DOM이 바뀌면 곧바로 장애가 발생할 수 있다.
게다가 학습용 자료는 저작권과 앱 구조 제약 때문에 클라이언트가 직접 크롤링해야 하는 경우가 있어, 앱 배포 없이 규칙을 바꿀 수 있어야 했다.

## 선택

HTML 추출 규칙을 코드에 하드코딩하지 않고 자체 DSL로 표현해 저장하고, 백엔드에서 도메인별 DSL을 관리하는 구조를 도입했다.
클라이언트와 서버는 같은 규칙 표현을 공유하고, 백엔드는 DSL 조회·검증·관리 API를 제공하는 방식으로 역할을 나눴다.

## 이유

사이트별 셀렉터를 코드에 직접 넣으면 사이트 변경 때마다 앱이나 서버를 다시 배포해야 한다.
반면 DSL을 저장하고 조회하는 구조로 가면, 규칙 자체를 데이터처럼 바꿔서 빠르게 대응할 수 있다.
이 방식은 크롤링 로직을 범용 엔진과 도메인별 규칙으로 분리하므로, 신규 사이트 추가와 장애 대응 속도 모두에 유리하다.

## 검증

- [CrawlerDsl.java](/Users/solfe/Desktop/WORK/llv/llv-api/src/main/java/com/linglevel/api/crawling/dsl/CrawlerDsl.java) 에서 셀렉터, 속성, fallback을 포함한 DSL 인터프리터 구조를 확인한다.
- [CrawlingController.java](/Users/solfe/Desktop/WORK/llv/llv-api/src/main/java/com/linglevel/api/crawling/controller/CrawlingController.java) 와 [AdminCrawlingController.java](/Users/solfe/Desktop/WORK/llv/llv-api/src/main/java/com/linglevel/api/admin/crawling/AdminCrawlingController.java) 에서 조회/검증/관리 API를 확인한다.
- [CrawlerDslTest.java](/Users/solfe/Desktop/WORK/llv/llv-api/src/test/java/com/linglevel/api/crawling/dsl/CrawlerDslTest.java) 로 규칙 해석과 fallback 추출이 동작하는지 검증한다.
- 운영 기준으로는 사이트 인터페이스가 바뀌어도 앱 강제 업데이트 없이 규칙만 바꿔 대응할 수 있는 구조를 확보했다.

## 결과와 남은 이슈

- DSL 문법이 커질수록 검증기와 에러 메시지 품질도 같이 좋아져야 운영이 편해진다.
- 외부 사이트 구조 변화 탐지와 규칙 실패 알림은 아직 더 자동화할 수 있다.
- 서버 크롤링, 클라이언트 크롤링, RSS fallback이 섞이는 영역은 추후 책임 경계를 더 선명하게 나눌 수 있다.
