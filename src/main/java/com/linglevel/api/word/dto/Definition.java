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
@Schema(description = "단어 뜻과 예문")
public class Definition {
    @Schema(description = "한국어 뜻")
    private List<String> meaningsKo;

    @Schema(description = "일본어 뜻")
    private List<String> meaningsJa;


    @Schema(description = "예문 목록")
    private List<String> examples;
}
