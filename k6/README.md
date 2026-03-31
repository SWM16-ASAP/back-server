# K6 Performance Testing

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