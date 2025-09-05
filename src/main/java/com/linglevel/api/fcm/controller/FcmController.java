package com.linglevel.api.fcm.controller;

import com.linglevel.api.fcm.dto.*;
import com.linglevel.api.fcm.exception.FcmException;
import com.linglevel.api.fcm.service.FcmTokenService;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.repository.UserRepository;
import com.linglevel.api.common.dto.ExceptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fcm")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "FCM Token Management", description = "Firebase Cloud Messaging 토큰 관리 API")
public class FcmController {

    private final FcmTokenService fcmTokenService;
    private final UserRepository userRepository;

    @Operation(summary = "FCM 토큰 등록/업데이트", 
               description = "사용자의 FCM 토큰을 등록하거나 업데이트합니다. 동일한 사용자+디바이스 조합이 이미 존재하는 경우 토큰을 업데이트하고, 존재하지 않는 경우 새로 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "FCM 토큰 업데이트 성공",
                    content = @Content(schema = @Schema(implementation = FcmTokenUpdateResponse.class))),
            @ApiResponse(responseCode = "201", description = "FCM 토큰 생성 성공",
                    content = @Content(schema = @Schema(implementation = FcmTokenCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 필드 누락)",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PutMapping("/token")
    public ResponseEntity<?> upsertFcmToken(@Valid @RequestBody FcmTokenUpsertRequest request, 
                                             Authentication authentication) {
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        
        FcmTokenUpsertResult result = fcmTokenService.upsertFcmToken(user.getId(), request);
        
        if (result.isCreated()) {
            FcmTokenCreateResponse response = FcmTokenCreateResponse.builder()
                    .message("FCM token created successfully.")
                    .tokenId(result.getTokenId())
                    .build();
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            FcmTokenUpdateResponse response = FcmTokenUpdateResponse.builder()
                    .message("FCM token updated successfully.")
                    .tokenId(result.getTokenId())
                    .build();
            return ResponseEntity.ok(response);
        }
    }

    @ExceptionHandler(FcmException.class)
    public ResponseEntity<ExceptionResponse> handleFcmException(FcmException e) {
        log.error("FCM Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}