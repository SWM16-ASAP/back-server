package com.linglevel.api.s3.service;

import com.linglevel.api.s3.strategy.S3PathStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;

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

    public void deleteFiles(String contentId, S3PathStrategy pathStrategy) {
        try {
            String prefix = pathStrategy.generateBasePath(contentId);
            deleteFilesWithPrefix(prefix);
            log.info("Successfully deleted files from S3 Static - contentId: {}, prefix: {}", contentId, prefix);
        } catch (Exception e) {
            log.error("Failed to delete files from S3 Static - contentId: {}, prefix: {}, error: {}", 
                    contentId, pathStrategy.generateBasePath(contentId), e.getMessage());
            throw new RuntimeException("Failed to delete files from S3", e);
        }
    }

    private void deleteFilesWithPrefix(String prefix) {
        try {
            // List all objects with the given prefix
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(staticBucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response listResponse;
            do {
                listResponse = s3StaticClient.listObjectsV2(listRequest);
                
                if (!listResponse.contents().isEmpty()) {
                    // Delete objects in batches
                    List<ObjectIdentifier> objectsToDelete = listResponse.contents().stream()
                            .map(s3Object -> ObjectIdentifier.builder()
                                    .key(s3Object.key())
                                    .build())
                            .toList();

                    DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                            .bucket(staticBucketName)
                            .delete(Delete.builder()
                                    .objects(objectsToDelete)
                                    .build())
                            .build();

                    DeleteObjectsResponse deleteResponse = s3StaticClient.deleteObjects(deleteRequest);
                    log.info("Deleted {} objects from S3 Static with prefix: {}", deleteResponse.deleted().size(), prefix);
                }

                // Update request for next iteration if there are more objects
                listRequest = listRequest.toBuilder()
                        .continuationToken(listResponse.nextContinuationToken())
                        .build();
                        
            } while (listResponse.isTruncated());
            
        } catch (Exception e) {
            log.error("Failed to delete files with prefix {} from S3 Static: {}", prefix, e.getMessage());
            throw e;
        }
    }
}