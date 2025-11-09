package com.linglevel.api.admin.migration;

import com.linglevel.api.content.custom.entity.CustomContent;
import com.linglevel.api.content.custom.entity.UserCustomContent;
import com.linglevel.api.content.custom.repository.CustomContentRepository;
import com.linglevel.api.content.custom.repository.UserCustomContentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * UserCustomContent 마이그레이션 컨트롤러
 * 기존 CustomContent의 userId를 기반으로 UserCustomContent 매핑 생성
 */
@RestController
@RequestMapping("/api/v1/admin/migration")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Migration", description = "관리자 마이그레이션 API")
@SecurityRequirement(name = "adminApiKey")
public class UserCustomContentMigrationController {

    private final CustomContentRepository customContentRepository;
    private final UserCustomContentRepository userCustomContentRepository;

    @PostMapping("/custom-content-to-user-mapping")
    @Operation(
            summary = "CustomContent → UserCustomContent 마이그레이션",
            description = "기존 CustomContent의 userId를 기반으로 UserCustomContent 매핑 생성. " +
                          "이미 삭제된 콘텐츠는 제외하고 처리됩니다."
    )
    @Transactional
    public MigrationResult migrateCustomContentToUserMapping() {
        log.warn("Starting CustomContent to UserCustomContent migration...");

        // 삭제되지 않은 모든 CustomContent 조회
        List<CustomContent> allContents = customContentRepository.findAll().stream()
                .filter(content -> content.getIsDeleted() == null || !content.getIsDeleted())
                .toList();

        int total = allContents.size();
        int created = 0;
        int skipped = 0;
        int failed = 0;

        log.info("Found {} active CustomContent records to migrate", total);

        for (CustomContent content : allContents) {
            try {
                String userId = content.getUserId();
                String customContentId = content.getId();
                String contentRequestId = content.getContentRequestId();

                // userId가 없는 경우 스킵 (데이터 무결성 문제)
                if (userId == null || userId.isBlank()) {
                    log.warn("Skipping content {} - missing userId", customContentId);
                    skipped++;
                    continue;
                }

                // 이미 매핑이 존재하는지 확인
                boolean exists = userCustomContentRepository.existsByUserIdAndCustomContentId(userId, customContentId);
                if (exists) {
                    log.debug("Mapping already exists for user: {} and content: {}", userId, customContentId);
                    skipped++;
                    continue;
                }

                // UserCustomContent 생성
                UserCustomContent userCustomContent = UserCustomContent.builder()
                        .userId(userId)
                        .customContentId(customContentId)
                        .contentRequestId(contentRequestId)
                        .build();

                try {
                    userCustomContentRepository.save(userCustomContent);
                    created++;
                    log.debug("Created mapping for user: {} and content: {}", userId, customContentId);
                } catch (DuplicateKeyException e) {
                    // Compound Index의 unique 제약 조건 위반 (거의 발생하지 않음)
                    log.info("Duplicate mapping detected for user: {} and content: {} - skipping", userId, customContentId);
                    skipped++;
                }

            } catch (Exception e) {
                failed++;
                log.error("Failed to migrate content: {} (userId: {})",
                        content.getId(), content.getUserId(), e);
            }
        }

        MigrationResult result = MigrationResult.builder()
                .totalContents(total)
                .createdMappings(created)
                .skippedMappings(skipped)
                .failedMappings(failed)
                .build();

        log.warn("Migration complete: {}", result);
        return result;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class MigrationResult {
        private int totalContents;
        private int createdMappings;
        private int skippedMappings;
        private int failedMappings;
    }
}
