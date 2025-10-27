package com.linglevel.api.streak.controller;

import com.linglevel.api.auth.jwt.JwtClaims;
import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.streak.dto.StreakResponse;
import com.linglevel.api.streak.exception.StreakException;
import com.linglevel.api.streak.service.StreakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/streaks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Streaks", description = "스트릭 관련 API")
public class StreakController {

    private final StreakService streakService;

    @GetMapping("/me")
    @Operation(
        summary = "내 스트릭 정보 조회",
        description = "현재 로그인한 사용자의 스트릭 정보를 조회합니다. " +
                     "현재 연속 일수, 총 학습 시간, 상위 몇%, 격려 메시지 등을 포함합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "스트릭 정보 조회 성공",
            useReturnTypeSchema = true
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "스트릭 기록을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))
        )
    })
    public ResponseEntity<StreakResponse> getMyStreak(
            @AuthenticationPrincipal JwtClaims claims) {

        // TODO: Service 구현 완료 후 주석 해제
        // StreakResponse response = streakService.getStreakInfo(claims.getId());
        // return ResponseEntity.ok(response);

        // Mock response for Swagger documentation (Service 구현 전 임시)
        StreakResponse mockResponse = StreakResponse.builder()
                .currentStreak(0)
                .todayCompleted(false)
                .todayStatus(com.linglevel.api.streak.entity.StreakStatus.MISSED)
                .longestStreak(0)
                .streakStartDate(null)
                .totalStudyDays(0L)
                .totalContentsRead(0L)
                .freezeCount(0)
                .totalReadingTimeSeconds(0L)
                .percentileRank(0)
                .build();

        return ResponseEntity.ok(mockResponse);
    }

    @ExceptionHandler(StreakException.class)
    public ResponseEntity<ExceptionResponse> handleStreakException(StreakException e) {
        log.error("Streak Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}
// This is Comment. Can write comment by double slash.
/* Or by this way */