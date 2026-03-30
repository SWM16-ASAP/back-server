# Architecture Documents

이 디렉터리는 프로젝트의 구조와 핵심 흐름을 정리하는 문서 모음이다.

## 포함 대상

- 시스템 개요
- 도메인 관계
- 대표 요청 흐름
- 상태 전이
- 외부 시스템 연결 구조

## 작성 기준

- 클래스 전체 나열보다 도메인과 흐름을 우선 정리한다.
- Mermaid 다이어그램은 핵심 구조와 흐름만 표현한다.
- 문서는 실제 리팩터링과 성능 개선 판단에 도움이 되는 수준으로 유지한다.
- 구현 상세보다 “어디가 핵심이고 어디가 위험한지”가 먼저 보이게 적는다.

## 템플릿

- [아키텍처 템플릿](../templates/architecture-template.md)

## 현재 문서

- [프로젝트 전체 구조 개요](overview.md)
- [Streak 도메인 구조](streak.md)
- [Word 도메인 구조](word.md)
- [Book 도메인 구조](content-book.md)
- [MongoDB 논리 ERD (dbdiagram.io용 DBML)](mongodb-logical-erd.dbml)
