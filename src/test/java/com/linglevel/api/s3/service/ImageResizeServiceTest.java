package com.linglevel.api.s3.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageResizeService 테스트")
class ImageResizeServiceTest {

    @Mock
    private S3Client s3StaticClient;

    @Mock
    private S3StaticService s3StaticService;

    @InjectMocks
    private ImageResizeService imageResizeService;

    private final String staticBucketName = "test-bucket";
    private final String originalS3Key = "books/test-book-id/cover.jpg";
    private final String expectedThumbnailKey = "books/test-book-id/small_cover.webp";
    private final String expectedThumbnailUrl = "https://cdn.example.com/books/test-book-id/small_cover.webp";

    @BeforeEach
    void setUp() throws Exception {
        // Reflection을 사용하여 private final 필드 설정
        var bucketNameField = ImageResizeService.class.getDeclaredField("staticBucketName");
        bucketNameField.setAccessible(true);
        bucketNameField.set(imageResizeService, staticBucketName);
    }

    @Nested
    @DisplayName("썸네일 생성 테스트")
    class CreateSmallImageTest {

        @Test
        @DisplayName("JPG 이미지를 WebP 썸네일로 변환 성공")
        void createSmallImage_Success() {
            // given - 테스트용 JPG 이미지 생성
            ResponseInputStream<GetObjectResponse> testImageStream = createMockResponseInputStream(createTestJpgImage(512, 512));
            when(s3StaticClient.getObject(any(GetObjectRequest.class))).thenReturn(testImageStream);
            when(s3StaticService.getPublicUrl(expectedThumbnailKey)).thenReturn(expectedThumbnailUrl);

            // when
            String result = imageResizeService.createSmallImage(originalS3Key);

            // then
            assertThat(result).isEqualTo(expectedThumbnailUrl);

            // S3 다운로드 요청 검증
            ArgumentCaptor<GetObjectRequest> getRequestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
            verify(s3StaticClient).getObject(getRequestCaptor.capture());
            GetObjectRequest getRequest = getRequestCaptor.getValue();
            assertThat(getRequest.bucket()).isEqualTo(staticBucketName);
            assertThat(getRequest.key()).isEqualTo(originalS3Key);

            // S3 업로드 요청 검증
            ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
            ArgumentCaptor<RequestBody> requestBodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
            verify(s3StaticClient).putObject(putRequestCaptor.capture(), requestBodyCaptor.capture());

            PutObjectRequest putRequest = putRequestCaptor.getValue();
            assertThat(putRequest.bucket()).isEqualTo(staticBucketName);
            assertThat(putRequest.key()).isEqualTo(expectedThumbnailKey);
            assertThat(putRequest.contentType()).isEqualTo("image/webp");

            // 공개 URL 생성 검증
            verify(s3StaticService).getPublicUrl(expectedThumbnailKey);
        }

        @Test
        @DisplayName("PNG 이미지를 WebP 썸네일로 변환 성공")
        void createSmallImage_FromPng_Success() {
            // given
            String originalPngS3Key = "articles/test-article-id/cover.png";
            String expectedPngThumbnailKey = "articles/test-article-id/small_cover.webp";
            ResponseInputStream<GetObjectResponse> testImageStream = createMockResponseInputStream(createTestPngImage(1024, 768));

            when(s3StaticClient.getObject(any(GetObjectRequest.class))).thenReturn(testImageStream);
            when(s3StaticService.getPublicUrl(expectedPngThumbnailKey)).thenReturn(expectedThumbnailUrl);

            // when
            String result = imageResizeService.createSmallImage(originalPngS3Key);

            // then
            assertThat(result).isEqualTo(expectedThumbnailUrl);

            ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3StaticClient).putObject(putRequestCaptor.capture(), any(RequestBody.class));

            PutObjectRequest putRequest = putRequestCaptor.getValue();
            assertThat(putRequest.key()).isEqualTo(expectedPngThumbnailKey);
            assertThat(putRequest.contentType()).isEqualTo("image/webp");
        }

        @Test
        @DisplayName("정사각형이 아닌 이미지도 256x256 정사각형 썸네일로 변환")
        void createSmallImage_RectangularImage_Success() {
            // given - 직사각형 이미지 (1200x800)
            ResponseInputStream<GetObjectResponse> testImageStream = createMockResponseInputStream(createTestJpgImage(1200, 800));
            when(s3StaticClient.getObject(any(GetObjectRequest.class))).thenReturn(testImageStream);
            when(s3StaticService.getPublicUrl(expectedThumbnailKey)).thenReturn(expectedThumbnailUrl);

            // when
            String result = imageResizeService.createSmallImage(originalS3Key);

            // then
            assertThat(result).isEqualTo(expectedThumbnailUrl);
            verify(s3StaticClient).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("작은 이미지도 256x256으로 확대")
        void createSmallImage_SmallImage_Success() {
            // given - 작은 이미지 (100x100)
            ResponseInputStream<GetObjectResponse> testImageStream = createMockResponseInputStream(createTestJpgImage(100, 100));
            when(s3StaticClient.getObject(any(GetObjectRequest.class))).thenReturn(testImageStream);
            when(s3StaticService.getPublicUrl(expectedThumbnailKey)).thenReturn(expectedThumbnailUrl);

            // when
            String result = imageResizeService.createSmallImage(originalS3Key);

            // then
            assertThat(result).isEqualTo(expectedThumbnailUrl);
            verify(s3StaticClient).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }
    }

    @Nested
    @DisplayName("S3 키 생성 테스트")
    class S3KeyGenerationTest {

