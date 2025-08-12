package com.linglevel.api.s3.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3AiService {

    @Qualifier("s3AiClient")
    private final S3Client s3AiClient;
    
    @Qualifier("aiBucketName")
    private final String aiBucketName;
    
    private final ObjectMapper objectMapper;

    public <T> T downloadJsonFile(String fileId, Class<T> targetClass) {
        try {
            String key = fileId + ".json";
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(aiBucketName)
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

    public List<String> listImagesInFolder(String folderId) {
        try {
            String prefix = folderId + "/images/";
            ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                    .bucket(aiBucketName)
                    .prefix(prefix)
                    .build();
            
            ListObjectsV2Response response = s3AiClient.listObjectsV2(listObjectsRequest);
            
            return response.contents().stream()
                    .map(S3Object::key)
                    .filter(key -> !key.endsWith("/")) // 폴더 제외, 파일만
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list images from S3 AI folder {}: {}", folderId, e.getMessage());
            throw new RuntimeException("Failed to list images", e);
        }
    }

    public byte[] downloadImageFile(String imageKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(aiBucketName)
                    .key(imageKey)
                    .build();
            
            var response = s3AiClient.getObject(getObjectRequest);
            return response.readAllBytes();
        } catch (Exception e) {
            log.error("Failed to download image from S3 AI: {}", e.getMessage());
            throw new RuntimeException("Failed to download image", e);
        }
    }
}