package com.linglevel.api.s3.strategy;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ArticlePathStrategy implements S3PathStrategy {

    private static final String BASE_DIR = "article";
    private static final String IMAGES_DIR = "/images/";
    private static final String COVER_FILENAME = "cover.jpg";
    private static final String JSON_EXTENSION = ".json";

    private String getBasePathWithId(String articleId) {
        return BASE_DIR + "/" + articleId;
    }

    @Override
    public String generateJsonFilePath(String articleId) {
        return getBasePathWithId(articleId) + "/" + articleId + JSON_EXTENSION;
    }

    @Override
    public String generateImageFolderPath(String articleId) {
        return getBasePathWithId(articleId) + IMAGES_DIR;
    }

    @Override
    public String generateCoverImagePath(String articleId) {
        return getBasePathWithId(articleId) + IMAGES_DIR + COVER_FILENAME;
    }

    @Override
    public String generateImagePath(String articleId, String imageFileName) {
        return getBasePathWithId(articleId) + IMAGES_DIR + imageFileName;
    }

    @Override
    public String generateBasePath(String articleId) {
        return getBasePathWithId(articleId);
    }

    @Override
    public List<String> processImageKeys(List<String> rawKeys) {
        return rawKeys.stream()
                .filter(key -> !key.endsWith("/"))
                .toList();
    }
}