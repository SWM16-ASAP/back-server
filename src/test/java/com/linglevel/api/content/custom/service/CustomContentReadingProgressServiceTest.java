package com.linglevel.api.content.custom.service;

import com.linglevel.api.content.custom.dto.CustomContentReadingProgressUpdateRequest;
import com.linglevel.api.content.custom.entity.CustomContentChunk;
import com.linglevel.api.content.custom.entity.CustomContentProgress;
import com.linglevel.api.content.custom.repository.CustomContentChunkRepository;
import com.linglevel.api.content.custom.repository.CustomContentProgressRepository;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.service.ProgressCalculationService;
import com.linglevel.api.content.common.service.ReadingCompletionService;
import com.linglevel.api.streak.service.StreakService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomContentReadingProgressServiceTest {

    @Mock
    private CustomContentService customContentService;

    @Mock
    private CustomContentProgressRepository customContentProgressRepository;

    @Mock
    private CustomContentChunkRepository customContentChunkRepository;

    @Mock
    private CustomContentChunkService customContentChunkService;

    @Mock
    private ProgressCalculationService progressCalculationService;

    @Mock
    private ReadingCompletionService readingCompletionService;

    @Mock
    private StreakService streakService;

    @InjectMocks
    private CustomContentReadingProgressService customContentReadingProgressService;

    @Captor
    private ArgumentCaptor<CustomContentProgress> customProgressCaptor;

    @Test
    @DisplayName("오래된 CustomContent 진행률 업데이트 시 V2 필드가 정상적으로 마이그레이션된다")
    void updateProgress_shouldLazyMigrate_forOldData() {
        // Given: 마이그레이션되지 않은(V2 필드가 null인) CustomContentProgress 설정
        String userId = "test-user";
        String customId = "test-custom";
        String chunkId = "test-chunk";

        // V2 필드가 null인 레거시 데이터
        CustomContentProgress legacyProgress = new CustomContentProgress();
        legacyProgress.setId("legacy-progress-id");
        legacyProgress.setUserId(userId);
        legacyProgress.setCustomId(customId);
        // legacyProgress.normalizedProgress is null
        // legacyProgress.currentDifficultyLevel is null

        CustomContentChunk currentChunk = new CustomContentChunk();
        currentChunk.setId(chunkId);
        currentChunk.setCustomContentId(customId);
        currentChunk.setChunkNum(5);
        currentChunk.setDifficultyLevel(DifficultyLevel.A2);

        CustomContentReadingProgressUpdateRequest request = new CustomContentReadingProgressUpdateRequest();
        request.setChunkId(chunkId);

        // Mocking
        when(customContentService.existsById(customId)).thenReturn(true);
        when(customContentProgressRepository.findByUserIdAndCustomId(userId, customId)).thenReturn(Optional.of(legacyProgress));
        when(customContentChunkService.findById(chunkId)).thenReturn(currentChunk);
        when(customContentChunkRepository.countByCustomContentIdAndDifficultyLevelAndIsDeletedFalse(customId, DifficultyLevel.A2)).thenReturn(50L);
        when(progressCalculationService.calculateNormalizedProgress(5, 50L)).thenReturn(10.0);

        // When: 진행률 업데이트 호출
        customContentReadingProgressService.updateProgress(customId, request, userId);

        // Then: V2 필드가 채워진 상태로 저장되는지 검증
        verify(customContentProgressRepository).save(customProgressCaptor.capture());
        CustomContentProgress savedProgress = customProgressCaptor.getValue();

        assertThat(savedProgress.getId()).isEqualTo("legacy-progress-id");
        assertThat(savedProgress.getNormalizedProgress()).isNotNull();
        assertThat(savedProgress.getNormalizedProgress()).isEqualTo(10.0);
        assertThat(savedProgress.getMaxNormalizedProgress()).isEqualTo(10.0);
        assertThat(savedProgress.getCurrentDifficultyLevel()).isNotNull();
        assertThat(savedProgress.getCurrentDifficultyLevel()).isEqualTo(DifficultyLevel.A2);
    }
}
