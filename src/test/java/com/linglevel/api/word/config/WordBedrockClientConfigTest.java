package com.linglevel.api.word.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.autoconfigure.bedrock.BedrockAwsConnectionProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class WordBedrockClientConfigTest {

	@Test
	@DisplayName("Bedrock 호출은 한 번 재시도하고 전체 요청 시간을 8초로 제한한다")
	void wordBedrockRuntimeClient_configuresSingleRetryAndTotalTimeout() {
		BedrockAwsConnectionProperties connectionProperties = new BedrockAwsConnectionProperties();
		connectionProperties.setTimeout(Duration.ofSeconds(8));

		WordBedrockClientConfig config = new WordBedrockClientConfig();
		try (BedrockRuntimeClient client = config.wordBedrockRuntimeClient(
				StaticCredentialsProvider.create(AwsBasicCredentials.create("access-key", "secret-key")),
				() -> Region.US_EAST_1, connectionProperties)) {
			ClientOverrideConfiguration overrideConfiguration = client.serviceClientConfiguration()
				.overrideConfiguration();

			assertThat(overrideConfiguration.apiCallTimeout()).contains(Duration.ofSeconds(8));
			assertThat(overrideConfiguration.retryStrategy())
				.hasValueSatisfying(retryStrategy -> assertThat(retryStrategy.maxAttempts()).isEqualTo(2));
		}
	}

}
