---
name: mission-close
description: Use when the user wants to close out a mission and write or update the mission document in docs/decisions. Trigger on phrases like "문서화하자", "회고 정리하자", or "decisions 문서로 남기자".
---

# Mission Close

이 스킬은 미션을 마무리하고 `docs/decisions` 문서를 정리하는 용도다.

## 반드시 할 일

1. [AGENTS.md](../../../AGENTS.md)를 먼저 읽고 문서화 원칙을 따른다.
2. [docs/decisions/README.md](../../../docs/decisions/README.md)와 [docs/templates/decision-record-template.md](../../../docs/templates/decision-record-template.md)를 참고한다.
3. 현재 미션이 새 문서인지, 기존 문서 갱신인지 먼저 판단한다.
4. 현재 PR/브랜치의 상태를 복원할 수 있도록 메타데이터를 남긴다.
   - 관련 PR/브랜치
   - 기준 브랜치
   - 현재 상태
   - 다음 시작점
5. 문서는 아래 흐름이 보이게 정리한다.
   - 문제
   - 선택
   - 이유
   - 검증
   - 결과와 남은 이슈

## 출력 원칙

- 결과 자랑보다 고민과 판단의 흐름을 먼저 드러낸다.
- 미션당 문서 하나 원칙을 우선한다.
- decision 문서를 다음 세션의 상태 저장소처럼 사용할 수 있게 쓴다.
- 다음 미션으로 이어질 남은 이슈가 있으면 짧게 남긴다.
