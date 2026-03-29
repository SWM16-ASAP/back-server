---
name: mission-evaluate
description: Use when the user says a mission is submitted or asks for explicit evaluation of the completed mission. Trigger on phrases like "제출할게, 평가해줘", "이 미션 평가해줘", "이제 평가해줘", or "이 미션 끝났는지 봐줘".
---

# Mission Evaluate

이 스킬은 미션 제출 이후 평가를 수행하는 용도다.

## 반드시 할 일

1. [AGENTS.md](../../../AGENTS.md)를 먼저 읽고 평가 기준을 따른다.
2. 루트 `MISSIONS.md`에서 현재 미션의 요구사항, 범위, 검증 계획을 먼저 확인하고, 없으면 [docs/templates/mission-state-template.md](../../../docs/templates/mission-state-template.md)를 기준으로 필요한 평가 기준을 복원한다.
3. 현재 PR의 변경점, 사용자의 접근 방식, 실제 검증 결과를 함께 본다.
4. 관련 `docs/decisions` 문서가 있으면 완료된 유사 미션의 판단이나 참고 맥락만 보조적으로 확인한다.
5. 아래 순서로 평가한다.
   - 정합성과 버그 위험
   - 회귀 위험
   - 안정성
   - 성능
   - 설계
6. 잘한 점보다 부족한 점과 남은 리스크를 먼저 정리한다.
7. 미션 완료 기준을 충족했는지 분명히 말한다.

## 출력 원칙

- 코드 리뷰처럼 구체적으로 말한다.
- 막연한 칭찬보다 “왜 통과/미통과인지”를 설명한다.
- 필요하면 다음 수정 포인트를 1~3개로 제한해 제시한다.
