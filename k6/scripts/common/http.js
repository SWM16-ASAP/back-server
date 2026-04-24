import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = (__ENV.BASE_URL || 'http://host.docker.internal:8080').replace(/\/$/, '');
const TEST_USERNAME = __ENV.TEST_USERNAME || '';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';
const THINK_TIME = Number(__ENV.THINK_TIME || 1);

export function buildHeaders() {
  const headers = {
    Accept: 'application/json',
  };

  if (TEST_USERNAME) {
    headers['X-Test-Username'] = TEST_USERNAME;
  }

  if (AUTH_TOKEN) {
    headers.Authorization = `Bearer ${AUTH_TOKEN}`;
  }

  return headers;
}

export function getSharedTestInfo() {
  return {
    baseUrl: BASE_URL,
    testUsername: TEST_USERNAME || null,
    thinkTime: THINK_TIME,
  };
}

export function buildUrl(path, query = {}) {
  const normalizedPath = path.startsWith('http')
    ? path
    : `${BASE_URL}${path.startsWith('/') ? path : `/${path}`}`;
  const queryString = Object.entries(query)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join('&');

  if (!queryString) {
    return normalizedPath;
  }

  return `${normalizedPath}${normalizedPath.includes('?') ? '&' : '?'}${queryString}`;
}

export function requestEndpoint(endpoint, metrics, iteration) {
  const request = endpoint.buildRequest({ iteration });
  const url = buildUrl(request.path, request.query);
  const response = http.get(url, {
    headers: buildHeaders(),
    tags: {
      endpoint: endpoint.tag,
      variant: request.variant,
    },
  });

  const success = check(response, {
    [`${endpoint.tag} status is 200`]: (res) => res.status === 200,
    [`${endpoint.tag} response shape is valid`]: (res) => endpoint.validate(res),
  });

  metrics.success.add(success);
  metrics.duration.add(response.timings.duration);

  sleep(THINK_TIME);

  return response;
}
