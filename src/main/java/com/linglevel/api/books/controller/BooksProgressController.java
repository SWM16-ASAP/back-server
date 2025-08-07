package com.linglevel.api.books.controller;

import com.linglevel.api.books.dto.BooksProgressResponse;
import com.linglevel.api.books.dto.GetBooksProgressRequest;
import com.linglevel.api.books.dto.ProgressResponse;
import com.linglevel.api.books.dto.ProgressUpdateRequest;
import com.linglevel.api.books.exception.BooksException;
import com.linglevel.api.books.service.ProgressService;
import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Books Progress", description = "도서 진도 관리 API")
public class BooksProgressController {

    private final ProgressService progressService;

    @Operation(summary = "읽기 진도 업데이트", description = "사용자의 읽기 진도를 업데이트합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "업데이트 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "책 또는 챕터를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 청크 번호",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PutMapping("/{bookId}/progress")
    public ResponseEntity<ProgressResponse> updateProgress(
            @Parameter(description = "책 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bookId,
            @RequestBody ProgressUpdateRequest request) {
        ProgressResponse response = progressService.updateProgress(bookId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "읽기 진도 조회", description = "특정 책에 대한 사용자의 읽기 진도를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "책을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{bookId}/progress")
    public ResponseEntity<ProgressResponse> getProgress(
            @Parameter(description = "책 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bookId) {
        ProgressResponse response = progressService.getProgress(bookId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "전체 책 읽기 진도 조회", description = "현재 사용자의 모든 책에 대한 읽기 진도를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/progress")
    public ResponseEntity<PageResponse<BooksProgressResponse>> getAllProgress(
            @ParameterObject @ModelAttribute GetBooksProgressRequest request) {
        // PageResponse<BooksProgressResponse> response = progressService.getAllProgress(request);
        // TODO: 추후 구현
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @ExceptionHandler(BooksException.class)
    public ResponseEntity<ExceptionResponse> handleBooksException(BooksException e) {
        log.info("Books Progress Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}