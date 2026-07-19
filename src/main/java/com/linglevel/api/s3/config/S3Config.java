package com.linglevel.api.s3.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@Profile("!performance-test")
public class S3Config {

	// AWS S3 Configuration (for AI buckets)
	@Value("${aws.s3.region}")
	private String region;

	@Value("${aws.access-key}")
	private String accessKey;

	@Value("${aws.secret-key}")
	private String secretKey;

	@Value("${aws.s3.ai.input.bucket}")
	private String aiInputBucketName;

	@Value("${aws.s3.ai.output.bucket}")
	private String aiOutputBucketName;

	// Cloudflare R2 Configuration (for Static files)
	@Value("${cf.r2.endpoint}")
	private String r2Endpoint;

	@Value("${cf.r2.access-key}")
	private String r2AccessKey;

	@Value("${cf.r2.secret-key}")
	private String r2SecretKey;

	@Value("${cf.r2.static.bucket}")
	private String r2StaticBucketName;

	/**
	 * AWS S3 클라이언트 (AI Input/Output 버킷용)
	 */
	@Bean("s3AiClient")
	public S3Client s3AiClient() {
		AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

		return S3Client.builder()
			.region(Region.of(region))
			.credentialsProvider(StaticCredentialsProvider.create(credentials))
			.build();
	}

	/**
	 * Cloudflare R2 클라이언트 (Static 파일용) R2는 S3 호환 API를 제공하므로 endpoint만 변경하면 됩니다.
	 */
	@Bean("s3StaticClient")
	public S3Client s3StaticClient() {
		AwsBasicCredentials credentials = AwsBasicCredentials.create(r2AccessKey, r2SecretKey);

		// R2 전용 설정 객체 생성
		S3Configuration serviceConfiguration = S3Configuration.builder()
			.pathStyleAccessEnabled(true)
			.chunkedEncodingEnabled(false)
			.build();

		return S3Client.builder()
			.endpointOverride(URI.create(r2Endpoint))
			.region(Region.of("auto"))
			.credentialsProvider(StaticCredentialsProvider.create(credentials))
			.serviceConfiguration(serviceConfiguration)
			.build();
	}

	@Bean("aiInputBucketName")
	public String aiInputBucketName() {
		return aiInputBucketName;
	}

	@Bean("aiOutputBucketName")
	public String aiOutputBucketName() {
		return aiOutputBucketName;
	}

	@Bean("staticBucketName")
	public String staticBucketName() {
		return r2StaticBucketName;
	}

}
