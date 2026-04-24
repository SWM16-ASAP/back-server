# K6 Seed Scripts

이 디렉토리에는 로컬 `k6` 성능 실험용 시드 생성 스크립트를 둔다.
도메인 구조가 바로 드러나도록 `k6/seed/<domain>/<subdomain>/...` 형태를 기본 원칙으로 사용한다.

## 디렉토리 원칙

- `k6/seed/content/book/...`
- `k6/seed/streak/...`
- `k6/seed/word/...`

생성 결과물이나 임시 데이터는 `k6/data`에 두고, 버전 관리 대상은 `k6/seed` 아래에 둔다.

## 포함된 스크립트

- `content/book/seed-books-content.mongosh.js`
  - `user`, `books`, `chapters`, `chunks`, `bookProgress` 컬렉션에 재실행 가능한 업서트 시드를 넣는다.
  - 기본 분포는 `short / medium / long` 책 구성을 섞고, 챕터 수와 청크 수를 현실적인 범위로 퍼뜨린다.
  - 같은 콘텐츠 그래프를 기준으로 `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED` 상태가 사용자별로 자연스럽게 섞인 `bookProgress`를 함께 생성한다.

## 실행

로컬 MongoDB 컨테이너가 떠 있는 상태에서 실행한다.

```bash
docker compose exec -T mongo mongosh llv_api_local < k6/seed/content/book/seed-books-content.mongosh.js
```

환경변수를 조정해서 규모를 바꿀 수 있다.

```bash
SEED_PREFIX=k6seed \
BOOK_COUNT=180 \
USER_COUNT=4 \
EXTRA_DIFFICULTY_RATIO=0.25 \
docker compose exec -T mongo mongosh llv_api_local < k6/seed/content/book/seed-books-content.mongosh.js
```

같은 prefix 데이터만 지우고 다시 만들고 싶으면 `RESET_EXISTING=true` 를 함께 준다.

```bash
RESET_EXISTING=true \
docker compose exec -T mongo mongosh llv_api_local < k6/seed/content/book/seed-books-content.mongosh.js
```

## 기본 특성

- 책 수 기본값: `240`
- 사용자 수 기본값: `4`
- 책 길이 분포:
  - `short`: 20%
  - `medium`: 60%
  - `long`: 20%
- 기본 난이도 기준 챕터당 평균 청크 수는 약 `30개`를 목표로 둔다.
- 프로필별 청크 범위:
  - `short`: 18~24
  - `medium`: 26~34
  - `long`: 34~42
- 각 책은 기본 난이도 1개를 갖고, 일부는 인접 난이도 청크를 추가로 가진다.
- 이미지 청크는 소량만 섞어서 응답 shape 를 단조롭게 만들지 않는다.
- `bookProgress`는 사용자 프로필별로 분포를 다르게 준다.
  - `mostly-unread`
  - `balanced`
  - `active-reader`
  - `completion-heavy`
- `NOT_STARTED`는 progress 문서를 만들지 않는 방식으로 표현한다.
- `IN_PROGRESS`는 `chapterProgresses`와 `maxReadChunkNumber`를 함께 채워서 현재 필터와 V3 응답 계산이 모두 자연스럽게 동작하게 한다.

## 주의

- 현재 스크립트는 `books` 콘텐츠 그래프와 `bookProgress`를 함께 만든다.
- 이미 같은 prefix로 넣은 시드를 다시 깔끔하게 만들고 싶으면 `RESET_EXISTING=true`로 재실행한다.
