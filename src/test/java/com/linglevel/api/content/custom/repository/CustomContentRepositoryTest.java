package com.linglevel.api.content.custom.repository;

import com.linglevel.api.common.AbstractDatabaseTest;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.ProgressStatus;
import com.linglevel.api.content.custom.dto.GetCustomContentsRequest;
import com.linglevel.api.content.custom.entity.CustomContent;
import com.linglevel.api.content.custom.entity.CustomContentProgress;
import com.linglevel.api.content.custom.entity.UserCustomContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Import(CustomContentRepositoryImpl.class)
class CustomContentRepositoryTest extends AbstractDatabaseTest {

    @Autowired
    private CustomContentRepository customContentRepository;

    @Autowired
    private UserCustomContentRepository userCustomContentRepository;

    @Autowired
    private CustomContentProgressRepository customContentProgressRepository;

    @Autowired
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    private String testUserId;
    private CustomContent content1;
    private CustomContent content2;
    private CustomContent content3;
    private UserCustomContent userCustomContent1;
    private UserCustomContent userCustomContent2;
    private UserCustomContent userCustomContent3;

    @BeforeEach
    void setUp() {
        // 기존 데이터 삭제
        customContentRepository.deleteAll();
        userCustomContentRepository.deleteAll();
        customContentProgressRepository.deleteAll();

        testUserId = "test-user-id";

        // CustomContent 데이터 생성
        content1 = CustomContent.builder()
                .userId("creator-1")
                .contentRequestId("request-1")
                .isDeleted(false)
                .title("The Little Prince")
                .author("Antoine de Saint-Exupéry")
                .coverImageUrl("https://example.com/cover1.jpg")
                .difficultyLevel(DifficultyLevel.A2)
                .targetDifficultyLevels(Arrays.asList(DifficultyLevel.A1, DifficultyLevel.A2))
                .readingTime(30)
                .averageRating(4.5)
                .reviewCount(100)
                .viewCount(1000)
                .tags(Arrays.asList("classic", "fiction"))
                .originUrl("https://example.com/prince")
                .originDomain("example.com")
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now())
                .build();

        content2 = CustomContent.builder()
                .userId("creator-2")
                .contentRequestId("request-2")
                .isDeleted(false)
                .title("Harry Potter")
                .author("J.K. Rowling")
                .coverImageUrl("https://example.com/cover2.jpg")
                .difficultyLevel(DifficultyLevel.B1)
                .targetDifficultyLevels(Arrays.asList(DifficultyLevel.A2, DifficultyLevel.B1))
                .readingTime(60)
                .averageRating(4.8)
                .reviewCount(200)
                .viewCount(2000)
                .tags(Arrays.asList("fantasy", "fiction"))
                .originUrl("https://example.com/harry")
                .originDomain("example.com")
                .createdAt(Instant.now().minusSeconds(7200))
                .updatedAt(Instant.now())
                .build();

        content3 = CustomContent.builder()
                .userId("creator-3")
                .contentRequestId("request-3")
                .isDeleted(false)
                .title("Alice in Wonderland")
                .author("Lewis Carroll")
                .coverImageUrl("https://example.com/cover3.jpg")
                .difficultyLevel(DifficultyLevel.A2)
                .targetDifficultyLevels(Arrays.asList(DifficultyLevel.A2, DifficultyLevel.B1))
                .readingTime(45)
                .averageRating(4.2)
                .reviewCount(150)
                .viewCount(1500)
                .tags(Arrays.asList("classic", "fantasy"))
                .originUrl("https://example.com/alice")
                .originDomain("example.com")
                .createdAt(Instant.now().minusSeconds(10800))
                .updatedAt(Instant.now())
                .build();

        // CustomContent 저장
        content1 = customContentRepository.save(content1);
        content2 = customContentRepository.save(content2);
        content3 = customContentRepository.save(content3);

        // UserCustomContent 매핑 생성
        userCustomContent1 = UserCustomContent.builder()
                .userId(testUserId)
                .customContentId(content1.getId())
                .contentRequestId("request-1")
                .unlockedAt(Instant.now().minusSeconds(3600))
                .build();

