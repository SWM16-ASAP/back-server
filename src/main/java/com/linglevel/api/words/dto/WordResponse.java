package com.linglevel.api.words.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "단어 응답")
public class WordResponse {
    @Schema(description = "단어 ID", example = "60d0fe4f5311236168a109ca")
    private String id;
    
    @Schema(description = "단어", example = "magnificent")
    private String word;
}