        @Test
        @DisplayName("디렉토리가 있는 경우 올바른 썸네일 키 생성")
        void generateSmallImagePath_WithDirectory() {
            // given
            String originalKey = "books/book-123/cover.jpg";
            ResponseInputStream<GetObjectResponse> testImageStream = createMockResponseInputStream(createTestJpgImage(256, 256));
            when(s3StaticClient.getObject(any(GetObjectRequest.class))).thenReturn(testImageStream);
            when(s3StaticService.getPublicUrl("books/book-123/small_cover.webp")).thenReturn("test-url");

            // when
            imageResizeService.createSmallImage(originalKey);

            // then
            ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3StaticClient).putObject(putRequestCaptor.capture(), any(RequestBody.class));
            assertThat(putRequestCaptor.getValue().key()).isEqualTo("books/book-123/small_cover.webp");
        }

        @Test
        @DisplayName("루트 디렉토리의 경우 올바른 썸네일 키 생성")
        void generateSmallImagePath_RootDirectory() {
            // given
            String originalKey = "image.png";
            ResponseInputStream<GetObjectResponse> testImageStream = createMockResponseInputStream(createTestJpgImage(256, 256));
            when(s3StaticClient.getObject(any(GetObjectRequest.class))).thenReturn(testImageStream);
            when(s3StaticService.getPublicUrl("small_image.webp")).thenReturn("test-url");

            // when
            imageResizeService.createSmallImage(originalKey);

            // then
            ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3StaticClient).putObject(putRequestCaptor.capture(), any(RequestBody.class));
            assertThat(putRequestCaptor.getValue().key()).isEqualTo("small_image.webp");
        }

        @Test
        @DisplayName("확장자가 없는 파일의 경우 올바른 썸네일 키 생성")
        void generateSmallImagePath_NoExtension() {
            // given
            String originalKey = "content/test-id/coverimage";
            ResponseInputStream<GetObjectResponse> testImageStream = createMockResponseInputStream(createTestJpgImage(256, 256));
            when(s3StaticClient.getObject(any(GetObjectRequest.class))).thenReturn(testImageStream);
            when(s3StaticService.getPublicUrl("content/test-id/small_coverimage.webp")).thenReturn("test-url");

            // when
            imageResizeService.createSmallImage(originalKey);

            // then
            ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3StaticClient).putObject(putRequestCaptor.capture(), any(RequestBody.class));
            assertThat(putRequestCaptor.getValue().key()).isEqualTo("content/test-id/small_coverimage.webp");
        }
    }

    @Nested
    @DisplayName("에러 처리 테스트")
    class ErrorHandlingTest {

        @Test
        @DisplayName("S3 다운로드 실패시 RuntimeException 발생")
        void createSmallImage_S3DownloadFailure() {
            // given
            when(s3StaticClient.getObject(any(GetObjectRequest.class)))
                    .thenThrow(new RuntimeException("S3 download failed"));

            // when & then
            assertThatThrownBy(() -> imageResizeService.createSmallImage(originalS3Key))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Small image creation failed");

            verify(s3StaticClient, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            verify(s3StaticService, never()).getPublicUrl(anyString());
        }

        @Test
        @DisplayName("이미지 변환 실패시 RuntimeException 발생")
        void createSmallImage_ImageConversionFailure() throws IOException {
            // given - 잘못된 이미지 데이터
            ResponseInputStream<GetObjectResponse> invalidImageStream = createMockResponseInputStream(new ByteArrayInputStream("invalid image data".getBytes()));
            when(s3StaticClient.getObject(any(GetObjectRequest.class))).thenReturn(invalidImageStream);

            // when & then
            assertThatThrownBy(() -> imageResizeService.createSmallImage(originalS3Key))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Small image creation failed");

            verify(s3StaticClient, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            verify(s3StaticService, never()).getPublicUrl(anyString());
        }

        @Test
        @DisplayName("S3 업로드 실패시 RuntimeException 발생")
        void createSmallImage_S3UploadFailure() throws IOException {
            // given
            ResponseInputStream<GetObjectResponse> testImageStream = createMockResponseInputStream(createTestJpgImage(256, 256));
            when(s3StaticClient.getObject(any(GetObjectRequest.class))).thenReturn(testImageStream);
            when(s3StaticClient.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenThrow(new RuntimeException("S3 upload failed"));

            // when & then
            assertThatThrownBy(() -> imageResizeService.createSmallImage(originalS3Key))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Small image creation failed");

            verify(s3StaticService, never()).getPublicUrl(anyString());
        }
    }

    // 테스트용 JPG 이미지 생성 헬퍼 메서드
    private InputStream createTestJpgImage(int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();

            // 그라데이션 배경 생성
            GradientPaint gradient = new GradientPaint(0, 0, Color.RED, width, height, Color.BLUE);
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, width, height);

            // 테스트 텍스트 추가
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, Math.max(12, width / 20)));
            g2d.drawString("TEST " + width + "x" + height, width / 4, height / 2);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("테스트 이미지 생성 실패", e);
        }
    }

    // 테스트용 PNG 이미지 생성 헬퍼 메서드
    private InputStream createTestPngImage(int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();

            // 투명한 배경에 원형 그리기
            g2d.setColor(new Color(0, 255, 0, 128)); // 반투명 녹색
            g2d.fillOval(width / 4, height / 4, width / 2, height / 2);

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, Math.max(12, width / 25)));
            g2d.drawString("PNG " + width + "x" + height, width / 6, height / 2);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("테스트 PNG 이미지 생성 실패", e);
        }
    }

    // ResponseInputStream Mock 생성 헬퍼 메서드
    private ResponseInputStream<GetObjectResponse> createMockResponseInputStream(InputStream inputStream) {
        GetObjectResponse response = GetObjectResponse.builder().build();
        return new ResponseInputStream<>(response, inputStream);
    }
}