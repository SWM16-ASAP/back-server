package com.linglevel.api.word.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    @Schema(description = "원형 (변형 형태인 경우)", example = "go")
    private String originalForm;

    @Schema(description = "변형 형태 타입")
    private VariantType variantType;

    @Schema(description = "품사", example = "[\"verb\", \"noun\"]")
    private List<String> partOfSpeech;

    @Schema(description = "뜻과 예문 목록")
    private List<Definition> definitions;

    @Schema(description = "관련 변형 형태들")
    private RelatedForms relatedForms;

    @Schema(description = "현재 사용자가 해당 단어를 북마크했는지 여부", example = "true")
    private Boolean bookmarked;
}