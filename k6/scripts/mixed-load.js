import { requestEndpoint, getSharedTestInfo } from './common/http.js';
import { resolveEndpointSet, resolveWeights, selectWeightedEndpoint } from './common/endpoints.js';
import { createProfileOptions } from './common/profiles.js';
import { createMetrics, createSummaryHandler } from './common/summary.js';

const endpoints = resolveEndpointSet();
const weights = resolveWeights(endpoints.length);
const metrics = createMetrics('mixed_load');

export const options = createProfileOptions('load', metrics.prefix);

export default function () {
  const endpoint = selectWeightedEndpoint(endpoints, weights, __ITER);
  requestEndpoint(endpoint, metrics, __ITER);
}

export const handleSummary = createSummaryHandler(metrics, {
  profileName: 'load',
  endpointNames: endpoints.map((endpoint) => endpoint.name),
  weights,
  ...getSharedTestInfo(),
});
