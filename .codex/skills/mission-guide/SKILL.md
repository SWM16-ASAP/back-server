---
name: mission-guide
description: Use when the user is actively solving the current mission and asks for hints, guidance, code reading order, or design help without wanting the full solution. Trigger on phrases like "힌트 줘", "가이드해줘", "어디부터 봐야 해", or "이 접근이 맞아?".
---

# Mission Guide

이 스킬은 현재 미션 해결 중 코칭을 제공하는 용도다.

## 반드시 할 일

1. [AGENTS.md](../../../AGENTS.md)를 먼저 읽고 현재 미션 단계가 `guide` 관점인지 확인한다.
2. 루트 `MISSIONS.md`가 있으면 현재 목표, 진행 상태, 다음 시작점을 먼저 확인하고, 없으면 [docs/templates/mission-state-template.md](../../../docs/templates/mission-state-template.md)를 참고해 필요한 섹션을 먼저 세운다.
3. 완료된 미션의 과거 판단이 필요할 때만 관련 `docs/decisions` 문서를 보조 자료로 참고한다.
4. 사용자의 현재 가설, 설계, 구현 상태를 먼저 요약한다.
5. 바로 정답을 주지 말고 아래 순서를 우선한다.
   - 질문
   - 코드 위치
   - 테스트/엣지 케이스
   - 설계 방향
   - 최소 예시
6. 필요하면 함께 봐야 할 개념을 최대 3개 이하로만 연결한다.

## 출력 원칙

- 힌트는 사용자가 스스로 다음 액션을 정할 수 있을 정도로만 준다.
- 구현보다 영향 범위, 테스트, 리스크를 먼저 짚는다.
- “이대로 고쳐라”보다 “무엇을 확인해야 하는가”를 먼저 말한다.
