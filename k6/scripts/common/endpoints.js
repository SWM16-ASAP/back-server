import { booksEndpoints } from '../domains/books.js';

function splitCsv(value, fallback) {
  return (value || fallback)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function splitNumberCsv(value, fallback) {
  return splitCsv(value, fallback)
    .map((item) => Number(item))
    .filter((item) => Number.isFinite(item) && item > 0);
}

const endpointCatalog = {
  ...booksEndpoints,
};

function createCustomEndpoint() {
  const path = __ENV.ENDPOINT_PATH;
  const tag = __ENV.ENDPOINT_TAG || 'custom';
  const variant = __ENV.ENDPOINT_VARIANT || tag;
  const arrayPath = __ENV.ENDPOINT_EXPECTS_ARRAY_AT || '';

  return {
    name: 'custom.endpoint',
    tag,
    buildRequest: () => ({
      path,
      variant,
      query: {},
    }),
    validate: (response) => {
      if (!arrayPath) {
        return true;
      }

      try {
        const body = response.json();
        const value = arrayPath.split('.').reduce((current, key) => current?.[key], body);
        return Array.isArray(value);
      } catch (error) {
        return false;
      }
    },
  };
}

export function resolveEndpointByName(name) {
  const endpoint = endpointCatalog[name];

  if (!endpoint) {
    throw new Error(`Unknown endpoint: ${name}`);
  }

  return endpoint;
}

export function resolveSingleEndpoint() {
  if (__ENV.ENDPOINT_PATH) {
    return createCustomEndpoint();
  }

  return resolveEndpointByName(__ENV.ENDPOINT_NAME || 'books.default_list');
}

export function resolveEndpointSet() {
  if (__ENV.ENDPOINT_PATH) {
    return [createCustomEndpoint()];
  }

  return splitCsv(__ENV.ENDPOINT_NAMES, 'books.default_list,books.progress_filter,books.pagination')
    .map(resolveEndpointByName);
}

export function resolveWeights(count) {
  const weights = splitNumberCsv(__ENV.ENDPOINT_WEIGHTS, '');

  if (weights.length === count) {
    return weights;
  }

  return Array.from({ length: count }, () => 1);
}

export function selectWeightedEndpoint(endpoints, weights, iteration) {
  const totalWeight = weights.reduce((sum, value) => sum + value, 0);
  let cursor = iteration % totalWeight;

  for (let index = 0; index < endpoints.length; index += 1) {
    cursor -= weights[index];
    if (cursor < 0) {
      return endpoints[index];
    }
  }

  return endpoints[endpoints.length - 1];
}
