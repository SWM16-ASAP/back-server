# Repository Conventions

이 문서는 브랜치, 커밋 메시지, PR 작성 형식을 정의한다.
새 작업을 시작하거나 PR을 만들 때는 최근 git log와 이 문서를 함께 확인한다.

## 브랜치명

기본 형식:

```text
<type>/<short-description>
```

사용하는 type:

- `feat`: 새 기능
- `fix`: 버그 수정
- `refactor`: 구조 개선 또는 리팩터링
- `docs`: 문서 변경
- `hotfix`: 긴급 수정
- `codex`: AI 에이전트가 분리 작업을 수행하는 임시 작업 브랜치

예시:

```text
docs/readme-update
refactor/optimize-books-domain
fix/stabilize-failing-tests
codex/readme-update
```

## 커밋 메시지

기본 형식:

```text
<type>(<scope>): <summary>
```

scope가 명확하지 않으면 생략한다.

```text
<type>: <summary>
```

사용하는 type:

- `feat`: 사용자 기능 추가
- `fix`: 버그 또는 회귀 수정
- `refactor`: 동작 변경 없는 구조 개선
- `docs`: 문서 변경
- `test`: 테스트 추가 또는 수정
- `chore`: 빌드, 설정, 운영 보조 변경

작성 원칙:

- summary는 영어 소문자 명령형에 가깝게 짧게 쓴다.
- 한 커밋은 하나의 의도를 담는다.
- 문서만 바꾼 경우 `docs:`를 사용한다.
- 도메인이 분명하면 `fix(word): ...`, `refactor(book): ...`처럼 scope를 붙인다.

예시:

```text
docs: refine project readme and agent context
docs(decisions): add single-flight stability closure report
fix(word): patch single-flight lock expiry
refactor(word): optimize indexes and transaction boundaries
```

## PR 제목

기본 형식은 커밋 메시지와 동일하게 둔다.
여러 커밋이 포함된 PR이면 전체 변경을 대표하는 제목을 쓴다.

예시:

```text
docs: refine project documentation entrypoints
refactor(word): optimize single-flight persistence path
```

## PR 본문

PR 본문은 [pull_request_template.md](../../.github/pull_request_template.md)를 따른다.
각 섹션은 비워두지 말고, 해당 내용이 없으면 `없음` 또는 `문서 변경이라 예시 없음`처럼 명시한다.

필수 섹션:

- `Summary`: 변경사항 요약
- `Problem`: 해결하려는 문제
- `Solution`: 해결 방법
- `Changes`: 주요 변경사항
- `Example`: 예시나 사용법
- `Related Issues`: 연관 이슈

## 코드 스타일

Java 포맷은 Spring Java Format을 기준으로 한다.
Spring Java Format은 Spring 프로젝트 스타일에 맞춘 formatter이며 Java indentation은 tab을 사용한다.

명령:

```bash
./gradlew checkFormat
./gradlew format
```

작성 원칙:

- 포맷 검사는 `checkFormat`으로 수행한다.
- 자동 포맷 적용은 `format`으로 수행한다.
- 포맷 변경은 기능 변경과 분리한다.
- 대규모 포맷 변경은 별도 PR로 분리한다.
