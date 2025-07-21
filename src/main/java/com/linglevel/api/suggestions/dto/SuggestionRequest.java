package com.linglevel.api.suggestions.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SuggestionRequest {
    @Schema(description = "건의자 이메일", example = "user@example.com")
    private String email;

    @Schema(description = "건의 태그 (쉼표로 구분)", example = "bug,ui,feature")
    private String tags;

    @Schema(description = "건의 내용", example = "이런이런 기능이 추가되었으면 좋겠습니다.", required = true)
    private String content;
}
