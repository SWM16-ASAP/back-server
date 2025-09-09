# Ling Level Spring API

## 사전 요구사항

- JDK 17
- docker compose

## 로컬 실행 방법

> 주의 : `.env.local` 파일이 존재해야 합니다.

```bash
# Docker Compose를 사용한 실행
 ./gradlew clean build && docker-compose up
```

## 접속 정보

- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`

> Swagger UI는 local, dev 프로필에서만 활성화됩니다.

## 모니터링 (선택사항)

### 로컬 개발용 모니터링:
```bash
# 로컬 앱만 모니터링
docker-compose -f monitoring/docker-compose.monitoring-local.yml up -d

# 접속 정보
# Prometheus: http://localhost:9090
# Grafana: http://localhost:3000 (admin/admin123 또는 환경변수)
```

### 운영환경 모니터링:
```bash
# dev + prod 환경 통합 모니터링
docker-compose -f monitoring/docker-compose.monitoring-prod.yml up -d

# 접속 정보  
# Prometheus: http://localhost:9090
# Grafana: http://localhost:3000 (admin/admin123 또는 환경변수)
```