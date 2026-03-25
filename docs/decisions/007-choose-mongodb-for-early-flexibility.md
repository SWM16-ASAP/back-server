# 서비스 초기 데이터 저장소를 MongoDB 중심으로 고정

## 문제

서비스 초기에는 기능 요구사항과 응답 형태가 빠르게 바뀌는 반면, 운영 비용은 제한적이었다.
이 시점에 관계형 DB와 문서형 DB를 함께 운영하면 관리 비용이 커지고, 빠른 스키마 변경에도 부담이 생길 수 있었다.

## 선택

초기 주 데이터 저장소를 MongoDB로 고정하고, 콘텐츠·단어·로그·추천 데이터까지 문서형 모델 중심으로 설계했다.
즉 익숙한 MySQL을 병행하기보다, 초기 단계에서는 MongoDB 단일 축으로 빠르게 전개하는 방향을 택했다.

## 이유

초기 제품에서는 정규화보다 요구사항 변화에 빠르게 적응하는 편이 더 중요할 때가 많다.
이 프로젝트는 콘텐츠 구조, AI 결과 구조, 로그 구조가 자주 바뀌는 편이라 문서형 저장소가 자연스럽게 맞았다.
또한 초기에 저장소를 하나로 단순화하면 인프라 복잡도와 운영 비용을 동시에 낮출 수 있다.

## 검증

- [application.properties](/Users/solfe/Desktop/WORK/llv/llv-api/src/main/resources/application.properties) 와 각 프로필 설정에서 MongoDB가 주 저장소로 사용되는 구성을 확인한다.
- [AbstractDatabaseTest.java](/Users/solfe/Desktop/WORK/llv/llv-api/src/test/java/com/linglevel/api/common/AbstractDatabaseTest.java) 기준으로 MongoDB 로컬/테스트 환경이 먼저 정착된 흐름을 확인한다.
- 콘텐츠, 단어, 추천, 로그 관련 엔티티와 리포지토리들이 MongoDB 문서 모델을 중심으로 구성된 현재 구조를 통해 초기 선택의 방향성을 확인할 수 있다.
- 실제 운영 관점에서는 스키마 변화와 신규 기능 추가 시 테이블 재설계 부담 없이 빠르게 적응할 수 있었다.

## 결과와 남은 이슈

- 조회가 복잡해질수록 Aggregation, 인덱스, 문서 구조 튜닝이 더 중요해지므로 MongoDB의 비용이 뒤늦게 커질 수 있다.
- 관계형 쿼리에 더 잘 맞는 영역이 생기면 저장소를 분리할지 다시 판단해야 한다.
- TTL, 벡터 저장, 로그 저장 등 MongoDB 활용 범위는 실제 운영 패턴에 맞춰 더 명확히 구분할 필요가 있다.
