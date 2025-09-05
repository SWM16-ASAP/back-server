package com.linglevel.api.s3.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.access-key}")
    private String accessKey;

    @Value("${aws.secret-key}")
    private String secretKey;

    @Value("${aws.s3.static.bucket}")
    private String staticBucketName;

    @Value("${aws.s3.ai.input.bucket}")
    private String aiInputBucketName;

    @Value("${aws.s3.ai.output.bucket}")
    private String aiOutputBucketName;

    @Bean("s3AiClient")
    public S3Client s3AiClient() {
        return createS3Client();
    }

    @Bean("s3StaticClient")
    public S3Client s3StaticClient() {
        return createS3Client();
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
        return staticBucketName;
    }

    private S3Client createS3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
} 