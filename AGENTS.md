# AGENTS.md

이 문서는 AI 도구가 이 저장소에서 코드 변경을 돕기 전에 먼저 읽을 프로젝트 맥락을 정리한다.
외부 방문자를 위한 소개는 루트 [README.md](README.md)를 기준으로 하고, 상세 구조와 판단 기록은 아래 문서를 따라간다.

## 먼저 볼 문서

1. [시스템 컨텍스트 다이어그램](docs/architecture/overview.md)
2. [Word 도메인 미니맵](docs/architecture/word.md)
3. [Book 도메인 미니맵](docs/architecture/content-book.md)
4. [Streak 도메인 미니맵](docs/architecture/streak.md)
5. [기술 의사결정 기록](docs/decisions/)

## 코드 작업 전 확인할 운영 포인트

- 주요 데이터 저장소는 MongoDB이며, Redis는 rate limit, 짧은 상태, 분산 조정에 사용한다.
- 단어 생성 경로는 Spring AI와 AWS Bedrock 호출 비용, 실패 재시도, 동시 요청 중복을 함께 고려해야 한다.
- `word` 동적 생성 경로는 Redisson `RLock` 기반 single-flight 조정 흐름을 가진다.
- 사용자 요청 경로에는 Bucket4j 기반 rate limiting이 적용될 수 있다.
- 이미지와 파일 저장은 S3/R2 계열 외부 저장소에 의존한다.
- 알림 기능은 Firebase Cloud Messaging에 의존한다.
- 성능 변경은 가능하면 k6 프로필이나 메트릭으로 전후 차이를 확인한다.

## 설계 및 운영 판단 기록

- [MongoDB 중심 데이터 모델링](docs/decisions/007-choose-mongodb-for-early-flexibility.md)
- [Redis 기반 Rate Limiting](docs/decisions/002-rate-limiting-with-bucket4j.md)
- [AI 단어 분석 파이프라인 비용과 안정성 개선](docs/decisions/005-ai-word-analysis-cost-and-reliability.md)
- [Word single-flight 분산 안정화](docs/decisions/011-word-single-flight-distributed-stability-with-redlock.md)
- [이미지 전달 최적화](docs/decisions/008-image-delivery-optimization.md)
- [DSL 기반 크롤링 규칙 관리](docs/decisions/009-dsl-driven-crawling.md)

## 변경 작업 원칙

- README는 외부 방문자용으로 유지하고, 긴 설계 설명은 `docs` 아래에 둔다.
- 문서화되지 않은 모듈은 `src/main/java/com/linglevel/api` 아래 실제 패키지와 테스트를 기준으로 맥락을 확인한다.
- 커밋 메시지, 브랜치명, PR 제목과 본문은 기존 저장소의 컨벤션을 먼저 확인한 뒤 같은 형식으로 작성한다.
- 구조 변경은 관련 architecture 문서와 decision 문서의 갱신 필요성을 함께 확인한다.
- 운영 리스크가 있는 변경은 테스트, 로그, 메트릭, 부하 테스트 중 최소 하나로 검증 근거를 남긴다.
- 외부 네트워크, AI 모델, 저장소, 푸시 알림에 의존하는 코드는 실패와 비용을 별도 리스크로 다룬다.
