package com.linglevel.api.banner.controller;

import com.linglevel.api.banner.dto.*;
import com.linglevel.api.banner.exception.BannerException;
import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.common.dto.PageResponse;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Admin - Content Banner Management", description = "어드민 - 콘텐츠 배너 관리 API")
@SecurityRequirement(name = "adminApiKey")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentBannerController {

    @Operation(summary = "콘텐츠 배너 생성",
               description = "새로운 콘텐츠 배너를 생성합니다. contentId와 contentType을 통해 실제 콘텐츠 정보를 자동으로 조회하여 설정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "콘텐츠를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 콘텐츠 타입",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 API 키",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/content-banners")
    public ResponseEntity<ContentBannerResponse> createContentBanner(
            @Valid @RequestBody CreateContentBannerRequest request) {

        // TODO: 실제 서비스 구현
        ContentBannerResponse response = new ContentBannerResponse();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "관리자용 콘텐츠 배너 목록 조회",
               description = "관리자용 콘텐츠 배너 목록을 조회합니다. 모든 배너를 조회하며 국가별 필터링이 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 API 키",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/content-banners")
    public ResponseEntity<PageResponse<ContentBannerResponse>> getAdminContentBanners(
            @ParameterObject @Valid @ModelAttribute GetAdminContentBannersRequest request) {

        // TODO: 실제 서비스 구현
        PageResponse<ContentBannerResponse> response = new PageResponse<>();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "콘텐츠 배너 상세 조회",
               description = "특정 콘텐츠 배너의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "배너를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 API 키",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/content-banners/{bannerId}")
    public ResponseEntity<ContentBannerResponse> getContentBanner(
            @Parameter(description = "조회할 배너의 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bannerId) {

        // TODO: 실제 서비스 구현
        ContentBannerResponse response = new ContentBannerResponse();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "콘텐츠 배너 수정",
               description = "콘텐츠 배너의 정보를 부분 업데이트합니다. 제목, 설명, 순서, 활성화 상태 등을 변경할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "배너를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "400", description = "최소 한 개의 필드가 필요함",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 API 키",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PatchMapping("/content-banners/{bannerId}")
    public ResponseEntity<ContentBannerResponse> updateContentBanner(
            @Parameter(description = "수정할 배너의 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bannerId,
            @Valid @RequestBody UpdateContentBannerRequest request) {

        // TODO: 실제 서비스 구현
        ContentBannerResponse response = new ContentBannerResponse();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "콘텐츠 배너 삭제",
               description = "콘텐츠 배너를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공",
                    content = @Content(schema = @Schema(implementation = DeleteResponse.class))),
            @ApiResponse(responseCode = "404", description = "배너를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 API 키",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @DeleteMapping("/content-banners/{bannerId}")
    public ResponseEntity<DeleteResponse> deleteContentBanner(
            @Parameter(description = "삭제할 배너의 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String bannerId) {

        // TODO: 실제 서비스 구현
        DeleteResponse response = new DeleteResponse("Banner deleted successfully.");
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(BannerException.class)
    public ResponseEntity<ExceptionResponse> handleBannerException(BannerException e) {
        log.info("Banner Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e.getMessage()));
    }

    // 삭제 응답용 내부 클래스
    @Schema(description = "삭제 응답")
    public static class DeleteResponse {
        @Schema(description = "응답 메시지", example = "Banner deleted successfully.")
        private String message;

        public DeleteResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}