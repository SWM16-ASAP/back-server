package com.linglevel.api.word.dto;

import com.linglevel.api.i18n.LanguageCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "필수 단어 통계 응답")
public class EssentialWordsStatsResponse {

    @Schema(description = "총 필수 단어 수", example = "3000")
    private Long totalEssentialWords;

    @Schema(description = "언어별 필수 단어 수")
    private Map<LanguageCode, Long> countByTargetLanguage;

    @Schema(description = "원본 언어별 필수 단어 수")
    private Map<LanguageCode, Long> countBySourceLanguage;
}