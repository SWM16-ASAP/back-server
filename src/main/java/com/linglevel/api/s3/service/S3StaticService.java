package com.linglevel.api.s3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StaticService {

    @Qualifier("s3StaticClient")
    private final S3Client s3StaticClient;
    
    @Qualifier("staticBucketName")
    private final String staticBucketName;
    
    @Value("${aws.s3.static.url:}")
    private String staticUrl;

    public String uploadFile(MultipartFile file, String path) {
        try {
            String key = path + "/" + file.getOriginalFilename();
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(staticBucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
                    
            s3StaticClient.putObject(putObjectRequest, 
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            log.info("Successfully uploaded file to S3 Static: {}", key);
            return getPublicUrl(key);
            
        } catch (IOException e) {
            log.error("Failed to upload file to S3 Static: {}", e.getMessage());
            throw new RuntimeException("File upload failed", e);
        } catch (Exception e) {
            log.error("S3 Static operation failed: {}", e.getMessage());
            throw new RuntimeException("S3 operation failed", e);
        }
    }

    public String uploadFileFromBytes(byte[] fileBytes, String key, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(staticBucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();
                    
            s3StaticClient.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));
            
            log.info("Successfully uploaded file to S3 Static: {}", key);
            return getPublicUrl(key);
            
        } catch (Exception e) {
            log.error("Failed to upload file to S3 Static: {}", e.getMessage());
            throw new RuntimeException("File upload failed", e);
        }
    }

    public String getPublicUrl(String key) {
        return staticUrl + "/" + key;
    }
}