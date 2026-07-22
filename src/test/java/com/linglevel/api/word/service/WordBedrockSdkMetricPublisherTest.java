package com.linglevel.api.word.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCollector;

import static org.assertj.core.api.Assertions.assertThat;

class WordBedrockSdkMetricPublisherTest {

	private SimpleMeterRegistry meterRegistry;

	private WordBedrockSdkMetricPublisher publisher;

	@BeforeEach
	void setUp() {
		meterRegistry = new SimpleMeterRegistry();
		publisher = new WordBedrockSdkMetricPublisher(new WordGenerationMetrics(meterRegistry));
	}

	@Test
	@DisplayName("AWS SDK 재시도 횟수로 실제 Bedrock 시도 수를 기록한다")
	void publish_recordsAttemptsAndRetries() {
		MetricCollector collector = MetricCollector.create("ApiCall");
		collector.reportMetric(CoreMetric.RETRY_COUNT, 1);

		publisher.publish(collector.collect());

		assertThat(meterRegistry.counter("word.bedrock.sdk.attempts").count()).isEqualTo(2);
		assertThat(meterRegistry.counter("word.bedrock.sdk.retries").count()).isEqualTo(1);
	}

	@Test
	@DisplayName("RetryCount가 없는 SDK metric collection은 집계하지 않는다")
	void publish_ignoresCollectionsWithoutRetryCount() {
		publisher.publish(MetricCollector.create("HttpClient").collect());

		assertThat(meterRegistry.find("word.bedrock.sdk.attempts").counter()).isNull();
		assertThat(meterRegistry.find("word.bedrock.sdk.retries").counter()).isNull();
	}

}
