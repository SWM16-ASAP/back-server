# 미션 기반 Codex 에이전트 운영 규칙 정리

## 문제

이 저장소는 미션 기반 백엔드 훈련장으로 쓰고 싶었지만, 기존 에이전트 규칙은 실습 흐름보다 일반적인 저장소 운영 안내에 더 가까웠다.
그 결과 미션 선정, 힌트 제공, 평가, 인터뷰, 문서화가 하나의 학습 사이클로 연결되지 않았고, 긴 세션에서는 현재 미션 상태를 잃기 쉬웠다.
또한 미션 시작 시 현재 PR 변경점보다 추상적인 도메인 이름을 먼저 보게 되면, 실제 변경과 동떨어진 문제를 제안할 위험이 있었다.

## 선택

에이전트 규칙을 `미션 선정 -> 해결 진행 -> 제출 -> 평가 -> 인터뷰 -> 문서화` 사이클 기준으로 다시 정리하고, 각 단계에 대응하는 repo-local skill을 명시적으로 분리했다.
미션 시작은 `develop...HEAD`와 working tree 변경점 리뷰를 먼저 보도록 바꾸고, 활성 미션 상태는 루트 `MISSIONS.md`에서 관리하기로 했다.
`docs/decisions`는 미션 종료 후 핵심 판단과 검증 결과를 남기는 최종 기록으로만 사용한다.

## 이유

학습 품질은 좋은 정답보다 좋은 문제 정의와 일관된 피드백 루프에서 나온다.
그래서 에이전트가 “무엇을 구현할지”보다 “지금 어떤 미션 단계에 있는지”를 먼저 인식하게 만드는 편이 맞았다.
또한 현재 브랜치의 변경점을 먼저 보면 실제 PR과 연결된 미션을 제안할 수 있어, 미션의 정확도와 리뷰 가능성이 같이 좋아진다.
활성 상태와 최종 회고를 같은 문서에 섞으면 진행 중 메모와 완료된 판단의 책임이 흐려진다.
그래서 진행 중 상태는 `MISSIONS.md`에 두고, 미션 종료 후에만 `docs/decisions`로 요약하는 편이 더 단순하고 덜 흔들린다.

## 검증

- [AGENTS.md](../../AGENTS.md) 에서 역할, 단계, 세션 시작 규칙, 문서화 원칙이 미션 중심으로 정리되었는지 확인한다.
- [mission-state-template.md](../templates/mission-state-template.md) 에서 활성 미션 상태 파일의 기본 템플릿을 확인한다.
- [mission-start/SKILL.md](../../.codex/skills/mission-start/SKILL.md) 와 [mission-evaluate/SKILL.md](../../.codex/skills/mission-evaluate/SKILL.md) 에서 `develop...HEAD` 리뷰 우선 규칙과 `MISSIONS.md` 참조 흐름을 확인한다.
- [docs/decisions/README.md](README.md) 와 [decision-record-template.md](../templates/decision-record-template.md) 에서 종료 후 회고 문서 규칙을 확인한다.
- 실제 대화 dry-run으로 `mission-start`, `mission-evaluate`, `mission-close` 출력이 이전보다 덜 흔들리는지 검토한다.

## 결과와 남은 이슈

- 미션 기반 에이전트 설계의 방향과 출력 규칙은 한 문서 집합으로 정리되었고, 현재 PR 변경점 기반 미션 선정 흐름도 명시됐다.
- `MISSIONS.md`가 활성 미션 상태의 단일 소스로 추가됐고, decision 문서는 미션 종료 후 판단과 회고를 남기는 기록으로 정리됐다.
- `MISSIONS.md`와 mission 스킬 사이의 자동 동기화 규칙은 더 구체화할 수 있다.

## 연관 이슈 및 PR

- 관련 이슈: 없음
- 관련 PR: 없음
