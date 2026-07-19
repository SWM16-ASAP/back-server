import http from 'k6/http';
import { check } from 'k6';

const baseUrl = (__ENV.BASE_URL || '').replace(/\/$/, '');
const healthPath = __ENV.HEALTH_PATH || '/actuator/health';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate==1'],
    http_req_failed: ['rate==0'],
  },
};

export default function () {
  const response = http.get(`${baseUrl}${healthPath}`, {
    tags: { endpoint: 'health' },
  });

  check(response, {
    'ALB target responds successfully': (result) => result.status >= 200 && result.status < 400,
  });
}
