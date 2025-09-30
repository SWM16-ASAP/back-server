package com.linglevel.api.content.book.dto;

import com.linglevel.api.content.common.ProgressStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "챕터 목록 조회 요청")
public class GetChaptersRequest {

    @Schema(description = "진도별 필터링", example = "IN_PROGRESS")
    private ProgressStatus progress;
    
    @Schema(description = "페이지 번호",
            example = "1",
            minimum = "1",
            defaultValue = "1")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    @Builder.Default
    private Integer page = 1;
    
    @Schema(description = "페이지 크기",
            example = "10",
            minimum = "1",
            maximum = "200",
            defaultValue = "10")
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 200, message = "페이지 크기는 200 이하여야 합니다.")
    @Builder.Default
    private Integer limit = 10;
} 