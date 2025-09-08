package com.linglevel.api.s3.strategy;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class CustomContentPathStrategy implements S3PathStrategy {

    private static final String BASE_DIR = "custom";
    private static final String IMAGES_DIR = "/images/";
    private static final String COVER_FILENAME = "cover.jpg";
    private static final String METADATA_FILENAME = "metadata.json";

    @Override
    public String generateJsonFilePath(String id) {
        return generateBasePath(id) + "/" + METADATA_FILENAME;
    }

    @Override
    public String generateImageFolderPath(String id) {
        return generateBasePath(id) + IMAGES_DIR;
    }

    @Override
    public String generateCoverImagePath(String id) {
        return generateBasePath(id) + IMAGES_DIR + COVER_FILENAME;
    }

    @Override
    public String generateImagePath(String id, String imageFileName) {
        return generateBasePath(id) + IMAGES_DIR + imageFileName;
    }

    @Override
    public String generateBasePath(String id) {
        return BASE_DIR + "/" + id;
    }

    @Override
    public List<String> processImageKeys(List<String> rawKeys) {
        return rawKeys.stream()
                .filter(key -> !key.endsWith("/"))
                .toList();
    }
}