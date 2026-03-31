import { requestEndpoint, getSharedTestInfo } from './common/http.js';
import { resolveSingleEndpoint } from './common/endpoints.js';
import { createProfileOptions } from './common/profiles.js';
import { createMetrics, createSummaryHandler } from './common/summary.js';

const endpoint = resolveSingleEndpoint();
const metrics = createMetrics('stress');

export const options = createProfileOptions('stress', metrics.prefix);

export default function () {
  requestEndpoint(endpoint, metrics, __ITER);
}

export const handleSummary = createSummaryHandler(metrics, {
  profileName: 'stress',
  endpointName: endpoint.name,
  ...getSharedTestInfo(),
});
