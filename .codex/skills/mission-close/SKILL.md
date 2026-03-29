---
name: mission-close
description: Use when the user wants to close out a mission, summarize the active state from MISSIONS.md, and write the final decision document in docs/decisions. Trigger on phrases like "문서화하자", "회고 정리하자", or "decisions 문서로 남기자".
---

# Mission Close

이 스킬은 미션을 마무리하고 `MISSIONS.md`의 진행 상태를 바탕으로 `docs/decisions` 문서를 정리하는 용도다.

## 반드시 할 일

1. [AGENTS.md](../../../AGENTS.md)를 먼저 읽고 문서화 원칙을 따른다.
2. 루트 `MISSIONS.md`를 먼저 읽고 현재 미션의 목표, 검증, 작업 로그, 남은 이슈를 복원한다. 파일이 없다면 [docs/templates/mission-state-template.md](../../../docs/templates/mission-state-template.md)를 참고해 필요한 항목을 먼저 정리한다.
3. [docs/decisions/README.md](../../../docs/decisions/README.md)와 [docs/templates/decision-record-template.md](../../../docs/templates/decision-record-template.md)를 참고한다.
4. 현재 미션이 새 문서인지, 기존 문서 갱신인지 먼저 판단한다.
5. 문서는 아래 흐름이 보이게 정리한다.
   - 문제
   - 선택
   - 이유
   - 검증
   - 결과와 남은 이슈
6. 필요하면 `MISSIONS.md`의 현재 미션 상태를 완료 기준에 맞게 정리하거나 다음 미션 준비 상태로 갱신한다.

## 출력 원칙

- 결과 자랑보다 고민과 판단의 흐름을 먼저 드러낸다.
- 미션당 문서 하나 원칙을 우선한다.
- decision 문서는 최종 회고와 판단 기록으로만 쓴다.
- 다음 미션으로 이어질 남은 이슈가 있으면 짧게 남긴다.
