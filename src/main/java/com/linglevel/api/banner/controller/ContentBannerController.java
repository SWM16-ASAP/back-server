package com.linglevel.api.banner.controller;

import com.linglevel.api.banner.dto.*;
import com.linglevel.api.banner.exception.BannerException;
import com.linglevel.api.banner.service.ContentBannerService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Content Banners", description = "콘텐츠 배너 관련 API")
public class ContentBannerController {

    private final ContentBannerService contentBannerService;

    @Operation(summary = "콘텐츠 배너 목록 조회",
               description = "메인 페이지에 노출할 활성화된 콘텐츠 배너 목록을 조회합니다. 국가별로 필터링 가능하며, 표시 순서에 따라 정렬됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ContentBannerListResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/content-banners")
    public ResponseEntity<ContentBannerListResponse> getContentBanners(
            @ParameterObject @Valid @ModelAttribute GetContentBannersRequest request) {

        log.info("Getting content banners for country: {}", request.getCountryCode());

        List<ContentBannerResponse> banners = contentBannerService.getActiveBanners(request.getCountryCode());

        ContentBannerListResponse response = new ContentBannerListResponse();
        response.setData(banners);

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(BannerException.class)
    public ResponseEntity<ExceptionResponse> handleBannerException(BannerException e) {
        log.info("Banner Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e.getMessage()));
    }

    // 내부 응답 클래스 - 리스트 래핑용
    @Schema(description = "콘텐츠 배너 목록 응답")
    public static class ContentBannerListResponse {
        @Schema(description = "배너 목록")
        private List<ContentBannerResponse> data;

        public List<ContentBannerResponse> getData() {
            return data;
        }

        public void setData(List<ContentBannerResponse> data) {
            this.data = data;
        }
    }
}