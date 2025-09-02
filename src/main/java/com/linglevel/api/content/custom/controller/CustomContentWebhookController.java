package com.linglevel.api.content.custom.controller;

import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.common.dto.MessageResponse;
import com.linglevel.api.common.exception.CommonErrorCode;
import com.linglevel.api.common.exception.CommonException;
import com.linglevel.api.content.custom.dto.*;
import com.linglevel.api.content.custom.exception.CustomContentException;
import com.linglevel.api.content.custom.service.CustomContentWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/custom-contents/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Custom Content Webhooks", description = "AI 처리 결과 웹훅 API")
public class CustomContentWebhookController {

    private final CustomContentWebhookService customContentWebhookService;

    @Value("${import.api.key}")
    private String importApiKey;

    @Operation(
        summary = "AI 콘텐츠 처리 완료 웹훅", 
        description = "AI가 콘텐츠 처리를 완료했을 때 결과 JSON 파일의 위치를 전달하여 백엔드에서 처리하도록 하는 웹훅 API입니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "처리 성공", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "404", description = "요청을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 상태",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 API 키",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/completed")
    public ResponseEntity<CustomContentCompletedResponse> handleContentCompleted(
            @RequestHeader(value = "X-API-Key", required = true) String apiKey,
            @Valid @RequestBody CustomContentCompletedRequest request) {

        if (!importApiKey.equals(apiKey)) {
            log.warn("Invalid API key provided for custom content completion webhook");
            throw new CommonException(CommonErrorCode.UNAUTHORIZED);
        }

        CustomContentCompletedResponse response = customContentWebhookService.handleContentCompleted(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "AI 콘텐츠 처리 실패 웹훅", 
        description = "AI 콘텐츠 처리가 실패했을 때 요청 상태를 업데이트하고 사용자에게 실패 알림을 발송하는 웹훅 API입니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "처리 성공", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "404", description = "요청을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 API 키",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/failed")
    public ResponseEntity<MessageResponse> handleContentFailed(
            @RequestHeader(value = "X-API-Key", required = true) String apiKey,
            @Valid @RequestBody CustomContentFailedRequest request) {

        if (!importApiKey.equals(apiKey)) {
            log.warn("Invalid API key provided for custom content failure webhook");
            throw new CommonException(CommonErrorCode.UNAUTHORIZED);
        }

        customContentWebhookService.handleContentFailed(request);
        return ResponseEntity.ok(new MessageResponse("Content request marked as failed successfully"));
    }

    @Operation(
        summary = "AI 콘텐츠 처리 진행률 웹훅", 
        description = "AI 콘텐츠 처리 중 진행률을 업데이트하는 웹훅 API입니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "처리 성공", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "404", description = "요청을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 API 키",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/progress")
    public ResponseEntity<MessageResponse> handleContentProgress(
            @RequestHeader(value = "X-API-Key", required = true) String apiKey,
            @Valid @RequestBody CustomContentProgressRequest request) {

        if (!importApiKey.equals(apiKey)) {
            log.warn("Invalid API key provided for custom content progress webhook");
            throw new CommonException(CommonErrorCode.UNAUTHORIZED);
        }

        customContentWebhookService.handleContentProgress(request);
        return ResponseEntity.ok(new MessageResponse("Progress updated successfully"));
    }

    @ExceptionHandler(CustomContentException.class)
    public ResponseEntity<ExceptionResponse> handleCustomContentException(CustomContentException e) {
        log.info("Custom Content Webhook Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}