package com.linglevel.api.s3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageResizeService {

    @Qualifier("s3StaticClient")
    private final S3Client s3StaticClient;

    @Qualifier("staticBucketName")
    private final String staticBucketName;

    private final S3StaticService s3StaticService;

    private static final int THUMBNAIL_SIZE = 256;

    public String createSmallImage(String originalS3Key) {
        try {
            log.info("Creating small image from: {}", originalS3Key);

            String thumbnailS3Key = generateSmallImagePath(originalS3Key);
            InputStream originalImageStream = downloadImageFromS3(originalS3Key);
            byte[] thumbnailBytes = resizeToThumbnail(originalImageStream);
            uploadThumbnailToS3(thumbnailS3Key, thumbnailBytes);

            String thumbnailUrl = s3StaticService.getPublicUrl(thumbnailS3Key);
            log.info("Small image created successfully: {}", thumbnailUrl);

            return thumbnailUrl;

        } catch (Exception e) {
            log.error("Failed to create small image for {}", originalS3Key, e);
            throw new RuntimeException("Small image creation failed", e);
        }
    }


    private String generateSmallImagePath(String originalS3Key) {
        int lastSlashIndex = originalS3Key.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            return "small_" + removeExtension(originalS3Key) + ".webp";
        }

        String directoryPath = originalS3Key.substring(0, lastSlashIndex + 1);
        String fileName = originalS3Key.substring(lastSlashIndex + 1);
        String fileNameWithoutExt = removeExtension(fileName);

        return directoryPath + "small_" + fileNameWithoutExt + ".webp";
    }

    private String removeExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return fileName;
        }
        return fileName.substring(0, lastDotIndex);
    }

    private InputStream downloadImageFromS3(String s3Key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(staticBucketName)
                    .key(s3Key)
                    .build();

            return s3StaticClient.getObject(getObjectRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download image from S3: " + s3Key, e);
        }
    }

    private byte[] resizeToThumbnail(InputStream imageStream) throws IOException {
        BufferedImage originalImage = ImageIO.read(imageStream);
        if (originalImage == null) {
            throw new IOException("Unable to read image from stream");
        }

        BufferedImage resizedImage = resizeWithCrop(originalImage, THUMBNAIL_SIZE, THUMBNAIL_SIZE);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "webp", outputStream);

        return outputStream.toByteArray();
    }

    private BufferedImage resizeWithCrop(BufferedImage originalImage, int targetWidth, int targetHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        double scaleX = (double) targetWidth / originalWidth;
        double scaleY = (double) targetHeight / originalHeight;
        double scale = Math.max(scaleX, scaleY);

        int scaledWidth = (int) (originalWidth * scale);
        int scaledHeight = (int) (originalHeight * scale);

        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        int x = (scaledWidth - targetWidth) / 2;
        int y = (scaledHeight - targetHeight) / 2;

        return scaledImage.getSubimage(x, y, targetWidth, targetHeight);
    }

    private void uploadThumbnailToS3(String s3Key, byte[] imageBytes) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(staticBucketName)
                    .key(s3Key)
                    .contentType("image/webp")
                    .build();

            s3StaticClient.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload thumbnail to S3: " + s3Key, e);
        }
    }
}