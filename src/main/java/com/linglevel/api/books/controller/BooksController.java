package com.linglevel.api.books.controller;

import com.linglevel.api.books.dto.*;
import com.linglevel.api.books.exception.BooksException;
import com.linglevel.api.common.dto.ExceptionResponseDTO;
import com.linglevel.api.common.dto.PageResponseDTO;
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
@Tag(name = "Books", description = "도서 관련 API")
public class BooksController {

    @Operation(summary = "책 목록 조회", description = "책 목록을 조건에 따라 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
                    content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponseDTO<BookResponse>> getBooks(
            @ParameterObject @ModelAttribute GetBooksRequest request) {
        // TODO: 책 목록 조회 로직 구현
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Operation(summary = "단일 책 조회", description = "특정 책의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "책을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class)))
    })
    @GetMapping("/{bookId}")
    public ResponseEntity<BookResponse> getBook(
            @Parameter(description = "책 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bookId) {
        // TODO: 단일 책 조회 로직 구현
         throw new UnsupportedOperationException("Not implemented yet");
    }

    @Operation(summary = "챕터 목록 조회", description = "특정 책의 챕터 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "책을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class)))
    })
    @GetMapping("/{bookId}/chapters")
    public ResponseEntity<PageResponseDTO<ChapterResponse>> getChapters(
            @Parameter(description = "책 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bookId,
            @ParameterObject @ModelAttribute GetChaptersRequest request) {
        // TODO: 챕터 목록 조회 로직 구현
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Operation(summary = "단일 챕터 조회", description = "특정 챕터의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "책 또는 챕터를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class)))
    })
    @GetMapping("/{bookId}/chapters/{chapterId}")
    public ResponseEntity<ChapterResponse> getChapter(
            @Parameter(description = "책 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bookId,
            @Parameter(description = "챕터 ID", example = "60d0fe4f5311236168a109cb")
            @PathVariable String chapterId) {
        // TODO: 단일 챕터 조회 로직 구현
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Operation(summary = "청크 목록 조회", description = "특정 챕터의 청크 목록을 난이도별로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "챕터를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 난이도 레벨",
                    content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class)))
    })
    @GetMapping("/{bookId}/chapters/{chapterId}/chunks")
    public ResponseEntity<PageResponseDTO<ChunkResponse>> getChunks(
            @Parameter(description = "책 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bookId,
            @Parameter(description = "챕터 ID", example = "60d0fe4f5311236168a109cb")
            @PathVariable String chapterId,
            @ParameterObject @ModelAttribute GetChunksRequest request) {
        // TODO: 청크 목록 조회 로직 구현
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Operation(summary = "단일 청크 조회", description = "특정 청크의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "책, 챕터 또는 청크를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class)))
    })
    @GetMapping("/{bookId}/chapters/{chapterId}/chunks/{chunkId}")
    public ResponseEntity<ChunkResponse> getChunk(
            @Parameter(description = "책 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bookId,
            @Parameter(description = "챕터 ID", example = "60d0fe4f5311236168a109cb")
            @PathVariable String chapterId,
            @Parameter(description = "청크 ID", example = "60d0fe4f5311236168a109cd")
            @PathVariable String chunkId) {
        // TODO: 단일 청크 조회 로직 구현
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Operation(summary = "읽기 진도 업데이트", description = "사용자의 읽기 진도를 업데이트합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "업데이트 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "책 또는 챕터를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 청크 번호",
                    content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class)))
    })
    @PutMapping("/{bookId}/progress")
    public ResponseEntity<ProgressResponse> updateProgress(
            @Parameter(description = "책 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bookId,
            @RequestBody ProgressUpdateRequest request) {
        // TODO: 읽기 진도 업데이트 로직 구현
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Operation(summary = "읽기 진도 조회", description = "특정 책에 대한 사용자의 읽기 진도를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "책을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class)))
    })
    @GetMapping("/{bookId}/progress")
    public ResponseEntity<ProgressResponse> getProgress(
            @Parameter(description = "책 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bookId) {
        // TODO: 읽기 진도 조회 로직 구현
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @ExceptionHandler(BooksException.class)
    public ResponseEntity<ExceptionResponseDTO> handleBooksException(BooksException e) {
        log.info("Books Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponseDTO(e));
    }
} 