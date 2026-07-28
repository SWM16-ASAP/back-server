import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import http from 'k6/http';
import { Rate } from 'k6/metrics';
import { check } from 'k6';

const baseUrl = (__ENV.BASE_URL || '').replace(/\/$/, '');
const word = __ENV.WORD || 'rabbit';
const targetLanguage = __ENV.WORD_TARGET_LANGUAGE || 'KO';
const jwtSecret = __ENV.JWT_SECRET || '';
const vus = Number(__ENV.WORD_VUS || 10);
const duration = __ENV.WORD_DURATION || '1m';
const clientTimeout = new Rate('client_timeout');

export const options = {
  scenarios: {
    word_single_flight: {
      executor: 'constant-vus',
      vus,
      duration,
      gracefulStop: '10s',
    },
  },
  thresholds: {
    checks: ['rate==1'],
    http_req_failed: ['rate==0'],
  },
};

function createTestJwt() {
  if (!jwtSecret) {
    throw new Error('JWT_SECRET is required for the Word single-flight scenario.');
  }

  const now = Math.floor(Date.now() / 1000);
  const header = encoding.b64encode(JSON.stringify({ alg: 'HS256', typ: 'JWT' }), 'rawurl');
  const payload = encoding.b64encode(JSON.stringify({
    sub: 'performance-test-user',
    id: '000000000000000000000001',
    email: 'performance-test@example.com',
    role: 'USER',
    provider: 'performance-test',
    display_name: 'Performance Test',
    iat: now,
    exp: now + 600,
  }), 'rawurl');
  const signingInput = `${header}.${payload}`;
  const signingKey = encoding.b64decode(jwtSecret, 'std');
  const signature = crypto.hmac('sha256', signingKey, signingInput, 'base64rawurl');

  return `${signingInput}.${signature}`;
}

export default function () {
  const path = `/api/v1/words/${encodeURIComponent(word)}?targetLanguage=${encodeURIComponent(targetLanguage)}`;
  const response = http.get(`${baseUrl}${path}`, {
    headers: { Authorization: `Bearer ${createTestJwt()}` },
    tags: { endpoint: 'word-single-flight' },
  });

  clientTimeout.add(response.error_code === 1050);

  check(response, {
    'Word generation succeeds': (result) => result.status === 200,
  });
}
