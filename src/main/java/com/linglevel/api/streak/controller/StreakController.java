package com.linglevel.api.streak.controller;

import com.linglevel.api.auth.jwt.JwtClaims;
import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.streak.dto.CalendarResponse;
import com.linglevel.api.streak.dto.FreezeTransactionResponse;
import com.linglevel.api.streak.dto.StreakResponse;
import com.linglevel.api.streak.dto.WeekStreakResponse;
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
import org.springframework.data.domain.Page;
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
        )
    })
    public ResponseEntity<StreakResponse> getMyStreak(
            @AuthenticationPrincipal JwtClaims claims) {

        StreakResponse response = streakService.getStreakInfo(claims.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/freeze-transactions")
    @Operation(
        summary = "내 프리즈 내역 조회",
        description = "현재 로그인한 사용자의 프리즈 획득 및 사용 내역을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "프리즈 내역 조회 성공",
            useReturnTypeSchema = true
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))
        )
    })
    public ResponseEntity<Page<FreezeTransactionResponse>> getMyFreezeTransactions(
            @AuthenticationPrincipal JwtClaims claims,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        Page<FreezeTransactionResponse> response = streakService.getFreezeTransactions(claims.getId(), page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/calendar")
    @Operation(
        summary = "달력 조회",
        description = "특정 년월의 달력 정보를 조회합니다. 각 날짜별 스트릭 상태, 완료한 학습 개수, 보상 정보를 포함합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "달력 조회 성공",
            useReturnTypeSchema = true
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))
        )
    })
    public ResponseEntity<CalendarResponse> getCalendar(
            @AuthenticationPrincipal JwtClaims claims,
            @RequestParam @Schema(description = "년도", example = "2025") int year,
            @RequestParam @Schema(description = "월", example = "10") int month) {

        CalendarResponse response = streakService.getCalendar(claims.getId(), year, month);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/this-week")
    @Operation(
        summary = "이번 주 스트릭 조회",
        description = "이번 주의 스트릭 정보를 조회합니다. 월요일부터 일요일까지의 스트릭 상태와 보상 정보를 포함합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "주간 스트릭 조회 성공",
            useReturnTypeSchema = true
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))
        )
    })
    public ResponseEntity<WeekStreakResponse> getThisWeekStreak(
            @AuthenticationPrincipal JwtClaims claims) {

        WeekStreakResponse response = streakService.getThisWeekStreak(claims.getId());
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(StreakException.class)
    public ResponseEntity<ExceptionResponse> handleStreakException(StreakException e) {
        log.error("Streak Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}