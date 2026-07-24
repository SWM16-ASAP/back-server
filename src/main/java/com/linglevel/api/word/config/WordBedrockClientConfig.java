package com.linglevel.api.word.config;

import com.linglevel.api.word.service.WordBedrockSdkMetricPublisher;
import org.springframework.ai.autoconfigure.bedrock.BedrockAwsConnectionProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientBuilder;

import java.net.URI;

@Configuration
public class WordBedrockClientConfig {

	private static final int MAX_ATTEMPTS = 2;

	@Bean
	public BedrockRuntimeClient wordBedrockRuntimeClient(AwsCredentialsProvider credentialsProvider,
			AwsRegionProvider regionProvider, BedrockAwsConnectionProperties connectionProperties,
			WordBedrockSdkMetricPublisher wordBedrockSdkMetricPublisher,
			@Value("${word.bedrock.endpoint:}") String endpoint) {
		BedrockRuntimeClientBuilder builder = BedrockRuntimeClient.builder()
			.region(regionProvider.getRegion())
			.credentialsProvider(credentialsProvider)
			.overrideConfiguration(configuration -> configuration.apiCallTimeout(connectionProperties.getTimeout())
				.addMetricPublisher(wordBedrockSdkMetricPublisher)
				.retryStrategy(AwsRetryStrategy.standardRetryStrategy().toBuilder().maxAttempts(MAX_ATTEMPTS).build()));

		if (StringUtils.hasText(endpoint)) {
			builder.endpointOverride(URI.create(endpoint));
		}

		return builder.build();
	}

}
