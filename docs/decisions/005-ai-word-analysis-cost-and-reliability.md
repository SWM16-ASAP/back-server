# AI 단어 분석 파이프라인의 비용과 안정성 개선

관련 PR: [#185](https://github.com/SWM16-ASAP/back-server/pull/185), [#209](https://github.com/SWM16-ASAP/back-server/pull/209), [#211](https://github.com/SWM16-ASAP/back-server/pull/211)

## 문제

영어 학습 서비스의 핵심 기능인 단어 검색과 단어장 생성은 AI 응답 품질에 직접 의존했다.
초기 구조에서는 프롬프트가 흔들리면 품사나 변형 정보가 잘못 들어오고, 실패한 단어를 반복 호출하면서 비용도 계속 누적될 수 있었다.

## 선택

Spring AI와 Bedrock 기반 단어 분석 파이프라인을 도입하고, `homograph` 같은 까다로운 케이스를 기준으로 프롬프트를 강화했다.
동시에 `InvalidWord` 컬렉션으로 실패 단어를 기록하고, 3회 재시도 후 차단하는 방식으로 불필요한 재호출을 막았다.

## 이유

단어 검색은 단순 번역보다 원형, 변형, 품사, 예문이 함께 맞아야 실제 학습 기능으로 쓸 수 있다.
그래서 프롬프트를 튜닝하는 것만으로 끝내지 않고, 잘못된 enum 후처리와 실패 단어 캐시까지 묶어서 파이프라인 전체를 안정화하는 편이 맞았다.
비용 측면에서도 실패 단어를 계속 AI로 보내는 구조는 손해가 크기 때문에, 실패를 기록하고 차단하는 정책이 필요했다.

## 검증

- [WordAiService.java](../../src/main/java/com/linglevel/api/word/service/WordAiService.java) 에서 homograph, variant, 품사 예외 케이스를 포함한 구조화 프롬프트와 토큰 비용 로깅을 확인한다.
- [WordService.java](../../src/main/java/com/linglevel/api/word/service/WordService.java) 에서 `InvalidWord` 기반 3회 재시도 정책과 성공 시 캐시 제거 흐름을 확인한다.
- [WordServiceTest.java](../../src/test/java/com/linglevel/api/word/service/WordServiceTest.java) 로 원형/변형 저장과 AI 호출 경로를 검증한다.
- 비용 비교는 테스트 코드를 통해 10개 단어의 평균 input/output 토큰 소비량을 측정한 뒤, 모델별 단가로 환산하는 방식으로 잡았다.
- 그 기준에서 단어 1개 생성 비용은 약 10원에서 0.5원 수준으로 줄었고, 실패 단어 재호출까지 차단해 비용 누수를 줄였다.

## 결과와 남은 이슈

- 모델 선택과 비용 비교 결과는 코드가 아니라 실험 기록 성격이 강하므로 별도 측정 자료와 함께 관리하는 편이 좋다.
- 번역 fallback 같은 사용자 경험 보완 로직은 현재 저장소 문서와 별도로 다시 정리할 필요가 있다.
- 단어 분석 실패 유형을 더 세분화하면 프롬프트 수정과 운영 대응이 더 빨라질 수 있다.
