package com.linglevel.api.word.controller;

import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.dto.EssentialWordsStatsResponse;
import com.linglevel.api.word.dto.Oxford3000InitResponse;
import com.linglevel.api.word.dto.WordSearchResponse;
import com.linglevel.api.word.exception.WordsException;
import com.linglevel.api.word.service.Oxford3000Service;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/words")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Words Admin", description = "단어 관리 API (관리자 전용)")
@SecurityRequirement(name = "adminApiKey")
public class WordsAdminController {

    private final WordService wordService;
    private final Oxford3000Service oxford3000Service;

    @Operation(
            summary = "단어 강제 재분석",
            description = """
                    관리자 전용: 단어를 AI로 강제 재분석합니다.

                    **사용 사례:**
                    1. Homograph 대응 (overwrite=false): 'saw'가 'see'의 과거형으로만 저장된 경우, '톱' 의미를 추가
                    2. 품질 개선 (overwrite=true, deleteVariants=false): 기존 Variant 관계 유지하면서 Word 재생성
                    3. 완전 초기화 (overwrite=true, deleteVariants=true): Variant + Word 모두 삭제 후 완전 재생성

                    **파라미터:**
                    - overwrite=false (기본): 기존 데이터 유지 + 새로운 의미 추가
                    - overwrite=true, deleteVariants=false: Variant 유지 + Word 재생성 (homograph 추가 가능)
                    - overwrite=true, deleteVariants=true: Variant + Word 모두 삭제 후 완전 재생성
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
            @RequestParam(defaultValue = "false") boolean overwrite,
            @Parameter(description = "true: Variant도 함께 삭제 (완전 초기화), false: Variant 유지 (기본값)")
            @RequestParam(defaultValue = "false") boolean deleteVariants) {

        log.info("Admin force-analyze: word='{}', targetLanguage={}, overwrite={}, deleteVariants={}",
                word, targetLanguage, overwrite, deleteVariants);

        WordSearchResponse response = wordService.forceReanalyzeWord(word, targetLanguage, overwrite, deleteVariants);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Oxford 3000 단어 초기화",
            description = """
                    관리자 전용: Oxford 3000 필수 단어를 일괄 생성/업데이트합니다.

                    **동작 방식:**
                    - 이미 존재하는 단어: 재사용하고 isEssential=true로 업데이트
                    - 존재하지 않는 단어: AI로 분석하여 새로 생성

                    **주의사항:**
                    - 이 작업은 매우 오래 걸릴 수 있습니다 (3000개 단어 × AI 호출)
                    - 비동기 처리를 권장하지만, 현재는 동기 처리로 구현되어 있습니다
                    - Rate limit을 고려하여 천천히 호출하세요

                    **파라미터:**
                    - targetLanguage: 번역 대상 언어 (예: KO, JA)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "초기화 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 실패 (관리자 권한 필요)",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @RateLimit(capacity = 1, refillMinutes = 60, keyType = KeyType.IP)
    @PostMapping("/essential/initialize-oxford3000")
    public ResponseEntity<Oxford3000InitResponse> initializeOxford3000(
            @Parameter(description = "번역 대상 언어", example = "KO")
            @RequestParam(defaultValue = "KO") LanguageCode targetLanguage) {

        log.info("Admin initialize-oxford3000: targetLanguage={}", targetLanguage);

        Oxford3000InitResponse response = oxford3000Service.initializeOxford3000(targetLanguage);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "필수 단어 통계 조회",
            description = """
                    관리자 전용: 필수 단어(isEssential=true)의 통계 정보를 조회합니다.

                    **응답 정보:**
                    - totalEssentialWords: 전체 필수 단어 수
                    - countByTargetLanguage: 번역 대상 언어별 필수 단어 수
                    - countBySourceLanguage: 원본 언어별 필수 단어 수
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 실패 (관리자 권한 필요)",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/essential/stats")
    public ResponseEntity<EssentialWordsStatsResponse> getEssentialWordsStats() {
        log.info("Admin get essential words stats");

        EssentialWordsStatsResponse response = oxford3000Service.getEssentialWordsStats();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "등록된 필수 단어 목록 조회",
            description = """
                    관리자 전용: 이미 등록된 필수 단어(isEssential=true)의 word 문자열 목록을 반환합니다.

                    **사용 사례:**
                    - Python 스크립트에서 중복 등록 방지를 위해 사용
                    - 특정 언어로 필터링 가능 (targetLanguage 파라미터 사용)

                    **파라미터:**
                    - targetLanguage (선택): 특정 번역 대상 언어로 필터링 (예: KO)
                    - 파라미터 없으면 모든 언어의 필수 단어 반환
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 실패 (관리자 권한 필요)",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/essential/words")
    public ResponseEntity<List<String>> getEssentialWords(
            @Parameter(description = "번역 대상 언어 (선택)", example = "KO")
            @RequestParam(required = false) LanguageCode targetLanguage) {

        log.info("Admin get essential words: targetLanguage={}", targetLanguage);

        List<String> words = oxford3000Service.getEssentialWordsList(targetLanguage);
        return ResponseEntity.ok(words);
    }

    @Operation(
            summary = "단어의 필수 여부 설정",
            description = """
                    관리자 전용: 특정 단어의 isEssential 플래그를 수동으로 설정/해제합니다.

                    **사용 사례:**
                    1. 실수로 필수 단어로 표시된 단어를 일반 단어로 변경
                    2. 커스텀 필수 단어 추가 (Oxford 3000 외의 단어)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "업데이트 성공"),
            @ApiResponse(responseCode = "404", description = "단어를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (관리자 권한 필요)",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PatchMapping("/{wordId}/essential")
    public ResponseEntity<Void> updateEssentialStatus(
            @Parameter(description = "단어 ID", example = "507f1f77bcf86cd799439011")
            @PathVariable String wordId,
            @Parameter(description = "필수 단어 여부", example = "true")
            @RequestParam boolean isEssential) {

        log.info("Admin update essential status: wordId={}, isEssential={}", wordId, isEssential);

        oxford3000Service.updateEssentialStatus(wordId, isEssential);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(WordsException.class)
    public ResponseEntity<ExceptionResponse> handleWordsException(WordsException e) {
        log.info("Words Admin Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}
