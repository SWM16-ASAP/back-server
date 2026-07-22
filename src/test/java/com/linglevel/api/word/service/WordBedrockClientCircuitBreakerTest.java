package com.linglevel.api.word.service;

import com.linglevel.api.word.exception.WordsErrorCode;
import com.linglevel.api.word.exception.WordsException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(classes = { WordBedrockClientCircuitBreakerTest.TestConfig.class, WordBedrockClient.class,
		WordGenerationMetrics.class })
@ImportAutoConfiguration({ AopAutoConfiguration.class, CircuitBreakerAutoConfiguration.class })
@TestPropertySource(properties = { "resilience4j.circuitbreaker.instances.wordBedrock.sliding-window-type=COUNT_BASED",
		"resilience4j.circuitbreaker.instances.wordBedrock.sliding-window-size=2",
		"resilience4j.circuitbreaker.instances.wordBedrock.minimum-number-of-calls=2",
		"resilience4j.circuitbreaker.instances.wordBedrock.failure-rate-threshold=50",
		"resilience4j.circuitbreaker.instances.wordBedrock.wait-duration-in-open-state=60s",
		"resilience4j.circuitbreaker.instances.wordBedrock.permitted-number-of-calls-in-half-open-state=1" })
class WordBedrockClientCircuitBreakerTest {

	@Autowired
	private WordBedrockClient client;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private AtomicInteger bedrockAttempts;

	@Autowired
	private MeterRegistry meterRegistry;

	@BeforeEach
	void resetCircuitBreaker() {
		applicationContext.getBean(CircuitBreakerRegistry.class).circuitBreaker("wordBedrock").reset();
		bedrockAttempts.set(0);
	}

	@Test
	@DisplayName("Bedrock 호출 실패가 반복되면 circuit을 열고 이후 호출은 Bedrock까지 보내지 않는다")
	void call_opensCircuitAndSkipsBedrockAfterRepeatedFailures() {
		Prompt prompt = new Prompt("Analyze word");
		double errorsBefore = meterRegistry.counter("word.bedrock.calls", "outcome", "error").count();
		double rejectionsBefore = meterRegistry.counter("word.bedrock.calls", "outcome", "rejected").count();

		assertTemporaryUnavailable(client, prompt);
		assertTemporaryUnavailable(client, prompt);

		CircuitBreakerRegistry circuitBreakerRegistry = applicationContext.getBean(CircuitBreakerRegistry.class);
		CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("wordBedrock");
		assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
		assertThat(bedrockAttempts).hasValue(2);
		assertThat(meterRegistry.counter("word.bedrock.calls", "outcome", "error").count() - errorsBefore).isEqualTo(2);
		assertThat(meterRegistry.get("word.bedrock.in.flight").gauge().value()).isZero();

		assertThatThrownBy(() -> client.call(prompt)).isInstanceOfSatisfying(WordsException.class, e -> {
			assertThat(e.getErrorCode()).isEqualTo(WordsErrorCode.WORD_AI_TEMPORARILY_UNAVAILABLE);
			assertThat(e.getCause()).isInstanceOf(CallNotPermittedException.class);
		});
		assertThat(bedrockAttempts).hasValue(2);
		assertThat(meterRegistry.counter("word.bedrock.calls", "outcome", "rejected").count() - rejectionsBefore)
			.isEqualTo(1);
	}

	@Test
	@DisplayName("JVM Error는 fallback으로 변환하지 않고 그대로 전파한다")
	void call_propagatesErrorWithoutFallback() {
		assertThatThrownBy(() -> client.call(new Prompt("Trigger error"))).isInstanceOf(AssertionError.class)
			.hasMessage("fatal error");
	}

	@Test
	@DisplayName("Bedrock timeout은 일반 오류와 구분해서 기록한다")
	void call_recordsTimeoutOutcome() {
		double timeoutsBefore = meterRegistry.counter("word.bedrock.calls", "outcome", "timeout").count();
		double errorsBefore = meterRegistry.counter("word.bedrock.calls", "outcome", "error").count();

		assertTemporaryUnavailable(client, new Prompt("Trigger timeout"));

		assertThat(meterRegistry.counter("word.bedrock.calls", "outcome", "timeout").count() - timeoutsBefore)
			.isEqualTo(1);
		assertThat(meterRegistry.counter("word.bedrock.calls", "outcome", "error").count() - errorsBefore).isZero();
	}

	private void assertTemporaryUnavailable(WordBedrockClient client, Prompt prompt) {
		assertThatThrownBy(() -> client.call(prompt)).isInstanceOfSatisfying(WordsException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(WordsErrorCode.WORD_AI_TEMPORARILY_UNAVAILABLE));
	}

	@Configuration
	static class TestConfig {

		@Bean
		AtomicInteger bedrockAttempts() {
			return new AtomicInteger();
		}

		@Bean
		MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

		@Bean
		ChatModel chatModel(AtomicInteger bedrockAttempts) {
			return prompt -> {
				bedrockAttempts.incrementAndGet();
				if ("Trigger timeout".equals(prompt.getContents())) {
					throw new IllegalStateException("Bedrock call timed out", ApiCallTimeoutException.create(8000));
				}
				if ("Trigger error".equals(prompt.getContents())) {
					throw new AssertionError("fatal error");
				}
				throw new IllegalStateException("bedrock unavailable");
			};
		}

	}

}
