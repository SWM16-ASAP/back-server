# Architecture Documents

이 디렉터리는 리팩터링이나 성능 개선 전에 빠르게 구조를 파악하기 위한 미니맵 문서 모음이다.

## 문서 원칙

- 목적은 현재 구조를 빠르게 이해하는 것이다.
- 문서는 책임, 외부 의존성, 핵심 기능 흐름만 남긴다.
- 도메인별로 `핵심 용어 사전(용어/정의)`을 유지해 의미 흔들림을 줄인다.
- 도메인 의미에 영향을 주는 선택은 `간결 의사결정 기록`에 누적한다.
- 핵심 기능은 중요도와 이해 난이도를 기준으로 2~3개만 고른다.
- 도메인 내부 구현 상세나 세부 설계 판단은 `MISSIONS.md` 또는 `docs/decisions`에서 다룬다.

## 템플릿

- [아키텍처 템플릿](../templates/architecture-template.md)

## 현재 문서

- [시스템 컨텍스트 다이어그램](overview.md)
- [Streak 도메인 미니맵](streak.md)
- [Word 도메인 미니맵](word.md)
- [Book 도메인 미니맵](content-book.md)
- [MongoDB 논리 ERD (dbdiagram.io용 DBML)](mongodb-logical-erd.dbml)
