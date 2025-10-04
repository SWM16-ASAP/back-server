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
@Schema(description = "관련 변형 형태들")
public class RelatedForms {
    @Schema(description = "동사 활용형")
    private Conjugations conjugations;

    @Schema(description = "형용사/부사 비교급")
    private Comparatives comparatives;

    @Schema(description = "명사 복수형")
    private Plural plural;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "동사 활용형")
    public static class Conjugations {
        @Schema(description = "현재형")
        private String present;

        @Schema(description = "과거형")
        private String past;

        @Schema(description = "과거분사")
        private String pastParticiple;

        @Schema(description = "현재분사")
        private String presentParticiple;

        @Schema(description = "3인칭 단수")
        private String thirdPerson;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "형용사/부사 비교급")
    public static class Comparatives {
        @Schema(description = "원급")
        private String positive;

        @Schema(description = "비교급")
        private String comparative;

        @Schema(description = "최상급")
        private String superlative;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "명사 복수형")
    public static class Plural {
        @Schema(description = "단수형")
        private String singular;

        @Schema(description = "복수형")
        private String plural;
    }
}
