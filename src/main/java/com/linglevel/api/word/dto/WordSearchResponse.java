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
@Schema(description = "단어 검색 응답")
public class WordSearchResponse {
    @Schema(description = "사용자가 검색한 단어", example = "saw")
    private String searchedWord;

    @Schema(description = "검색 결과 목록 (일반 단어는 1개, Homograph는 2개 이상)")
    private List<WordResponse> results;
}