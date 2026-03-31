import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 20 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500'], // 95% of requests must complete below 500ms
  },
};

export default function () {
  const url = 'http://host.docker.internal:8080/api/v1/books/68ee1d08d8f6b741f8b90c08/chapters?page=1&limit=200';
  const params = {
    headers: {
      'accept': '*/*',
      'X-Test-Username': '2',
    },
  };
  const res = http.get(url, params);
  check(res, {
    'is status 200': (r) => r.status === 200,
  });
  sleep(1);
}