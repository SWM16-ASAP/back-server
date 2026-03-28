---
name: mission-start
description: Use when the user explicitly starts a new backend training mission, asks for mission candidates, or wants to select the next practice topic in this repository. Trigger especially on phrases like "새로운 학습 시작하자", "다음 미션 추천해줘", or "이번엔 어떤 과제를 할까".
---

# Mission Start

이 스킬은 새 미션의 시작점을 명시적으로 여는 용도다.

## 반드시 할 일

1. [AGENTS.md](../../../AGENTS.md)를 먼저 읽고 현재 운영 규칙을 따른다.
2. 현재 브랜치와 작업 상태를 확인한다.
3. 직전 미션이나 최근 `docs/decisions`에서 이어서 볼 만한 맥락이 있으면 짧게 상기한다.
4. 현재 저장소 기준으로 미션 후보 2~4개를 제안한다.
5. 각 후보에 대해 아래만 짧게 제시한다.
   - 미션명
   - 왜 지금 적절한지
   - 학습 포인트
   - 난이도
6. 가능하면 추천 미션 1개를 함께 제시한다.

## 출력 원칙

- 정답이나 구현 계획을 길게 풀지 않는다.
- 사용자가 선택할 수 있을 정도로만 정보를 준다.
- 미션은 한 번에 하나의 핵심 문제만 다루게 설계한다.

