---
name: mission-guide
description: Use when the user is actively solving the current mission and asks for hints, guidance, code reading order, or design help without wanting the full solution. Trigger on phrases like "힌트 줘", "가이드해줘", "어디부터 봐야 해", or "이 접근이 맞아?".
---

# Mission Guide

이 스킬은 현재 미션 해결 중 코칭을 제공하는 용도다.

## 반드시 할 일

1. [AGENTS.md](../../../AGENTS.md)를 먼저 읽고 현재 미션 단계가 `guide` 관점인지 확인한다.
2. 관련 `docs/decisions` 문서가 있으면 현재 PR/브랜치의 목표와 남은 이슈를 먼저 확인한다.
3. 사용자의 현재 가설, 설계, 구현 상태를 먼저 요약한다.
4. 바로 정답을 주지 말고 아래 순서를 우선한다.
   - 질문
   - 코드 위치
   - 테스트/엣지 케이스
   - 설계 방향
   - 최소 예시
5. 필요하면 함께 봐야 할 개념을 최대 3개 이하로만 연결한다.

## 출력 원칙

- 힌트는 사용자가 스스로 다음 액션을 정할 수 있을 정도로만 준다.
- 구현보다 영향 범위, 테스트, 리스크를 먼저 짚는다.
- “이대로 고쳐라”보다 “무엇을 확인해야 하는가”를 먼저 말한다.
