package com.linglevel.api.word.dto;

import com.linglevel.api.i18n.LanguageCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 단어 검색 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "단어 검색 요청")
public class WordSearchRequest {

    @NotNull(message = "번역 대상 언어는 필수입니다")
    @Schema(description = "번역 대상 언어 코드", example = "KO", required = true)
    private LanguageCode targetLanguage;
}
