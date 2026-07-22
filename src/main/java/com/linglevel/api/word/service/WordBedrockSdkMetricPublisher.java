package com.linglevel.api.word.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WordBedrockSdkMetricPublisher implements MetricPublisher {

	private final WordGenerationMetrics metrics;

	@Override
	public void publish(MetricCollection metricCollection) {
		List<Integer> retryCounts = metricCollection.metricValues(CoreMetric.RETRY_COUNT);
		if (!retryCounts.isEmpty()) {
			metrics.recordBedrockSdkExecution(retryCounts.get(0));
		}
	}

	@Override
	public void close() {
	}

}
