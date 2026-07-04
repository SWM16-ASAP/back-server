# K6 Performance Testing

`k6` 디렉토리는 Ling-Level API의 정량 부하 테스트 자산을 관리한다.
테스트 스크립트는 개별 요청보다 `baseline`, `load`, `stress`, `mixed-load` 같은 부하 프로필을 기준으로 둔다.

## 디렉토리 구조

```text
k6/
├── docker-compose.yml
├── README.md
├── reports/
├── scripts/
│   ├── baseline.js
│   ├── load.js
│   ├── stress.js
│   ├── mixed-load.js
│   ├── domains/
│   │   └── books.js
│   └── common/
│       ├── endpoints.js
│       ├── http.js
│       ├── profiles.js
│       └── summary.js
└── seed/
    └── README.md
```

## 역할

- `docker-compose.yml`: k6 실행과 InfluxDB 결과 저장을 위한 로컬 구성
- `scripts/`: 부하 프로필별 k6 실행 스크립트
- `scripts/domains/`: 도메인별 엔드포인트와 요청 variant 정의
- `scripts/common/`: 엔드포인트, HTTP 호출, 부하 프로필, 요약 리포트 공통 코드
- `seed/`: 성능 테스트용 데이터 생성 및 적재 가이드
- `reports/`: k6 실행 결과와 분석 JSON 보관 위치. 로컬 산출물이며 `.gitkeep` 외에는 커밋하지 않는다.

## 관리 원칙

- 테스트 파일은 요청 이름보다 부하 목적이 먼저 보이도록 둔다.
- 엔드포인트는 `scripts/domains/<domain>.js`에 정의하고 `scripts/common/endpoints.js`에서 등록한다.
- 대조군 비교 시에는 seed, 사용자, 정렬 조건, 부하 프로필을 고정한다.
- 실행 결과 JSON은 `reports/`에 저장하되, PR에는 테스트 로직과 분석에 필요한 요약만 남긴다.

## 리포트 파이프라인 계획

현재 `reports/`는 k6 원본 결과와 요약 JSON을 저장하는 위치로 사용한다. 이후에는 테스트 실행 단위별 디렉터리를 만들고,
테스트 조건(`manifest.json`), k6 원본 요약(`k6-summary.json`), 분석용 요약(`analysis.json`), 사람이 읽는
정리(`report.md`)를 함께 남기는 구조로 확장한다.

이 구조의 목적은 터미널 출력에 의존하지 않고, 사람과 AI가 같은 근거를 기준으로 성능 테스트 결과를 다시 분석할 수 있게
하는 것이다. 특히 단어 조회처럼 요청 경로가 여러 갈래로 나뉘는 테스트에서는 누락된 서버 지표와 추가 확인이 필요한
항목도 리포트에 명시한다.
