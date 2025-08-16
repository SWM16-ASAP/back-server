package com.linglevel.api.s3.strategy;

import java.util.List;

public interface S3PathStrategy {
    String generateJsonFilePath(String id);
    String generateImageFolderPath(String id);
    String generateCoverImagePath(String id);
    String generateImagePath(String id, String imageFileName);
    String generateBasePath(String id);
    List<String> processImageKeys(List<String> rawKeys);
}