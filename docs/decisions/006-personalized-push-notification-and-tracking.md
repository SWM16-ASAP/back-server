# 개인화된 추천 PUSH와 캠페인 추적 체계 도입

## 미션 메타데이터

- 관련 PR: [#222](https://github.com/SWM16-ASAP/back-server/pull/222), [#253](https://github.com/SWM16-ASAP/back-server/pull/253), [#288](https://github.com/SWM16-ASAP/back-server/pull/288)
- 작업 브랜치: `미상`
- 기준 브랜치: `develop`
- 현재 상태: 개인화 PUSH와 캠페인 추적 체계 도입 회고 정리 완료. 실험 설계와 빈도 제한 정책은 남아 있다.
- 다음 시작점: 캠페인별 실험 기준과 A/B 구조를 정리하고, 사용자 피로도 기반 알림 빈도 제한 정책을 설계한다.

## 문제

기존 PUSH 알림은 일괄 리마인드 성격이 강해서 사용자별 선호나 학습 패턴을 충분히 반영하지 못했다.
또한 캠페인 단위로 송신 성공률과 오픈율을 추적하기 어려워, 어떤 알림이 실제 리텐션에 기여하는지 판단할 근거도 약했다.

## 선택

사용자의 콘텐츠 접근 로그를 모아 카테고리 선호도를 계산하고, 그 결과를 알림 대상 선정과 메시지 구성에 활용하는 구조를 도입했다.
동시에 FCM 전송 경로에 `campaignId`, 송신 로그, 오픈 리포트, 통계 API를 넣어 캠페인 단위 성과를 추적하도록 정리했다.

## 이유

리텐션을 높이려면 푸시를 많이 보내는 것보다, 어떤 사용자가 무엇에 반응하는지 추적 가능한 구조가 먼저 필요하다.
선호도 집계와 캠페인 추적이 같이 있어야 추천 알림의 효과를 비교할 수 있고, 운영 중 정책을 바꿔도 근거 있는 조정이 가능하다.
또한 단건 위주 전송이 아니라 배치 전송과 로그 저장을 같이 최적화해야 실제 대량 발송에서도 병목이 덜 생긴다.

## 검증

- [UserPreferenceAggregationScheduler.java](../../src/main/java/com/linglevel/api/content/recommendation/scheduler/UserPreferenceAggregationScheduler.java) 에서 최근 90일 로그 기반 선호도 집계와 시간/읽기시간 가중치를 확인한다.
- [NotificationService.java](../../src/main/java/com/linglevel/api/admin/service/NotificationService.java) 에서 선호 카테고리 기반 알림 발송과 국가별 메시지 분기를 확인한다.
- [FcmMessagingService.java](../../src/main/java/com/linglevel/api/fcm/service/FcmMessagingService.java), [PushLogService.java](../../src/main/java/com/linglevel/api/fcm/service/PushLogService.java), [PushCampaignService.java](../../src/main/java/com/linglevel/api/fcm/service/PushCampaignService.java) 에서 `campaignId`, 배치 전송, 송신/오픈 로깅, 통계 집계를 확인한다.
- 리텐션 수치는 AppsFlyer 기준으로 확인했고, 2025년 10월 27일부터 11월 2일까지의 전체 유저 주간 리텐션을 비교 지표로 사용했다.
- 그 기준에서 주간 리텐션은 16.67%에서 32.24%까지 개선되었고, FCM 배치 전송 속도와 캠페인 단위 분석 가능성도 함께 좋아졌다.

## 결과와 남은 이슈

- 리텐션 상승은 제품 전체 변화의 영향도 섞일 수 있으므로, 캠페인별 실험 기준을 더 분리할 필요가 있다.
- 추천 점수와 메시지 전략을 실험할 A/B 구조는 아직 별도 체계가 없다.
- 사용자 피로도와 알림 빈도 제한 정책은 추후 rate limit 성격으로 다시 정리할 수 있다.
