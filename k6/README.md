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
- `scripts/common/`: 엔드포인트, HTTP 호출, 부하 프로필, 요약 리포트 공통 코드
- `seed/`: 성능 테스트용 데이터 생성 및 적재 가이드
- `reports/`: k6 실행 결과와 분석 JSON 보관 위치

## 관리 원칙

- 테스트 파일은 요청 이름보다 부하 목적이 먼저 보이도록 둔다.
- 엔드포인트는 `scripts/common/endpoints.js`에서 관리하거나 환경변수로 주입한다.
- 대조군 비교 시에는 seed, 사용자, 정렬 조건, 부하 프로필을 고정한다.
