package com.linglevel.api.word.controller;

import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.dto.WordSearchResponse;
import com.linglevel.api.word.exception.WordsException;
import com.linglevel.api.word.service.WordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.linglevel.api.common.ratelimit.annotation.RateLimit;
import com.linglevel.api.common.ratelimit.annotation.RateLimit.KeyType;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/words")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Words Admin", description = "단어 관리 API (관리자 전용)")
@SecurityRequirement(name = "adminApiKey")
public class WordsAdminController {

    private final WordService wordService;

    @Operation(
            summary = "단어 강제 재분석",
            description = """
                    관리자 전용: 단어를 AI로 강제 재분석합니다.

                    **사용 사례:**
                    1. Homograph 대응 (overwrite=false): 'saw'가 'see'의 과거형으로만 저장된 경우, '톱' 의미를 추가
                    2. 품질 개선 (overwrite=true): 기존 번역/의미의 품질이 낮다는 리포트가 있을 때 완전히 재생성

                    **파라미터:**
                    - overwrite=false (기본): 기존 데이터 유지 + 새로운 의미 추가
                    - overwrite=true: 기존 데이터 삭제 후 완전히 재생성
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재분석 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "단어를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (관리자 권한 필요)",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @RateLimit(capacity = 5, refillMinutes = 10, keyType = KeyType.IP)
    @PostMapping("/{word}/force-analyze")
    public ResponseEntity<WordSearchResponse> forceAnalyzeWord(
            @Parameter(description = "재분석할 단어", example = "saw")
            @PathVariable String word,
            @Parameter(description = "번역 대상 언어", example = "KO")
            @RequestParam(defaultValue = "KO") LanguageCode targetLanguage,
            @Parameter(description = "true: 기존 데이터 삭제 후 재생성, false: 기존 데이터 유지 + 새로운 의미 추가")
            @RequestParam(defaultValue = "false") boolean overwrite) {

        log.info("Admin force-analyze: word='{}', targetLanguage={}, overwrite={}",
                word, targetLanguage, overwrite);

        WordSearchResponse response = wordService.forceReanalyzeWord(word, targetLanguage, overwrite);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(WordsException.class)
    public ResponseEntity<ExceptionResponse> handleWordsException(WordsException e) {
        log.info("Words Admin Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}
