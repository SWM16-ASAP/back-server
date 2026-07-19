package com.linglevel.api.s3.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Profile("performance-test")
public class PerformanceTestS3Config {

	@Bean("s3AiClient")
	public S3Client s3AiClient(@Value("${aws.s3.region}") String region) {
		return S3Client.builder()
			.region(Region.of(region))
			.credentialsProvider(DefaultCredentialsProvider.builder().build())
			.build();
	}

	@Bean(value = "s3StaticClient", destroyMethod = "")
	public S3Client s3StaticClient(@Qualifier("s3AiClient") S3Client s3AiClient) {
		return s3AiClient;
	}

	@Bean("aiInputBucketName")
	public String aiInputBucketName(@Value("${aws.s3.ai.input.bucket}") String bucketName) {
		return bucketName;
	}

	@Bean("aiOutputBucketName")
	public String aiOutputBucketName(@Value("${aws.s3.ai.output.bucket}") String bucketName) {
		return bucketName;
	}

	@Bean("staticBucketName")
	public String staticBucketName(@Qualifier("aiOutputBucketName") String aiOutputBucketName) {
		return aiOutputBucketName;
	}

}
