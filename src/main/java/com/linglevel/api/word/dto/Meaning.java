package com.linglevel.api.word.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "단어의 의미 (품사별)")
public class Meaning {
    @NotNull(message = "품사는 필수입니다")
    @Schema(description = "품사", example = "VERB")
    private PartOfSpeech partOfSpeech;

    @NotBlank(message = "의미는 필수입니다")
    @Schema(description = "의미", example = "보다, 시각적으로 인지하다")
    private String meaning;

    @NotBlank(message = "예문은 필수입니다")
    @Schema(description = "예문", example = "I <see> him at the store yesterday.")
    private String example;

    @NotBlank(message = "예문 번역은 필수입니다")
    @Schema(description = "예문 번역", example = "나는 어제 가게에서 그를 봤습니다.")
    private String exampleTranslation;
}