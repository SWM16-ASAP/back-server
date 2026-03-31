import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

const imageLoadRate = new Rate('image_load_success');
const imageLoadTime = new Trend('image_load_duration', true);
const imageSizeMetric = new Trend('image_size_bytes', true);
const cacheHitRate = new Rate('cache_hit_rate');
const totalRequests = new Counter('total_requests');

export let options = {
  stages: [
    { duration: '30s', target: 5 },   // 워밍업
    { duration: '30s', target: 20 },   // 부하 증가
    { duration: '1m', target: 50 },   // 최대 부하 유지
    { duration: '30s', target: 0 },   // 종료
  ],
  thresholds: {
    'image_load_success': ['rate > 0.95'],
    'image_load_duration': ['p(95) < 5000'],
    'http_req_duration': ['p(95) < 3000'],
    'http_req_failed': ['rate < 0.05'],
  },
};

// 테스트할 URL (직접 수정해서 사용)
const IMAGE_URL = 'https://static.linglevel.com/cozy_sofa.jpg.webp';

export default function () {
  const startTime = new Date();

  const response = http.get(IMAGE_URL, {
    headers: {
      'User-Agent': 'k6-image-performance-test',
      'Accept': 'image/webp,image/jpeg,image/png,image/*,*/*;q=0.8',
      'Accept-Encoding': 'gzip, deflate, br',
    },
    timeout: '30s',
  });

  const endTime = new Date();
  const loadTime = endTime - startTime;
  const imageSize = response.body ? response.body.length : 0;

  // 캐시 상태 확인 (여러 CDN 헤더 지원)
  const cacheHeaders = [
    response.headers['X-Cache'],
    response.headers['CF-Cache-Status'],
    response.headers['X-Amz-Cf-Id'],
    response.headers['X-Cache-Status'],
    response.headers['Cache-Control']
  ].filter(Boolean);

  const cacheStatus = cacheHeaders.join(', ') || 'no-cache-info';
  const isCacheHit = cacheStatus.toLowerCase().includes('hit') ||
                     cacheStatus.toLowerCase().includes('edge') ||
                     cacheStatus.toLowerCase().includes('cloudfront');

  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'content-type is image': (r) => {
      const contentType = r.headers['Content-Type'] || '';
      return contentType.includes('image');
    },
    'response body size > 0': (r) => r.body && r.body.length > 0,
    'load time < 10s': () => loadTime < 10000,
    'image size reasonable': () => imageSize > 1000 && imageSize < 10000000, // 1KB ~ 10MB
  });

  // 메트릭 기록
  imageLoadRate.add(success);
  imageLoadTime.add(loadTime);
  imageSizeMetric.add(imageSize);
  cacheHitRate.add(isCacheHit);
  totalRequests.add(1);

  // 상세 로그
  if (response.status !== 200) {
    console.error(`❌ Failed: Status ${response.status}, URL: ${IMAGE_URL}`);
  } else {
    const sizeKB = Math.round(imageSize / 1024);
    console.log(`✅ Success: ${loadTime}ms, ${sizeKB}KB, Cache: ${cacheStatus}`);
  }

  // 응답 헤더 정보 (첫 번째 요청에서만 출력)
  if (__ITER === 0) {
    console.log('\n📊 Response Headers Analysis:');
    console.log(`Content-Type: ${response.headers['Content-Type'] || 'N/A'}`);
    console.log(`Content-Length: ${response.headers['Content-Length'] || 'N/A'}`);
    console.log(`Cache-Control: ${response.headers['Cache-Control'] || 'N/A'}`);
    console.log(`Server: ${response.headers['Server'] || 'N/A'}`);
    console.log(`X-Cache: ${response.headers['X-Cache'] || 'N/A'}`);
    console.log(`CF-Cache-Status: ${response.headers['CF-Cache-Status'] || 'N/A'}`);
    console.log(`X-Amz-Cf-Id: ${response.headers['X-Amz-Cf-Id'] || 'N/A'}`);
    console.log('');
  }
}

export function handleSummary(data) {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');

  // 상세 성능 분석
  const analysis = {
    test_info: {
      url: IMAGE_URL,
      timestamp: timestamp,
      total_duration_seconds: Math.round(data.state.testRunDurationMs / 1000),
      total_requests: data.metrics.total_requests?.values.count || 0,
    },
    performance_metrics: {
      success_rate: Math.round((data.metrics.image_load_success?.values.rate || 0) * 100 * 100) / 100,
      cache_hit_rate: Math.round((data.metrics.cache_hit_rate?.values.rate || 0) * 100 * 100) / 100,
      load_time_ms: {
        avg: Math.round(data.metrics.image_load_duration?.values.avg || 0),
        min: Math.round(data.metrics.image_load_duration?.values.min || 0),
        max: Math.round(data.metrics.image_load_duration?.values.max || 0),
        p50: Math.round(data.metrics.image_load_duration?.values.med || 0),
        p95: Math.round(data.metrics.image_load_duration?.values['p(95)'] || 0),
        p99: Math.round(data.metrics.image_load_duration?.values['p(99)'] || 0),
      },
      image_size_kb: {
        avg: Math.round((data.metrics.image_size_bytes?.values.avg || 0) / 1024),
        min: Math.round((data.metrics.image_size_bytes?.values.min || 0) / 1024),
        max: Math.round((data.metrics.image_size_bytes?.values.max || 0) / 1024),
      },
      throughput: {
        requests_per_second: Math.round(data.metrics.http_reqs?.values.rate || 0),
        data_received_mb_per_second: Math.round((data.metrics.data_received?.values.rate || 0) / 1024 / 1024 * 100) / 100,
      }
    }
  };

  console.log('\n🎯 === PERFORMANCE TEST SUMMARY ===');
  console.log(`URL: ${IMAGE_URL}`);
  console.log(`Total Requests: ${analysis.test_info.total_requests}`);
  console.log(`Success Rate: ${analysis.performance_metrics.success_rate}%`);
  console.log(`Cache Hit Rate: ${analysis.performance_metrics.cache_hit_rate}%`);
  console.log(`\n⏱️  Load Time:`);
  console.log(`  Average: ${analysis.performance_metrics.load_time_ms.avg}ms`);
  console.log(`  P95: ${analysis.performance_metrics.load_time_ms.p95}ms`);
  console.log(`  P99: ${analysis.performance_metrics.load_time_ms.p99}ms`);
  console.log(`\n📦 Image Size: ${analysis.performance_metrics.image_size_kb.avg}KB (avg)`);
  console.log(`\n🚀 Throughput: ${analysis.performance_metrics.throughput.requests_per_second} RPS`);
  console.log(`📥 Data Rate: ${analysis.performance_metrics.throughput.data_received_mb_per_second} MB/s`);

  return {
    [`/reports/image-test-${timestamp}.json`]: JSON.stringify(data, null, 2),
    [`/reports/analysis-${timestamp}.json`]: JSON.stringify(analysis, null, 2),
  };
}