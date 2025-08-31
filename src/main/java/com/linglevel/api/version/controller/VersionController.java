package com.linglevel.api.version.controller;

import com.linglevel.api.version.dto.VersionResponse;
import com.linglevel.api.version.service.VersionService;
import com.linglevel.api.version.exception.VersionException;
import com.linglevel.api.common.dto.ExceptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/version")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Version", description = "앱 버전 관리 API")
public class VersionController {
    
    private final VersionService versionService;

    @Operation(summary = "앱 버전 정보 조회", description = "클라이언트에서 사용할 앱의 최신 버전과 최소 요구 버전을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "버전 정보를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping
    public ResponseEntity<VersionResponse> getVersion() {
        VersionResponse response = versionService.getVersion();
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(VersionException.class)
    public ResponseEntity<ExceptionResponse> handleVersionException(VersionException e) {
        log.info("Version Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}