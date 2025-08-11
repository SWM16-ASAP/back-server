package com.linglevel.api.s3.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;

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
}