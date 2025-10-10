package com.linglevel.api.word.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.linglevel.api.i18n.LanguageCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI로부터 받은 단어 분석 결과
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordAnalysisResult {
    @NotBlank(message = "originalForm은 필수입니다")
    @JsonProperty("originalForm")
    private String originalForm;

    @JsonProperty("variantType")
    private VariantType variantType;

    @NotNull(message = "sourceLanguageCode는 필수입니다")
    @JsonProperty("sourceLanguageCode")
    private LanguageCode sourceLanguageCode;

    @NotNull(message = "targetLanguageCode는 필수입니다")
    @JsonProperty("targetLanguageCode")
    private LanguageCode targetLanguageCode;

    @Size(max = 3, message = "summary는 최대 3개까지 가능합니다")
    @JsonProperty("summary")
    private List<String> summary;

    @Size(max = 15, message = "meanings는 최대 15개까지 가능합니다")
    @Valid
    @JsonProperty("meanings")
    private List<Meaning> meanings;

    @JsonProperty("conjugations")
    private RelatedForms.Conjugations conjugations;

    @JsonProperty("comparatives")
    private RelatedForms.Comparatives comparatives;

    @JsonProperty("plural")
    private RelatedForms.Plural plural;
}
