# K6 Performance Testing

## 디렉토리 구조
```
k6/
├── docker-compose.yml     # K6 실행 환경
├── scripts/              # 테스트 스크립트
│   ├── smoke-test.js     # 기본 연결 테스트
│   ├── load-test.js      # 부하 테스트
│   └── stress-test.js    # 스트레스 테스트
├── data/                 # 테스트 데이터 파일
├── reports/              # 테스트 결과 리포트
└── README.md
```

## 테스트 실행

### 기본 연결 테스트 (Smoke Test)
```bash
docker-compose run --rm k6 run /scripts/smoke-test.js
```

### 부하 테스트 (Load Test)
```bash
docker-compose run --rm k6 run /scripts/load-test.js
```

### 커스텀 설정으로 실행
```bash
docker-compose run --rm k6 run /scripts/smoke-test.js --vus 10 --duration 1m
```

## 결과 확인
- 콘솔에서 실시간 확인
- `/reports` 폴더에 JSON 결과 저장
- Grafana 대시보드 연동 가능

## 네트워크 설정
- `host.docker.internal:8080`로 로컬 API 접근
- 운영 환경 테스트 시 URL 변경 필요