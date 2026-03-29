# Ling Level Spring API

## 프로젝트 소개

Ling Level API는 학습 콘텐츠, 단어 학습, 스트릭, 추천, 알림 기능을 포함하는 Spring Boot 기반 백엔드 서버입니다.

이 프로젝트는 단순 기능 구현용 저장소가 아니라, 구조 개선, 성능 최적화, 안정성 강화, 의사결정 기록을 함께 관리하는 운영형 API 프로젝트를 목표로 합니다.

## 문서

- [프로젝트 문서 허브](docs/README.md)
- 활성 미션 상태 파일: 로컬 루트 `MISSIONS.md` (`docs/templates/mission-state-template.md` 기준)
- [아키텍처 문서 모음](docs/architecture/)
- [의사결정 기록 모음](docs/decisions/)

## 교육용 스킬

이 저장소는 미션 기반 학습을 위해 repo-local Codex skill을 함께 관리합니다.

- [mission-start](.codex/skills/mission-start/SKILL.md)
- [mission-guide](.codex/skills/mission-guide/SKILL.md)
- [mission-evaluate](.codex/skills/mission-evaluate/SKILL.md)
- [mission-interview](.codex/skills/mission-interview/SKILL.md)
- [mission-close](.codex/skills/mission-close/SKILL.md)

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
