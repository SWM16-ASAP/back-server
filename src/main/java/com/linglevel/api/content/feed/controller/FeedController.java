package com.linglevel.api.content.feed.controller;

import com.linglevel.api.auth.jwt.JwtClaims;
import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.feed.dto.FeedResponse;
import com.linglevel.api.content.feed.dto.GetFeedsRequest;
import com.linglevel.api.content.feed.exception.FeedException;
import com.linglevel.api.content.feed.service.FeedService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Feeds", description = "피드 추천 관련 API")
public class FeedController {

    private final FeedService feedService;

    @Operation(
            summary = "피드 목록 조회",
            description = "필터링 및 정렬 조건에 따라 피드 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<FeedResponse>> getFeeds(
            @ParameterObject @ModelAttribute GetFeedsRequest request,
            @AuthenticationPrincipal JwtClaims claims) {
        String userId = claims != null ? claims.getId() : null;
        PageResponse<FeedResponse> response = feedService.getFeeds(request, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "단일 피드 조회", description = "특정 피드의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{feedId}")
    public ResponseEntity<FeedResponse> getFeed(
            @Parameter(description = "피드 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String feedId,
            @AuthenticationPrincipal JwtClaims claims) {
        String userId = claims != null ? claims.getId() : null;
        FeedResponse response = feedService.getFeed(feedId, userId);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(FeedException.class)
    public ResponseEntity<ExceptionResponse> handleFeedException(FeedException e) {
        log.info("Feed Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}
