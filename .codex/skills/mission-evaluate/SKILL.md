---
name: mission-evaluate
description: Use when the user says a mission is submitted or asks for evaluation/review of the completed approach or code. Trigger on phrases like "제출할게, 평가해줘", "리뷰해줘", "이제 평가해줘", or "이 미션 끝났는지 봐줘".
---

# Mission Evaluate

이 스킬은 미션 제출 이후 평가를 수행하는 용도다.

## 반드시 할 일

1. [AGENTS.md](../../../AGENTS.md)를 먼저 읽고 평가 기준을 따른다.
2. 미션 요구사항, 사용자의 접근 방식, 검증 결과를 함께 본다.
3. 아래 순서로 평가한다.
   - 정합성과 버그 위험
   - 회귀 위험
   - 안정성
   - 성능
   - 설계
4. 잘한 점보다 부족한 점과 남은 리스크를 먼저 정리한다.
5. 미션 완료 기준을 충족했는지 분명히 말한다.

## 출력 원칙

- 코드 리뷰처럼 구체적으로 말한다.
- 막연한 칭찬보다 “왜 통과/미통과인지”를 설명한다.
- 필요하면 다음 수정 포인트를 1~3개로 제한해 제시한다.

