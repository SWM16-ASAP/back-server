# Decision Records

이 디렉터리는 큰 기술적 선택과 개선 판단을 기록하는 문서 모음이며, 기술 블로그형 기록에 가까운 톤으로 유지한다.

## 기록 대상

- 구조를 바꾸는 결정
- 성능에 영향을 주는 결정
- 안정성과 운영성에 영향을 주는 결정
- 테스트 전략을 바꾸는 결정

## 작성 기준

- 자잘한 구현 선택은 기록하지 않는다.
- 문제, 선택, 이유, 검증, 결과와 남은 이슈 중심으로 짧게 정리한다.
- 하나의 문서에는 하나의 큰 결정만 기록한다.
- 구현 자체보다 왜 그런 선택을 했는지와 그 결과를 이해할 수 있게 적는다.

## 템플릿

- [의사결정 기록 템플릿](../templates/decision-record-template.md)

## 기록된 사례

- [001. Chapter 조회 N+1 문제를 Aggregation으로 해소](001-chapter-query-aggregation.md)
- [002. Bucket4j와 Redis 기반 Rate Limiting 도입](002-rate-limiting-with-bucket4j.md)
- [003. 개발 배포 환경을 온프레미스 기반으로 전환](003-migrate-dev-infrastructure-to-on-premise.md)
- [004. R2 서명 불일치를 피하기 위해 Chunked Encoding 비활성화](004-disable-r2-chunked-encoding.md)
- [005. AI 단어 분석 파이프라인의 비용과 안정성 개선](005-ai-word-analysis-cost-and-reliability.md)
- [006. 개인화된 추천 PUSH와 캠페인 추적 체계 도입](006-personalized-push-notification-and-tracking.md)
- [007. 서비스 초기 데이터 저장소를 MongoDB 중심으로 고정](007-choose-mongodb-for-early-flexibility.md)
- [008. 글로벌 이미지 전달 성능 최적화](008-image-delivery-optimization.md)
- [009. DSL 기반 크롤링 규칙 관리 구조 도입](009-dsl-driven-crawling.md)
