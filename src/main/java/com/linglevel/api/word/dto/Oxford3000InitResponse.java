package com.linglevel.api.word.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Oxford 3000 초기화 응답")
public class Oxford3000InitResponse {

    @Schema(description = "초기화 시작 시간")
    private LocalDateTime startedAt;

    @Schema(description = "초기화 완료 시간")
    private LocalDateTime completedAt;

    @Schema(description = "총 단어 수", example = "3417")
    private Integer totalWords;

    @Schema(description = "성공한 단어 수", example = "3400")
    private Integer successCount;

    @Schema(description = "실패한 단어 수", example = "17")
    private Integer failureCount;

    @Schema(description = "이미 존재하던 단어 수 (업데이트만 수행)", example = "1500")
    private Integer alreadyExistCount;

    @Schema(description = "새로 생성된 단어 수", example = "1900")
    private Integer newlyCreatedCount;

    @Schema(description = "실패한 단어 리스트")
    private java.util.List<String> failedWords;

    @Schema(description = "처리 상태 메시지")
    private String message;
}