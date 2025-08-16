package com.linglevel.api.s3.strategy;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NewsPathStrategy implements S3PathStrategy {

    private static final String BASE_DIR = "article";
    private static final String IMAGES_DIR = "/images/";
    private static final String COVER_FILENAME = "cover.jpg";
    private static final String JSON_EXTENSION = ".json";

    private String getBasePathWithId(String newsId) {
        return BASE_DIR + "/" + newsId;
    }

    @Override
    public String generateJsonFilePath(String newsId) {
        return getBasePathWithId(newsId) + "/" + newsId + JSON_EXTENSION;
    }

    @Override
    public String generateImageFolderPath(String newsId) {
        return getBasePathWithId(newsId) + IMAGES_DIR;
    }

    @Override
    public String generateCoverImagePath(String newsId) {
        return getBasePathWithId(newsId) + IMAGES_DIR + COVER_FILENAME;
    }

    @Override
    public String generateImagePath(String newsId, String imageFileName) {
        return getBasePathWithId(newsId) + IMAGES_DIR + imageFileName;
    }

    @Override
    public String generateBasePath(String newsId) {
        return getBasePathWithId(newsId);
    }

    @Override
    public List<String> processImageKeys(List<String> rawKeys) {
        return rawKeys.stream()
                .filter(key -> !key.endsWith("/"))
                .toList();
    }
}