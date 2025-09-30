package com.linglevel.api.content.book.controller;

import com.linglevel.api.content.book.dto.BooksProgressResponse;
import com.linglevel.api.content.book.dto.GetBooksProgressRequest;
import com.linglevel.api.content.book.dto.ProgressResponse;
import com.linglevel.api.content.book.dto.ProgressUpdateRequest;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.service.ProgressService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

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
            @Valid @RequestBody ProgressUpdateRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        ProgressResponse response = progressService.updateProgress(bookId, request, username);
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
            @PathVariable String bookId,
            Authentication authentication) {
        String username = authentication.getName();
        ProgressResponse response = progressService.getProgress(bookId, username);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "읽기 진도 초기화", description = "사용자의 현재 읽기 진도를 0으로 초기화합니다. 최대 진도 기록은 유지됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "초기화 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "책 또는 진도 기록을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/{bookId}/progress/reset")
    public ResponseEntity<ProgressResponse> resetProgress(
            @Parameter(description = "책 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bookId,
            Authentication authentication) {
        String username = authentication.getName();
        ProgressResponse response = progressService.resetProgress(bookId, username);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "읽기 진도 삭제", description = "사용자의 읽기 진도 기록을 완전히 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "책 또는 진도 기록을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @DeleteMapping("/{bookId}/progress")
    public ResponseEntity<Void> deleteProgress(
            @Parameter(description = "책 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bookId,
            Authentication authentication) {
        String username = authentication.getName();
        progressService.deleteProgress(bookId, username);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(BooksException.class)
    public ResponseEntity<ExceptionResponse> handleBooksException(BooksException e) {
        log.info("Books Progress Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}