package com.linglevel.api.suggestion.controller;

import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.suggestion.dto.SuggestionRequest;
import com.linglevel.api.suggestion.dto.SuggestionResponse;
import com.linglevel.api.suggestion.service.SuggestionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/suggestions")
@RequiredArgsConstructor
@Tag(name = "Suggestions", description = "고객 건의 관련 API")
public class SuggestionsController {

    private final SuggestionsService suggestionsService;

    @Operation(summary = "고객 건의 제출", description = "고객의 건의사항을 제출받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "건의 제출 성공",
                    content = @Content(schema = @Schema(implementation = SuggestionResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping
    public ResponseEntity<SuggestionResponse> submitSuggestion(@RequestBody SuggestionRequest request) {
        SuggestionResponse response = suggestionsService.saveSuggestion(request);
        return ResponseEntity.ok(response);
    }
}
