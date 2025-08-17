package com.linglevel.api.word.dto;

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
    
    @Schema(description = "현재 사용자가 해당 단어를 북마크했는지 여부", example = "true")
    private Boolean bookmarked;
}