package com.linglevel.api.s3.service;

import com.linglevel.api.s3.strategy.S3PathStrategy;
import com.linglevel.api.s3.utils.S3FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3TransferService {

    private final S3AiService s3AiService;
    private final S3StaticService s3StaticService;

    public void transferImagesFromAiToStatic(String sourceId, String targetId, S3PathStrategy pathStrategy) {
        try {
            log.info("Starting image transfer from AI bucket to Static bucket for sourceId: {} to targetId: {}", sourceId, targetId);
            
            List<String> imageKeys = s3AiService.listImagesInFolder(sourceId, pathStrategy);
            
            for (String imageKey : imageKeys) {
                byte[] imageBytes = s3AiService.downloadImageFile(imageKey);
                String contentType = S3FileUtils.getContentTypeFromKey(imageKey);

                String newKey = imageKey.replace(sourceId, targetId);
                s3StaticService.uploadFileFromBytes(imageBytes, newKey, contentType);
            }
            
            log.info("Successfully transferred {} images to Static bucket", imageKeys.size());
            
        } catch (Exception e) {
            log.error("Failed to transfer images from AI to Static bucket: {}", e.getMessage());
            throw new RuntimeException("Image transfer failed", e);
        }
    }
}