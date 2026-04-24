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

function getByPath(target, path) {
  if (!path) {
    return target;
  }

  return path.split('.').reduce((current, key) => current?.[key], target);
}

function validateArrayResponse(response, path = 'content') {
  try {
    const body = response.json();
    return Array.isArray(getByPath(body, path));
  } catch (error) {
    return false;
  }
}

const DEFAULT_LIMIT = Number(__ENV.DEFAULT_LIMIT || 20);
const DEFAULT_LANGUAGE_CODE = __ENV.LANGUAGE_CODE || 'EN';
const DEFAULT_SORT_BY = __ENV.SORT_BY || 'created_at';
const DEFAULT_PAGE = Number(__ENV.PAGE || 1);
const PROGRESS_FILTERS = splitCsv(__ENV.PROGRESS_FILTERS, 'NOT_STARTED,IN_PROGRESS,COMPLETED');
const PAGINATION_LIMITS = splitNumberCsv(__ENV.PAGINATION_LIMITS, '10,20,50,100');

function buildBooksBaseQuery() {
  return {
    languageCode: DEFAULT_LANGUAGE_CODE,
    sortBy: DEFAULT_SORT_BY,
    page: DEFAULT_PAGE,
    limit: DEFAULT_LIMIT,
  };
}

const endpointCatalog = {
  'books.default_list': {
    name: 'books.default_list',
    tag: 'books',
    buildRequest: () => ({
      path: '/api/v1/books',
      query: buildBooksBaseQuery(),
      variant: 'default_list',
    }),
    validate: (response) => validateArrayResponse(response, 'data'),
  },
  'books.progress_filter': {
    name: 'books.progress_filter',
    tag: 'books',
    buildRequest: ({ iteration }) => {
      const progress = PROGRESS_FILTERS[iteration % PROGRESS_FILTERS.length];
      return {
        path: '/api/v1/books',
        query: {
          ...buildBooksBaseQuery(),
          progress,
        },
        variant: `progress_${progress.toLowerCase()}`,
      };
    },
    validate: (response) => validateArrayResponse(response, 'data'),
  },
  'books.pagination': {
    name: 'books.pagination',
    tag: 'books',
    buildRequest: ({ iteration }) => {
      const limit = PAGINATION_LIMITS[iteration % PAGINATION_LIMITS.length];
      return {
        path: '/api/v1/books',
        query: {
          ...buildBooksBaseQuery(),
          limit,
        },
        variant: `pagination_limit_${limit}`,
      };
    },
    validate: (response) => validateArrayResponse(response, 'data'),
  },
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
    validate: (response) => (arrayPath ? validateArrayResponse(response, arrayPath) : true),
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
