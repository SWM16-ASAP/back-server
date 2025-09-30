package com.linglevel.api.content.common.controller;

import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.common.dto.GetRecentContentsRequest;
import com.linglevel.api.content.common.dto.RecentContentResponse;
import com.linglevel.api.content.common.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contents")
@RequiredArgsConstructor
@Tag(name = "Contents", description = "통합 콘텐츠 관련 API")
public class ContentController {

    private final ContentService contentService;

    @Operation(summary = "최근 공부 콘텐츠 목록 조회", description = "사용자가 최근에 공부한 모든 타입의 콘텐츠 목록을 최신순으로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/recent")
    public ResponseEntity<PageResponse<RecentContentResponse>> getRecentContents(
            @AuthenticationPrincipal String username,
            @ParameterObject @ModelAttribute GetRecentContentsRequest request) {
        PageResponse<RecentContentResponse> response = contentService.getRecentContents(username, request);
        return ResponseEntity.ok(response);
    }
}
