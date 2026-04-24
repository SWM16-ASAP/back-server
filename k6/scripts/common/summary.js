import { Rate, Trend } from 'k6/metrics';

export function createMetrics(metricPrefix) {
  return {
    prefix: metricPrefix,
    success: new Rate(`${metricPrefix}_success`),
    duration: new Trend(`${metricPrefix}_duration`, true),
  };
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

export function createSummaryHandler(metrics, metadata) {
  return function handleSummary(data) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const analysis = {
      test_info: metadata,
      metric_name: metrics.prefix,
      scenario_metrics: {
        success: metricSnapshot(data.metrics[`${metrics.prefix}_success`]),
        duration: metricSnapshot(data.metrics[`${metrics.prefix}_duration`]),
      },
      transport_metrics: {
        http_req_duration: metricSnapshot(data.metrics.http_req_duration),
        http_req_failed: metricSnapshot(data.metrics.http_req_failed),
        http_reqs: metricSnapshot(data.metrics.http_reqs),
        data_received: metricSnapshot(data.metrics.data_received),
      },
    };

    return {
      [`/reports/${metrics.prefix}-test-${timestamp}.json`]: JSON.stringify(data, null, 2),
      [`/reports/${metrics.prefix}-analysis-${timestamp}.json`]: JSON.stringify(analysis, null, 2),
    };
  };
}
