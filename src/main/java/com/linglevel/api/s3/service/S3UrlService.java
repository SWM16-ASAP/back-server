package com.linglevel.api.s3.service;

import com.linglevel.api.s3.strategy.S3PathStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class S3UrlService {

    private final S3StaticService s3StaticService;

    public String getCoverImageUrl(String id, S3PathStrategy pathStrategy) {
        String path = pathStrategy.generateCoverImagePath(id);
        return s3StaticService.getPublicUrl(path);
    }

    public String getImageUrl(String id, String imageFileName, S3PathStrategy pathStrategy) {
        String path = pathStrategy.generateImagePath(id, imageFileName);
        return s3StaticService.getPublicUrl(path);
    }

    public String buildImageUrl(String id, String imageFileName, S3PathStrategy pathStrategy) {
        return getImageUrl(id, imageFileName, pathStrategy);
    }
}