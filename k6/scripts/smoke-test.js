import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 1,
  stages: [
    { duration: "1m", target: 10 },
    { duration: "3m", target: 10 },
    { duration: "1m", target: 50 },
    { duration: "3m", target: 50 },
    { duration: "1m", target: 100 },
    { duration: "3m", target: 100 },
    { duration: "1m", target: 200 },
    { duration: "3m", target: 200 },
    { duration: "1m", target: 300 },
    { duration: "3m", target: 300 },
    { duration: "1m", target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95%가 500ms 이하
    http_req_failed: ['rate<0.1'],    // 에러율 10% 이하
  },
};

export default function () {
  // Health check
  const healthRes = http.get('http://host.docker.internal:8080/actuator/health');
  check(healthRes, {
    'health check status is 200': (r) => r.status === 200,
  });

  // API 테스트 예시
  // const apiRes = http.get('http://host.docker.internal:8080/api/some-endpoint');
  // check(apiRes, {
  //   'api status is 200': (r) => r.status === 200,
  //   'response time < 200ms': (r) => r.timings.duration < 200,
  // });
}