        userCustomContent2 = UserCustomContent.builder()
                .userId(testUserId)
                .customContentId(content2.getId())
                .contentRequestId("request-2")
                .unlockedAt(Instant.now().minusSeconds(7200))
                .build();

        userCustomContent3 = UserCustomContent.builder()
                .userId(testUserId)
                .customContentId(content3.getId())
                .contentRequestId("request-3")
                .unlockedAt(Instant.now().minusSeconds(10800))
                .build();

        // UserCustomContent 저장
        userCustomContentRepository.save(userCustomContent1);
        userCustomContentRepository.save(userCustomContent2);
        userCustomContentRepository.save(userCustomContent3);
    }

    @Test
    @DisplayName("UserCustomContent 매핑을 통한 aggregation 쿼리가 정상적으로 동작한다")
    void findCustomContentsByUserWithFilters_shouldReturnContents() {
        // Given
        GetCustomContentsRequest request = new GetCustomContentsRequest();
        request.setPage(1);
        request.setLimit(10);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // When
        Page<CustomContent> result = customContentRepository.findCustomContentsByUserWithFilters(testUserId, request, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);

        // createdAt 기준 내림차순 정렬 확인
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("The Little Prince");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("Harry Potter");
        assertThat(result.getContent().get(2).getTitle()).isEqualTo("Alice in Wonderland");
    }

    @Test
    @DisplayName("키워드 필터링이 정상적으로 동작한다")
    void findCustomContentsByUserWithFilters_shouldFilterByKeyword() {
        // Given
        GetCustomContentsRequest request = new GetCustomContentsRequest();
        request.setPage(1);
        request.setLimit(10);
        request.setKeyword("prince");

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // When
        Page<CustomContent> result = customContentRepository.findCustomContentsByUserWithFilters(testUserId, request, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains("Prince");
    }

    @Test
    @DisplayName("태그 필터링이 정상적으로 동작한다")
    void findCustomContentsByUserWithFilters_shouldFilterByTags() {
        // Given
        GetCustomContentsRequest request = new GetCustomContentsRequest();
        request.setPage(1);
        request.setLimit(10);
        request.setTags("classic,fiction");

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // When
        Page<CustomContent> result = customContentRepository.findCustomContentsByUserWithFilters(testUserId, request, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("The Little Prince");
        assertThat(result.getContent().get(0).getTags()).containsExactlyInAnyOrder("classic", "fiction");
    }

    @Test
    @DisplayName("viewCount 정렬이 정상적으로 동작한다")
    void findCustomContentsByUserWithFilters_shouldSortByViewCount() {
        // Given
        GetCustomContentsRequest request = new GetCustomContentsRequest();
        request.setPage(1);
        request.setLimit(10);
        request.setSortBy("view_count");

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "viewCount"));

        // When
        Page<CustomContent> result = customContentRepository.findCustomContentsByUserWithFilters(testUserId, request, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getViewCount()).isEqualTo(2000);
        assertThat(result.getContent().get(1).getViewCount()).isEqualTo(1500);
        assertThat(result.getContent().get(2).getViewCount()).isEqualTo(1000);
    }

    @Test
    @DisplayName("averageRating 정렬이 정상적으로 동작한다")
    void findCustomContentsByUserWithFilters_shouldSortByAverageRating() {
        // Given
        GetCustomContentsRequest request = new GetCustomContentsRequest();
        request.setPage(1);
        request.setLimit(10);
        request.setSortBy("average_rating");

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "averageRating"));

        // When
        Page<CustomContent> result = customContentRepository.findCustomContentsByUserWithFilters(testUserId, request, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getAverageRating()).isEqualTo(4.8);
        assertThat(result.getContent().get(1).getAverageRating()).isEqualTo(4.5);
        assertThat(result.getContent().get(2).getAverageRating()).isEqualTo(4.2);
    }

    @Test
    @DisplayName("페이지네이션이 정상적으로 동작한다")
    void findCustomContentsByUserWithFilters_shouldHandlePagination() {
        // Given
        GetCustomContentsRequest request = new GetCustomContentsRequest();
        request.setPage(2);
        request.setLimit(2);

        Pageable pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdAt"));

        // When
        Page<CustomContent> result = customContentRepository.findCustomContentsByUserWithFilters(testUserId, request, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1); // 3개 중 2페이지(2개씩)니까 1개
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Alice in Wonderland");
    }

    @Test
    @DisplayName("진행 중인 콘텐츠만 필터링한다")
    void findCustomContentsByUserWithFilters_shouldFilterByProgressStatus_InProgress() {
        // Given - content2에 진행 중인 progress 추가
        CustomContentProgress progress = new CustomContentProgress();
        progress.setUserId(testUserId);
        progress.setCustomId(content2.getId());
        progress.setChunkId("chunk-1");
        progress.setNormalizedProgress(10.0);
        progress.setCurrentDifficultyLevel(DifficultyLevel.B1);
        progress.setIsCompleted(false);
        customContentProgressRepository.save(progress);

        GetCustomContentsRequest request = new GetCustomContentsRequest();
        request.setPage(1);
        request.setLimit(10);
        request.setProgress(ProgressStatus.IN_PROGRESS);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // When
        Page<CustomContent> result = customContentRepository.findCustomContentsByUserWithFilters(testUserId, request, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Harry Potter");
    }

    @Test
    @DisplayName("완료된 콘텐츠만 필터링한다")
    void findCustomContentsByUserWithFilters_shouldFilterByProgressStatus_Completed() {
        // Given - content1을 완료 상태로 설정
        CustomContentProgress progress = new CustomContentProgress();
        progress.setUserId(testUserId);
        progress.setCustomId(content1.getId());
        progress.setChunkId("chunk-50");
        progress.setNormalizedProgress(100.0);
        progress.setCurrentDifficultyLevel(DifficultyLevel.A2);
        progress.setIsCompleted(true);
        progress.setCompletedAt(Instant.now());
        customContentProgressRepository.save(progress);

        GetCustomContentsRequest request = new GetCustomContentsRequest();
        request.setPage(1);
        request.setLimit(10);
        request.setProgress(ProgressStatus.COMPLETED);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // When
        Page<CustomContent> result = customContentRepository.findCustomContentsByUserWithFilters(testUserId, request, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("The Little Prince");
    }

    @Test
    @DisplayName("시작하지 않은 콘텐츠만 필터링한다")
    void findCustomContentsByUserWithFilters_shouldFilterByProgressStatus_NotStarted() {
        // Given - content1에만 progress 추가
        CustomContentProgress progress = new CustomContentProgress();
        progress.setUserId(testUserId);
        progress.setCustomId(content1.getId());
        progress.setChunkId("chunk-1");
        progress.setNormalizedProgress(2.0);
        progress.setCurrentDifficultyLevel(DifficultyLevel.A2);
        progress.setIsCompleted(false);
        customContentProgressRepository.save(progress);

        GetCustomContentsRequest request = new GetCustomContentsRequest();
        request.setPage(1);
        request.setLimit(10);
        request.setProgress(ProgressStatus.NOT_STARTED);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // When
        Page<CustomContent> result = customContentRepository.findCustomContentsByUserWithFilters(testUserId, request, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2); // content2, content3
        assertThat(result.getContent()).extracting("title")
                .containsExactlyInAnyOrder("Harry Potter", "Alice in Wonderland");
    }

    @Test
    @DisplayName("isDeleted가 true인 콘텐츠는 제외된다")
    void findCustomContentsByUserWithFilters_shouldExcludeDeletedContents() {
        // Given - content1을 삭제 상태로 변경
        content1.setIsDeleted(true);
        customContentRepository.save(content1);

        GetCustomContentsRequest request = new GetCustomContentsRequest();
        request.setPage(1);
        request.setLimit(10);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // When
        Page<CustomContent> result = customContentRepository.findCustomContentsByUserWithFilters(testUserId, request, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting("title")
                .containsExactlyInAnyOrder("Harry Potter", "Alice in Wonderland");
    }

    @Test
    @DisplayName("다른 유저의 콘텐츠는 조회되지 않는다")
    void findCustomContentsByUserWithFilters_shouldNotReturnOtherUsersContents() {
        // Given
        String otherUserId = "other-user-id";
        GetCustomContentsRequest request = new GetCustomContentsRequest();
        request.setPage(1);
        request.setLimit(10);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // When
        Page<CustomContent> result = customContentRepository.findCustomContentsByUserWithFilters(otherUserId, request, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }
}
