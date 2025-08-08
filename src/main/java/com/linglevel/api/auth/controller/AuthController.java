package com.linglevel.api.auth.controller;

import com.linglevel.api.auth.dto.*;
import com.linglevel.api.auth.exception.AuthException;
import com.linglevel.api.auth.jwt.JwtClaims;
import com.linglevel.api.auth.jwt.JwtService;
import com.linglevel.api.auth.service.AuthService;
import com.linglevel.api.common.dto.ExceptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @Operation(summary = "Firebase OAuth 로그인", description = "Firebase OAuth를 통해 소셜 로그인하고 JWT 토큰을 발급받습니다.",
            security = @SecurityRequirement(name = ""))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Firebase 토큰 인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/oauth/login")
    public ResponseEntity<LoginResponse> oauthLogin(@RequestBody OauthLoginRequest request) {
        LoginResponse response = authService.authenticateWithFirebase(request.getAuthCode());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공",
                    content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "리프레시 토큰 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        // TODO: 토큰 갱신 로직 구현
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Operation(summary = "로그아웃", description = "현재 세션을 종료하고 토큰을 무효화합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공",
                    content = @Content(schema = @Schema(implementation = LogoutResponse.class))),
            @ApiResponse(responseCode = "401", description = "토큰 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout() {
        // TODO: 로그아웃 로직 구현
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Operation(summary = "현재 사용자 정보 조회", description = "현재 Access Token에 포함된 JWT Claims 정보를 추출하여 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = JwtClaims.class))),
            @ApiResponse(responseCode = "401", description = "토큰 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<JwtClaims> getCurrentUser(HttpServletRequest request) {
        JwtClaims claims = jwtService.extractJwtClaimsFromRequest(request);
        return ResponseEntity.ok(claims);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ExceptionResponse> handleAuthException(AuthException e) {
        log.error("Auth Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
} 