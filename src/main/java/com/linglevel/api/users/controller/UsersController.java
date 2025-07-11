package com.linglevel.api.users.controller;

import com.linglevel.api.common.dto.ExceptionResponseDTO;
import com.linglevel.api.common.dto.PageResponseDTO;
import com.linglevel.api.users.dto.GetMyBooksProgressRequest;
import com.linglevel.api.users.dto.UserBooksProgressResponse;
import com.linglevel.api.users.exception.UsersException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "사용자 관련 API")
public class UsersController {

    @Operation(summary = "나의 책 읽기 진도 조회", description = "현재 사용자의 모든 책에 대한 읽기 진도를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class)))
    })
    @GetMapping("/me/progress/books")
    public ResponseEntity<PageResponseDTO<UserBooksProgressResponse>> getMyBookProgress(
            @ParameterObject @ModelAttribute GetMyBooksProgressRequest request) {
        // TODO: 사용자 읽기 진도 조회 로직 구현
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @ExceptionHandler(UsersException.class)
    public ResponseEntity<ExceptionResponseDTO> handleUsersException(UsersException e) {
        log.error("Users Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponseDTO(e));
    }
} 