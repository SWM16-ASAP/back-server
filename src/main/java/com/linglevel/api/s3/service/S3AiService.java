package com.linglevel.api.s3.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linglevel.api.s3.strategy.S3PathStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3AiService {

    @Qualifier("s3AiClient")
    private final S3Client s3AiClient;
    
    @Qualifier("aiInputBucketName")
    private final String aiInputBucketName;
    
    @Qualifier("aiOutputBucketName")
    private final String aiOutputBucketName;
    
    private final ObjectMapper objectMapper;

    public <T> T downloadJsonFile(String fileId, Class<T> targetClass, S3PathStrategy pathStrategy) {
        try {
            String key = pathStrategy.generateJsonFilePath(fileId);
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(aiOutputBucketName)
                    .key(key)
                    .build();
            
            var response = s3AiClient.getObject(getObjectRequest);
            String jsonContent = new String(response.readAllBytes());
            
            return objectMapper.readValue(jsonContent, targetClass);
        } catch (IOException e) {
            log.error("Failed to read JSON from S3 AI: {}", e.getMessage());
            throw new RuntimeException("File download failed", e);
        } catch (Exception e) {
            log.error("S3 AI operation failed: {}", e.getMessage());
            throw new RuntimeException("S3 operation failed", e);
        }
    }

    public List<String> listImagesInFolder(String folderId, S3PathStrategy pathStrategy) {
        try {
            String prefix = pathStrategy.generateImageFolderPath(folderId);
            ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                    .bucket(aiOutputBucketName)
                    .prefix(prefix)
                    .build();
            
            ListObjectsV2Response response = s3AiClient.listObjectsV2(listObjectsRequest);
            
            List<String> rawKeys = response.contents().stream()
                    .map(S3Object::key)
                    .toList();
            
            return pathStrategy.processImageKeys(rawKeys);
        } catch (Exception e) {
            log.error("Failed to list images from S3 AI folder {}: {}", folderId, e.getMessage());
            throw new RuntimeException("Failed to list images", e);
        }
    }

    public byte[] downloadImageFile(String imageKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(aiOutputBucketName)
                    .key(imageKey)
                    .build();
            
            var response = s3AiClient.getObject(getObjectRequest);
            return response.readAllBytes();
        } catch (Exception e) {
            log.error("Failed to download image from S3 AI: {}", e.getMessage());
            throw new RuntimeException("Failed to download image", e);
        }
    }

    public void uploadJsonToInputBucket(String requestId, Object data, S3PathStrategy pathStrategy) {
        try {
            String key = pathStrategy.generateJsonFilePath(requestId);
            String jsonContent = objectMapper.writeValueAsString(data);
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(aiInputBucketName)
                    .key(key)
                    .contentType("application/json")
                    .build();
            
            s3AiClient.putObject(putObjectRequest, RequestBody.fromString(jsonContent));
            log.info("Successfully uploaded JSON to AI input bucket: {}", key);
        } catch (Exception e) {
            log.error("Failed to upload JSON to S3 AI input bucket: {}", e.getMessage());
            throw new RuntimeException("Failed to upload to input bucket", e);
        }
    }
}