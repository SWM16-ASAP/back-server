package com.linglevel.api.word.config;

import org.springframework.ai.autoconfigure.bedrock.BedrockAwsConnectionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration
public class WordBedrockClientConfig {

	private static final int MAX_ATTEMPTS = 2;

	@Bean
	public BedrockRuntimeClient wordBedrockRuntimeClient(AwsCredentialsProvider credentialsProvider,
			AwsRegionProvider regionProvider, BedrockAwsConnectionProperties connectionProperties) {
		return BedrockRuntimeClient.builder()
			.region(regionProvider.getRegion())
			.credentialsProvider(credentialsProvider)
			.overrideConfiguration(builder -> builder.apiCallTimeout(connectionProperties.getTimeout())
				.retryStrategy(AwsRetryStrategy.standardRetryStrategy().toBuilder().maxAttempts(MAX_ATTEMPTS).build()))
			.build();
	}

}
