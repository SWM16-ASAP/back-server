package com.linglevel.api.word.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordAnalysisResult {
    @JsonProperty("originalForm")
    private String originalForm;

    @JsonProperty("variantType")
    private VariantType variantType;

    @JsonProperty("partOfSpeech")
    private List<String> partOfSpeech;

    @JsonProperty("conjugations")
    private RelatedForms.Conjugations conjugations;

    @JsonProperty("comparatives")
    private RelatedForms.Comparatives comparatives;

    @JsonProperty("plural")
    private RelatedForms.Plural plural;

    @JsonProperty("meaningsKo")
    private List<String> meaningsKo;

    @JsonProperty("meaningsJa")
    private List<String> meaningsJa;

    @JsonProperty("examples")
    private List<String> examples;
}
