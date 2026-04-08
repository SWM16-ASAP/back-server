# 시스템 컨텍스트 다이어그램

## 현재 시스템의 책임

- 모바일 학습 앱의 API를 제공한다.
- 책 읽기, 단어 조회, 스트릭 유지 같은 핵심 학습 기능을 한 애플리케이션에서 처리한다.
- 추천, 알림, 크롤링, 파일 처리 같은 보조 기능도 함께 운영한다.

## 핵심 용어 사전

| 용어 | 정의 |
| --- | --- |
| 학습 콘텐츠 | 사용자가 소비하는 책/아티클/커스텀 학습 단위 |
| 진행도 | 사용자의 현재 학습 위치를 수치화한 상태 |
| 스트릭 | 사용자 학습 연속성(일 단위 유지 상태) |
| 보조 기능 | 추천/알림/크롤링처럼 핵심 학습 흐름을 지원하는 기능 |

## 외부 시스템 의존성

- MongoDB: 주요 도메인 데이터와 로그 저장
- Redis: 읽기 세션과 짧은 상태 관리
- S3 / R2: 이미지와 파일 저장
- AI Model: 단어 분석과 생성 요청
- FCM: 푸시 알림 발송
- External Content Sites: 크롤링 대상

## 핵심 기능

- 책 읽기와 진행도 반영
- 단어 조회와 AI 기반 보완
- 스트릭 계산과 보상/알림 처리

## 시스템 컨텍스트 다이어그램

```mermaid
flowchart TD
    Client[Client App]
    Admin[Admin]
    Api[Spring Boot API]

    Streak[Streak Domain]
    Word[Word Domain]
    Book[Book Domain]
    Recommend[Recommendation / Notification]
    Crawl[Crawling / Feed]

    Mongo[(MongoDB)]
    Redis[(Redis)]
    S3[(S3 / R2)]
    AI[AI Model]
    FCM[Firebase Cloud Messaging]
    External[External Content Sites]

    Client --> Api
    Admin --> Api

    Api --> Streak
    Api --> Word
    Api --> Book
    Api --> Recommend
    Api --> Crawl

    Streak --> Mongo
    Streak --> Redis
    Streak --> FCM

    Word --> Mongo
    Word --> AI

    Book --> Mongo
    Book --> S3
    Book --> Streak

    Recommend --> Mongo
    Recommend --> FCM

    Crawl --> Mongo
    Crawl --> External
```

## 핵심 기능 선정 기준

1. 실제 사용자 요청이 자주 통과하는 기능이다.
2. 외부 의존성이나 도메인 결합이 있어 이해 난이도가 높다.
3. 리팩터링이나 성능 개선 시 영향 범위가 큰 영역이다.

## 간결 의사결정 기록

| 날짜 | 결정 | 이유 | 영향 범위 | 상태 |
| --- | --- | --- | --- | --- |
| 2026-04-08 | 시스템 문서는 미니맵 중심으로 유지 | 전체 구조를 빠르게 파악하기 위한 목적 우선 | docs/architecture/* | 유지 |
