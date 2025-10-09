package com.linglevel.api.admin.migration;

import com.linglevel.api.bookmark.entity.WordBookmark;
import com.linglevel.api.bookmark.repository.WordBookmarkRepository;
import com.linglevel.api.word.dto.WordResponse;
import com.linglevel.api.word.repository.WordRepository;
import com.linglevel.api.word.repository.WordVariantRepository;
import com.linglevel.api.word.service.WordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/migration")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Migration", description = "관리자 마이그레이션 API")
@SecurityRequirement(name = "adminApiKey")
public class BookmarkMigrationController {

    private final WordBookmarkRepository wordBookmarkRepository;
    private final WordService wordService;
    private final WordRepository wordRepository;
    private final WordVariantRepository wordVariantRepository;

    @PostMapping("/reset-and-normalize")
    @Operation(summary = "단어 데이터 리셋 및 북마크 정규화",
               description = "1) 단어 데이터를 먼저 삭제 2) 북마크를 순회하며 원형으로 정규화하고 단어 복원 3) 중복 발생 시 삭제")
    public MigrationResult resetAndNormalizeBookmarks() {
        log.warn("Starting word reset and bookmark normalization...");

        // 1. 기존 단어 데이터 먼저 삭제
        long deletedWords = wordRepository.count();
        long deletedVariants = wordVariantRepository.count();

        wordRepository.deleteAll();
        wordVariantRepository.deleteAll();

        log.warn("Deleted {} words and {} variants", deletedWords, deletedVariants);

        // 2. 북마크 순회하며 정규화 및 단어 복원 (각 북마크는 독립적으로 처리)
        List<WordBookmark> allBookmarks = wordBookmarkRepository.findAll();
        int total = allBookmarks.size();
        int normalized = 0;
        int duplicateRemoved = 0;
        int failed = 0;
        java.util.Set<String> restoredWords = new java.util.HashSet<>();

        for (WordBookmark bookmark : allBookmarks) {
            try {
                String currentWord = bookmark.getWord();
                String userId = bookmark.getUserId();

                // AI를 통해 단어 새로 생성 및 원형 획득
                log.info("Restoring and normalizing: {} (userId: {})", currentWord, userId);
                WordResponse wordResponse = wordService.getOrCreateWord(userId, currentWord);
                String originalForm = wordResponse.getOriginalForm();

                restoredWords.add(originalForm); // 복원된 원형 단어 추적

                // 이미 원형이면 스킵
                if (currentWord.equals(originalForm)) {
                    log.debug("Already in original form: {}", currentWord);
                    continue;
                }

                log.info("Normalizing bookmark: {} -> {} (userId: {})", currentWord, originalForm, userId);

                // 원형으로 업데이트 시도
                bookmark.setWord(originalForm);

                try {
                    wordBookmarkRepository.save(bookmark);
                    normalized++;
                    log.info("Successfully normalized: {} -> {}", currentWord, originalForm);
                } catch (DuplicateKeyException e) {
                    // 중복 발생 시 현재 북마크 삭제 (원형이 이미 북마크되어 있음)
                    wordBookmarkRepository.delete(bookmark);
                    duplicateRemoved++;
                    log.info("Duplicate bookmark removed: {} (already has {})", currentWord, originalForm);
                }

            } catch (Exception e) {
                failed++;
                log.error("Failed to process bookmark: {} (userId: {})",
                         bookmark.getWord(), bookmark.getUserId(), e);
                // 실패해도 계속 진행
            }
        }

        MigrationResult result = MigrationResult.builder()
                .deletedWords(deletedWords)
                .deletedVariants(deletedVariants)
                .totalBookmarks(total)
                .restoredWords(restoredWords.size())
                .normalizedBookmarks(normalized)
                .duplicateRemoved(duplicateRemoved)
                .failed(failed)
                .build();

        log.warn("Migration complete: {}", result);
        return result;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class MigrationResult {
        private long deletedWords;
        private long deletedVariants;
        private int totalBookmarks;
        private int restoredWords;
        private int normalizedBookmarks;
        private int duplicateRemoved;
        private int failed;
    }
}