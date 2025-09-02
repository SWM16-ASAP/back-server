package com.linglevel.api.content.custom.dto;

import com.linglevel.api.content.common.DifficultyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Schema(description = "커스텀 콘텐츠 청크 목록 조회 요청")
public class GetCustomContentChunksRequest {
    
    @Schema(description = "청크의 난이도", example = "A1", required = true)
    @NotNull(message = "난이도는 필수입니다.")
    private DifficultyLevel difficulty;
    
    @Schema(description = "페이지 번호", example = "1", defaultValue = "1")
    private Integer page = 1;
    
    @Schema(description = "페이지 당 항목 수", example = "10", defaultValue = "10")
    private Integer limit = 10;
}