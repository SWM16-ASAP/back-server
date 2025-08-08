package com.linglevel.api.bookmarks.controller;

import com.linglevel.api.bookmarks.dto.BookmarkedWordResponse;
import com.linglevel.api.bookmarks.dto.BookmarkToggleResponse;
import com.linglevel.api.bookmarks.dto.GetBookmarkedWordsRequest;
import com.linglevel.api.bookmarks.exception.BookmarksException;
import com.linglevel.api.bookmarks.service.BookmarkService;
import com.linglevel.api.users.repository.UserRepository;
import com.linglevel.api.users.entity.User;
import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.common.dto.MessageResponse;
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

@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bookmarks", description = "북마크 관련 API")
public class BookmarksController {

    private final BookmarkService bookmarkService;
    private final UserRepository userRepository;

    @Operation(summary = "북마크된 단어 목록 조회", description = "현재 사용자가 북마크한 단어 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/words")
    public ResponseEntity<PageResponse<BookmarkedWordResponse>> getBookmarkedWords(
            @ParameterObject @ModelAttribute GetBookmarkedWordsRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        var bookmarkedWords = bookmarkService.getBookmarkedWords(user.getId(), request.getPage(), request.getLimit(), request.getSearch());
        return ResponseEntity.ok(new PageResponse<>(bookmarkedWords.getContent(), bookmarkedWords));
    }

    @Operation(summary = "단어 북마크 추가", description = "특정 단어를 북마크에 추가합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "북마크 추가 성공",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "404", description = "단어를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 북마크된 단어",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/words/{word}")
    public ResponseEntity<MessageResponse> addWordBookmark(
            @Parameter(description = "북마크할 단어", example = "magnificent")
            @PathVariable String word,
            Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        bookmarkService.addWordBookmark(user.getId(), word);
        return ResponseEntity.ok(new MessageResponse("Word bookmarked successfully."));
    }

    @Operation(summary = "단어 북마크 제거", description = "특정 단어를 북마크에서 제거합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "북마크 제거 성공",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "404", description = "단어 또는 북마크를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @DeleteMapping("/words/{word}")
    public ResponseEntity<MessageResponse> removeWordBookmark(
            @Parameter(description = "북마크 해제할 단어", example = "magnificent")
            @PathVariable String word,
            Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        bookmarkService.removeWordBookmark(user.getId(), word);
        return ResponseEntity.ok(new MessageResponse("Word bookmark removed successfully."));
    }

    @Operation(summary = "단어 북마크 토글", description = "특정 단어의 북마크 상태를 토글합니다. 북마크되어 있으면 제거하고, 북마크되어 있지 않으면 추가합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토글 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PutMapping("/words/{word}/toggle")
    public ResponseEntity<BookmarkToggleResponse> toggleWordBookmark(
            @Parameter(description = "토글할 단어", example = "magnificent")
            @PathVariable String word,
            Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        boolean bookmarked = bookmarkService.toggleWordBookmark(user.getId(), word);
        return ResponseEntity.ok(new BookmarkToggleResponse(bookmarked));
    }

    @ExceptionHandler(BookmarksException.class)
    public ResponseEntity<ExceptionResponse> handleBookmarksException(BookmarksException e) {
        log.info("Bookmarks Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}