package com.linglevel.api.suggestions.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionResponse {
    @Schema(description = "응답 메시지", example = "Suggestion submitted successfully.")
    private String message;
}
