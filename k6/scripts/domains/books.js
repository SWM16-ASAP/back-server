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

export const booksEndpoints = {
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
