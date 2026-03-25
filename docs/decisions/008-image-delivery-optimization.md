# 글로벌 이미지 전달 성능 최적화

관련 PR: [#160](https://github.com/SWM16-ASAP/back-server/pull/160)

## 문제

정적 이미지가 미국 리전에 가까운 저장소 기준으로 제공되면서 아시아 사용자 입장에서는 로딩 지연이 컸다.
원본 이미지를 그대로 내려주면 파일 크기도 커서, 커버 이미지와 섬네일이 많은 화면에서 체감 지연이 더 커질 수 있었다.

## 선택

CDN과 Lambda@Edge 기반 WebP 변환 전략을 도입하고, 자주 쓰는 썸네일 크기는 서버에서 미리 256x256 WebP로 전처리해 저장하도록 구성했다.
즉 요청 시점의 동적 처리와, 자주 쓰는 규격의 사전 생성 전략을 함께 가져가는 혼합형 구조를 택했다.

## 이유

글로벌 사용자에게는 저장소 위치보다 최종 전달 경로 최적화가 더 중요하다.
동적 리사이징만 쓰면 유연하지만 cold start와 초기 변환 비용이 생기고, 전처리만 쓰면 다양한 크기 요구를 다 감당하기 어렵다.
그래서 일반 이미지는 CDN 경로 최적화를 쓰고, 반복적으로 많이 쓰는 썸네일은 별도 생성하는 편이 균형이 좋았다.

## 검증

- [#160](https://github.com/SWM16-ASAP/back-server/pull/160) 에서 CDN + Lambda@Edge + WebP 전환 방향과 `ImageResizeService` 도입 의도를 확인한다.
- [ImageResizeService.java](/Users/solfe/Desktop/WORK/llv/llv-api/src/main/java/com/linglevel/api/s3/service/ImageResizeService.java) 와 [ImageResizeServiceTest.java](/Users/solfe/Desktop/WORK/llv/llv-api/src/test/java/com/linglevel/api/s3/service/ImageResizeServiceTest.java) 에서 256x256 WebP 썸네일 생성과 업로드 경로를 확인한다.
- 성능 비교는 로컬 환경에서 k6로 동일 시나리오를 반복 실행하고, Grafana로 응답시간과 처리량을 모니터링하는 방식으로 측정했다.
- 그 기준에서 파일 크기는 1,473KB에서 13KB 수준까지 줄었고, 평균 응답시간은 883ms에서 27ms, 처리량은 23.7 RPS에서 734.3 RPS 수준까지 개선됐다.

## 결과와 남은 이슈

- CDN, Lambda@Edge, 저장소 구성은 코드 저장소 밖 인프라 설정도 포함하므로 별도 운영 문서와 함께 관리하는 편이 좋다.
- 이미지 종류별로 전처리 규격을 더 세분화할지, 혹은 WebP 외 포맷 대응을 늘릴지는 다시 검토할 수 있다.
- 현재 R2 호환성 같은 저장소 세부 이슈는 별도 기록으로 분리되어 있으므로, 상위 전략과 하위 호환성 기록을 함께 유지해야 한다.
