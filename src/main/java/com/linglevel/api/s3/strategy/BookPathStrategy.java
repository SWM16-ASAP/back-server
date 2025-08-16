package com.linglevel.api.s3.strategy;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookPathStrategy implements S3PathStrategy {

    private static final String BASE_DIR = "literature";
    private static final String IMAGES_DIR = "/images/";
    private static final String COVER_FILENAME = "cover.jpg";
    private static final String JSON_EXTENSION = ".json";

    private String getBasePathWithId(String bookId) {
        return BASE_DIR + "/" + bookId;
    }

    @Override
    public String generateJsonFilePath(String bookId) {
        return getBasePathWithId(bookId) + "/" + bookId + JSON_EXTENSION;
    }

    @Override
    public String generateImageFolderPath(String bookId) {
        return getBasePathWithId(bookId) + IMAGES_DIR;
    }

    @Override
    public String generateCoverImagePath(String bookId) {
        return getBasePathWithId(bookId) + IMAGES_DIR + COVER_FILENAME;
    }

    @Override
    public String generateImagePath(String bookId, String imageFileName) {
        return getBasePathWithId(bookId) + IMAGES_DIR + imageFileName;
    }

    @Override
    public String generateBasePath(String bookId) {
        return getBasePathWithId(bookId);
    }

    @Override
    public List<String> processImageKeys(List<String> rawKeys) {
        return rawKeys.stream()
                .filter(key -> !key.endsWith("/"))
                .toList();
    }
}