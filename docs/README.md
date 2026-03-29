# Project Documentation

이 디렉터리는 프로젝트 구조와 주요 고민을 정리하는 문서 허브다.

## 문서 구성

- [아키텍처 문서 모음](architecture/)
- [의사결정 기록 모음](decisions/)
- [템플릿 모음](templates/)

## 언제 무엇을 쓰는가

- 현재 구조, 도메인 관계, 대표 흐름을 정리할 때: [architecture](architecture/)
- 미션을 수행하며 내린 큰 판단과 고민을 미션당 문서 하나로 남길 때: [decisions](decisions/)
- 새 문서를 시작할 때: [templates](templates/)
- 활성 미션 상태 파일이 필요할 때: [mission-state-template.md](templates/mission-state-template.md) 를 복사해 루트 `MISSIONS.md`로 사용

## 기본 원칙

- 문서는 길게 쓰기보다 빠르게 읽히는 수준으로 유지한다.
- 자잘한 구현 선택보다 구조와 판단이 드러나는 내용만 기록한다.
- 같은 설명을 여러 문서에 반복하지 않고, 허브에서는 링크 중심으로 정리한다.
- 단순 변경 내역보다 문제 인식, 선택 이유, 결과와 남은 이슈가 드러나도록 정리한다.
- `decisions` 문서는 결과 자랑보다 고민 과정 공유에 가깝게 쓴다.
