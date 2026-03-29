---
name: mission-interview
description: Use when the user wants a mock interview after finishing a mission. Trigger on phrases like "모의 인터뷰하자", "인터뷰 질문 해줘", or "내가 설명하는 연습을 하고 싶어".
---

# Mission Interview

이 스킬은 방금 수행한 미션을 기반으로 모의 인터뷰를 진행하는 용도다.

## 반드시 할 일

1. [AGENTS.md](../../../AGENTS.md)를 먼저 읽고 `interviewer` 관점으로 전환한다.
2. 루트 `MISSIONS.md`가 있으면 직전 미션의 목표, 범위, 검증 계획, 남은 이슈를 먼저 복기하고, 없으면 [docs/templates/mission-state-template.md](../../../docs/templates/mission-state-template.md)를 기준으로 핵심 항목을 복원한다.
3. 관련 `docs/decisions` 문서가 있으면 완료된 판단과 결과를 보조적으로 확인한다.
4. 직전 미션의 핵심 맥락을 짧게 정리한다.
5. 질문은 아래 주제를 우선한다.
   - 왜 이 문제를 그렇게 정의했는가
   - 왜 그 설계를 택했는가
   - 대안은 무엇이었는가
   - 실패/장애/고트래픽 상황에서는 어떻게 되는가
   - 무엇을 다시 개선할 것인가
6. 질문은 한 번에 너무 많이 주지 말고, 답변 후 꼬리 질문이 가능하게 구성한다.

## 출력 원칙

- 실제 면접처럼 짧고 날카로운 질문을 우선한다.
- 기술 선택의 이유와 트레이드오프를 설명하게 만든다.
- 필요하면 마지막에 답변 품질에 대한 짧은 피드백을 준다.
