package com.linglevel.api.user.controller;

import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.common.dto.MessageResponse;
import com.linglevel.api.user.exception.UsersException;
import com.linglevel.api.user.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "사용자 관련 API")
public class UsersController {
    
    private final UsersService usersService;

    @DeleteMapping("/me")
    @Operation(summary = "사용자 계정 삭제", description = "현재 인증된 사용자의 계정을 삭제합니다. JWT 토큰을 통해 사용자를 식별하며, 관련된 모든 사용자 데이터가 삭제됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "계정 삭제 성공",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public ResponseEntity<MessageResponse> deleteUser(Authentication authentication) {
        String username = authentication.getName();
        usersService.deleteUser(username);
        return ResponseEntity.ok(new MessageResponse("User account deleted successfully."));
    }

    @ExceptionHandler(UsersException.class)
    public ResponseEntity<ExceptionResponse> handleUsersException(UsersException e) {
        log.error("Users Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
} 