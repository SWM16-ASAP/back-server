function positiveNumber(value, fallback) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function buildRampingScenario(stages, exec = 'default') {
  return {
    executor: 'ramping-vus',
    exec,
    stages,
    gracefulRampDown: '5s',
  };
}

const BASELINE_VUS = positiveNumber(__ENV.BASELINE_VUS, 5);
const TARGET_VUS = positiveNumber(__ENV.TARGET_VUS, 20);
const STRESS_TARGET_VUS = positiveNumber(__ENV.STRESS_TARGET_VUS, 60);

export function createProfileScenario(profileName, exec = 'default') {
  switch (profileName) {
    case 'baseline':
      return buildRampingScenario([
        { duration: __ENV.BASELINE_RAMP_UP_DURATION || '15s', target: BASELINE_VUS },
        { duration: __ENV.BASELINE_STEADY_DURATION || '45s', target: BASELINE_VUS },
        { duration: __ENV.BASELINE_RAMP_DOWN_DURATION || '10s', target: 0 },
      ], exec);
    case 'stress':
      return buildRampingScenario([
        { duration: __ENV.STRESS_RAMP_UP_DURATION || '20s', target: TARGET_VUS },
        { duration: __ENV.STRESS_STEP_DURATION || '30s', target: STRESS_TARGET_VUS },
        { duration: __ENV.STRESS_STEADY_DURATION || '30s', target: STRESS_TARGET_VUS },
        { duration: __ENV.STRESS_RAMP_DOWN_DURATION || '15s', target: 0 },
      ], exec);
    case 'load':
    default:
      return buildRampingScenario([
        { duration: __ENV.RAMP_UP_DURATION || '30s', target: TARGET_VUS },
        { duration: __ENV.STEADY_DURATION || '1m', target: TARGET_VUS },
        { duration: __ENV.RAMP_DOWN_DURATION || '10s', target: 0 },
      ], exec);
  }
}

export function createProfileOptions(profileName, metricPrefix, exec = 'default') {
  return {
    scenarios: {
      [profileName]: createProfileScenario(profileName, exec),
    },
    thresholds: {
      http_req_failed: ['rate<0.05'],
      http_req_duration: ['p(95)<1000'],
      [`${metricPrefix}_success`]: ['rate>0.95'],
      [`${metricPrefix}_duration`]: ['p(95)<1000'],
    },
  };
}
