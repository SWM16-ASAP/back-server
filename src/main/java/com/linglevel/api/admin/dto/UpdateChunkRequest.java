package com.linglevel.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Schema(description = "청크 수정 요청")
public class UpdateChunkRequest {

    @Schema(description = "수정할 청크 내용 (텍스트 청크의 경우 텍스트, 이미지 청크의 경우 이미지 URL)", example = "Updated chunk content...")
    @NotBlank(message = "Content is required")
    private String content;

    @Schema(description = "이미지 청크의 설명 (선택사항, 이미지 청크인 경우에만 사용)", example = "Updated description for image chunks")
    private String description;
}