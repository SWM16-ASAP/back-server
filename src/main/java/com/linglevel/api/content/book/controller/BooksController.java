package com.linglevel.api.content.book.controller;

import com.linglevel.api.content.book.dto.*;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.service.BookService;
import com.linglevel.api.content.book.service.ChapterService;
import com.linglevel.api.content.book.service.ChunkService;
import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.common.exception.CommonErrorCode;
import com.linglevel.api.common.exception.CommonException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;



@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Books", description = "도서 관련 API")
public class BooksController {

    private final BookService bookService;
    private final ChapterService chapterService;
    private final ChunkService chunkService;


    @Operation(summary = "책 목록 조회", description = "책 목록을 조건에 따라 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<BookResponse>> getBooks(
            @ParameterObject @Valid @ModelAttribute GetBooksRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        PageResponse<BookResponse> response = bookService.getBooks(request, username);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "단일 책 조회", description = "특정 책의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "책을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{bookId}")
    public ResponseEntity<BookResponse> getBook(
            @Parameter(description = "책 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bookId,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        BookResponse response = bookService.getBook(bookId, username);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "챕터 목록 조회", description = "특정 책의 챕터 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "책을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{bookId}/chapters")
    public ResponseEntity<PageResponse<ChapterResponse>> getChapters(
            @Parameter(description = "책 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bookId,
            @ParameterObject @Valid @ModelAttribute GetChaptersRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        PageResponse<ChapterResponse> response = chapterService.getChapters(bookId, request, username);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "단일 챕터 조회", description = "특정 챕터의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "책 또는 챕터를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{bookId}/chapters/{chapterId}")
    public ResponseEntity<ChapterResponse> getChapter(
            @Parameter(description = "책 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bookId,
            @Parameter(description = "챕터 ID", example = "60d0fe4f5311236168a109cb")
            @PathVariable String chapterId,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        ChapterResponse response = chapterService.getChapter(bookId, chapterId, username);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "청크 목록 조회", description = "특정 챕터의 청크 목록을 난이도별로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "챕터를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 난이도 레벨",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{bookId}/chapters/{chapterId}/chunks")
    public ResponseEntity<PageResponse<ChunkResponse>> getChunks(
            @Parameter(description = "책 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bookId,
            @Parameter(description = "챕터 ID", example = "60d0fe4f5311236168a109cb")
            @PathVariable String chapterId,
            @ParameterObject @Valid @ModelAttribute GetChunksRequest request) {
        PageResponse<ChunkResponse> response = chunkService.getChunks(bookId, chapterId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "단일 청크 조회", description = "특정 청크의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "책, 챕터 또는 청크를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{bookId}/chapters/{chapterId}/chunks/{chunkId}")
    public ResponseEntity<ChunkResponse> getChunk(
            @Parameter(description = "책 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bookId,
            @Parameter(description = "챕터 ID", example = "60d0fe4f5311236168a109cb")
            @PathVariable String chapterId,
            @Parameter(description = "청크 ID", example = "60d0fe4f5311236168a109cd")
            @PathVariable String chunkId) {
        ChunkResponse response = chunkService.getChunk(bookId, chapterId, chunkId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "책 데이터 import", description = "S3에 저장된 JSON 파일을 읽어서 새로운 책과 관련 챕터, 청크 데이터를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "500", description = "import 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @SecurityRequirement(name = "adminApiKey")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import")
    public ResponseEntity<BookImportResponse> importBook(
            @RequestBody BookImportRequest request) {

        BookImportResponse response = bookService.importBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ExceptionHandler(BooksException.class)
    public ResponseEntity<ExceptionResponse> handleBooksException(BooksException e) {
        log.info("Books Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
} 