package com.linglevel.api.word.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WordGenerationMetrics {

	private static final String SINGLE_FLIGHT_REQUESTS = "word.single.flight.requests";

	private static final String SINGLE_FLIGHT_FOLLOWER_WAIT = "word.single.flight.follower.wait";

	private static final String SINGLE_FLIGHT_LOCK_FAILURES = "word.single.flight.lock.failures";

	private static final String WORD_LOOKUP_RESULTS = "word.lookup.results";

	private static final String BEDROCK_CALLS = "word.bedrock.calls";

	private static final String BEDROCK_DURATION = "word.bedrock.duration";

	private static final String BEDROCK_SDK_ATTEMPTS = "word.bedrock.sdk.attempts";

	private static final String BEDROCK_SDK_RETRIES = "word.bedrock.sdk.retries";

	private final MeterRegistry meterRegistry;

	private final AtomicInteger bedrockInFlight = new AtomicInteger();

	public WordGenerationMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
		Gauge.builder("word.bedrock.in.flight", bedrockInFlight, AtomicInteger::get)
			.description("Current Word Bedrock calls in progress")
			.register(meterRegistry);
	}

	public void recordSingleFlightRequest(String role, String outcome) {
		meterRegistry.counter(SINGLE_FLIGHT_REQUESTS, "role", role, "outcome", outcome).increment();
	}

	public Timer.Sample startFollowerWait() {
		return Timer.start(meterRegistry);
	}

	public void recordFollowerWait(Timer.Sample sample, String outcome) {
		sample.stop(meterRegistry.timer(SINGLE_FLIGHT_FOLLOWER_WAIT, "outcome", outcome));
	}

	public void recordLockFailure(String operation) {
		meterRegistry.counter(SINGLE_FLIGHT_LOCK_FAILURES, "operation", operation).increment();
	}

	public void recordLookupResult(boolean hit) {
		meterRegistry.counter(WORD_LOOKUP_RESULTS, "outcome", hit ? "hit" : "miss").increment();
	}

	public Timer.Sample startBedrockCall() {
		bedrockInFlight.incrementAndGet();
		return Timer.start(meterRegistry);
	}

	public void recordBedrockCall(Timer.Sample sample, String outcome) {
		bedrockInFlight.decrementAndGet();
		meterRegistry.counter(BEDROCK_CALLS, "outcome", outcome).increment();
		sample.stop(meterRegistry.timer(BEDROCK_DURATION, "outcome", outcome));
	}

	public void recordBedrockRejected() {
		meterRegistry.counter(BEDROCK_CALLS, "outcome", "rejected").increment();
	}

	public void recordBedrockSdkExecution(int retryCount) {
		meterRegistry.counter(BEDROCK_SDK_ATTEMPTS).increment(retryCount + 1L);
		meterRegistry.counter(BEDROCK_SDK_RETRIES).increment(retryCount);
	}

}
