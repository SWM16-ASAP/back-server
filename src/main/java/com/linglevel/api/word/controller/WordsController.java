package com.linglevel.api.word.controller;

import com.linglevel.api.auth.jwt.JwtClaims;
import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.word.dto.WordSearchRequest;
import com.linglevel.api.word.dto.WordSearchResponse;
import com.linglevel.api.word.exception.WordsException;
import com.linglevel.api.word.service.WordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/words")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Words", description = "단어 관련 API")
public class WordsController {

    private final WordService wordService;

    @Operation(summary = "단일 단어 조회", description = "특정 단어의 상세 정보를 조회합니다. Homograph인 경우 여러 결과를 반환합니다. 현재 사용자의 북마크 상태도 함께 반환됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "단어를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{word}")
    public ResponseEntity<WordSearchResponse> getWord(
            @Parameter(description = "조회할 단어", example = "saw")
            @PathVariable String word,
            @ParameterObject @Valid @ModelAttribute WordSearchRequest request,
            @AuthenticationPrincipal JwtClaims claims) {
        WordSearchResponse response = wordService.getOrCreateWords(claims.getId(), word, request.getTargetLanguage());
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(WordsException.class)
    public ResponseEntity<ExceptionResponse> handleWordsException(WordsException e) {
        log.info("Words Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}