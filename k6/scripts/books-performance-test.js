import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || 'http://host.docker.internal:8080').replace(/\/$/, '');
const TEST_USERNAME = __ENV.TEST_USERNAME || '';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';
const LANGUAGE_CODE = __ENV.LANGUAGE_CODE || 'EN';
const SORT_BY = __ENV.SORT_BY || 'created_at';
const PAGE = Number(__ENV.PAGE || 1);
const DEFAULT_LIMIT = Number(__ENV.DEFAULT_LIMIT || 20);
const PAGINATION_LIMITS = (__ENV.PAGINATION_LIMITS || '10,20,50')
  .split(',')
  .map((value) => Number(value.trim()))
  .filter((value) => Number.isFinite(value) && value > 0);
const PROGRESS_FILTERS = (__ENV.PROGRESS_FILTERS || 'NOT_STARTED,IN_PROGRESS')
  .split(',')
  .map((value) => value.trim())
  .filter(Boolean);
const RUN_MODE = __ENV.RUN_MODE || 'all';
const TARGET_VUS = Number(__ENV.TARGET_VUS || 20);
const RAMP_UP_DURATION = __ENV.RAMP_UP_DURATION || '30s';
const STEADY_DURATION = __ENV.STEADY_DURATION || '1m';
const RAMP_DOWN_DURATION = __ENV.RAMP_DOWN_DURATION || '10s';
const THINK_TIME = Number(__ENV.THINK_TIME || 1);

const defaultListSuccess = new Rate('books_default_list_success');
const defaultListDuration = new Trend('books_default_list_duration', true);
const progressFilterSuccess = new Rate('books_progress_filter_success');
const progressFilterDuration = new Trend('books_progress_filter_duration', true);
const paginationSuccess = new Rate('books_pagination_success');
const paginationDuration = new Trend('books_pagination_duration', true);

function buildScenario(exec, startTime = '0s') {
  return {
    executor: 'ramping-vus',
    exec,
    startTime,
    stages: [
      { duration: RAMP_UP_DURATION, target: TARGET_VUS },
      { duration: STEADY_DURATION, target: TARGET_VUS },
      { duration: RAMP_DOWN_DURATION, target: 0 },
    ],
    gracefulRampDown: '5s',
  };
}

function buildScenarios() {
  switch (RUN_MODE) {
    case 'default_list':
      return {
        default_list: buildScenario('defaultListScenario'),
      };
    case 'progress_filter':
      return {
        progress_filter: buildScenario('progressFilterScenario'),
      };
    case 'pagination':
      return {
        pagination: buildScenario('paginationScenario'),
      };
    default:
      return {
        default_list: buildScenario('defaultListScenario', '0s'),
        progress_filter: buildScenario('progressFilterScenario', '2m'),
        pagination: buildScenario('paginationScenario', '4m'),
      };
  }
}

export const options = {
  scenarios: buildScenarios(),
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1000'],
    books_default_list_success: ['rate>0.95'],
    books_progress_filter_success: ['rate>0.95'],
    books_pagination_success: ['rate>0.95'],
    books_default_list_duration: ['p(95)<1000'],
    books_progress_filter_duration: ['p(95)<1000'],
    books_pagination_duration: ['p(95)<1000'],
  },
};

function buildHeaders() {
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

function toQueryString(params) {
  return Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join('&');
}

function buildBooksUrl(overrides = {}) {
  const query = {
    languageCode: LANGUAGE_CODE,
    sortBy: SORT_BY,
    page: PAGE,
    limit: DEFAULT_LIMIT,
    ...overrides,
  };

  return `${BASE_URL}/api/v1/books?${toQueryString(query)}`;
}

function requestBooks(url, variant, successMetric, durationMetric) {
  const response = http.get(url, {
    headers: buildHeaders(),
    tags: {
      endpoint: 'books',
      variant,
    },
  });

  const success = check(response, {
    'books status is 200': (res) => res.status === 200,
    'books response has items': (res) => {
      const body = res.json();
      return Array.isArray(body?.content);
    },
  });

  successMetric.add(success);
  durationMetric.add(response.timings.duration);

  sleep(THINK_TIME);
}

export function defaultListScenario() {
  const url = buildBooksUrl();
  requestBooks(url, 'default_list', defaultListSuccess, defaultListDuration);
}

export function progressFilterScenario() {
  const progress = PROGRESS_FILTERS[__ITER % PROGRESS_FILTERS.length];
  const url = buildBooksUrl({ progress });
  requestBooks(url, `progress_${progress.toLowerCase()}`, progressFilterSuccess, progressFilterDuration);
}

export function paginationScenario() {
  const limit = PAGINATION_LIMITS[__ITER % PAGINATION_LIMITS.length];
  const url = buildBooksUrl({ limit });
  requestBooks(url, `pagination_limit_${limit}`, paginationSuccess, paginationDuration);
}

function metricSnapshot(metric) {
  const values = metric?.values || {};
  return {
    avg: Math.round(values.avg || 0),
    min: Math.round(values.min || 0),
    max: Math.round(values.max || 0),
    p95: Math.round(values['p(95)'] || 0),
    p99: Math.round(values['p(99)'] || 0),
    rate: Math.round((values.rate || 0) * 10000) / 100,
    count: values.count || 0,
  };
}

export function handleSummary(data) {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');

  const analysis = {
    test_info: {
      runMode: RUN_MODE,
      baseUrl: BASE_URL,
      testUsername: TEST_USERNAME || null,
      page: PAGE,
      defaultLimit: DEFAULT_LIMIT,
      paginationLimits: PAGINATION_LIMITS,
      progressFilters: PROGRESS_FILTERS,
      targetVus: TARGET_VUS,
    },
    scenario_metrics: {
      default_list: {
        success: metricSnapshot(data.metrics.books_default_list_success),
        duration: metricSnapshot(data.metrics.books_default_list_duration),
      },
      progress_filter: {
        success: metricSnapshot(data.metrics.books_progress_filter_success),
        duration: metricSnapshot(data.metrics.books_progress_filter_duration),
      },
      pagination: {
        success: metricSnapshot(data.metrics.books_pagination_success),
        duration: metricSnapshot(data.metrics.books_pagination_duration),
      },
    },
    transport_metrics: {
      http_req_duration: metricSnapshot(data.metrics.http_req_duration),
      http_req_failed: metricSnapshot(data.metrics.http_req_failed),
      http_reqs: metricSnapshot(data.metrics.http_reqs),
      data_received: metricSnapshot(data.metrics.data_received),
    },
  };

  return {
    [`/reports/books-test-${timestamp}.json`]: JSON.stringify(data, null, 2),
    [`/reports/books-analysis-${timestamp}.json`]: JSON.stringify(analysis, null, 2),
  };
}
