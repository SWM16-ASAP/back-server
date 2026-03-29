---
name: mission-start
description: Use when the user explicitly starts a new backend training mission, asks for mission candidates, or wants to select the next practice topic in this repository. Trigger especially on phrases like "새로운 학습 시작하자", "다음 미션 추천해줘", or "이번엔 어떤 과제를 할까".
---

# Mission Start

이 스킬은 새 미션의 시작점을 명시적으로 여는 용도다.

## 반드시 할 일

1. [AGENTS.md](../../../AGENTS.md)를 먼저 읽고 현재 운영 규칙을 따른다.
2. 현재 브랜치와 작업 상태를 확인한다.
3. 현재 작업 브랜치가 `develop`에서 갈라진 작업 브랜치라면 `develop...HEAD`와 working tree 변경점을 먼저 리뷰한다.
4. 루트 `MISSIONS.md`가 있으면 직전 미션의 상태와 다음 시작점을 먼저 확인하고, 없으면 [docs/templates/mission-state-template.md](../../../docs/templates/mission-state-template.md)를 참고해 상태 파일을 바로 시작한다. 완료된 미션의 맥락이 필요할 때만 관련 `docs/decisions` 문서를 참고한다.
5. 현재 저장소 기준으로 미션 후보 2~4개를 제안한다.
6. 각 후보에 대해 아래만 짧게 제시한다.
   - 미션명
   - 왜 지금 적절한지
   - 학습 포인트
   - 난이도
7. 가능하면 추천 미션 1개를 함께 제시한다.
8. 미션이 선택되면 `MISSIONS.md`에 현재 활성 미션 상태를 바로 남길 수 있게 목표와 다음 시작점을 분명하게 정리한다.
9. 현재 PR의 변경점과 직접 연결되는 미션이 있으면 우선순위를 높인다.

## 출력 원칙

- 정답이나 구현 계획을 길게 풀지 않는다.
- 사용자가 선택할 수 있을 정도로만 정보를 준다.
- 미션은 한 번에 하나의 핵심 문제만 다루게 설계한다.
- 탐색 시간이 길어져도 괜찮으니, 미션 후보의 근거를 분명하게 만든다.
