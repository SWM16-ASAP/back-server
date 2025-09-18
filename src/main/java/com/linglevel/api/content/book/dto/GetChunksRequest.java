package com.linglevel.api.content.book.dto;

import com.linglevel.api.content.common.DifficultyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "청크 목록 조회 요청")
public class GetChunksRequest {
    
    @Schema(description = "청크의 난이도", example = "A1", required = true)
    @NotNull(message = "난이도는 필수입니다.")
    private DifficultyLevel difficultyLevel;
    
    @Schema(description = "페이지 번호",
            example = "1",
            minimum = "1",
            defaultValue = "1")
    @Builder.Default
    private Integer page = 1;

    @Schema(description = "페이지 크기",
            example = "10",
            minimum = "1",
            maximum = "200",
            defaultValue = "10")
    @Builder.Default
    private Integer limit = 10;
} 