package com.linglevel.api.fcm.controller;

import com.linglevel.api.fcm.dto.PushOpenedRequest;
import com.linglevel.api.fcm.service.PushLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/push-logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Push Logs", description = "푸시 알림 로그 API")
public class PushLogController {

    private final PushLogService pushLogService;

    @PostMapping("/opened")
    @Operation(
            summary = "푸시 알림 오픈 리포트",
            description = "클라이언트에서 사용자가 푸시 알림을 탭하여 열었을 때 서버에 리포트합니다."
    )
    public ResponseEntity<Void> logOpened(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody PushOpenedRequest request) {

        log.debug("Push opened report received - userId: {}, campaignId: {}", userId, request.getCampaignId());

        pushLogService.logOpened(userId, request.getCampaignId(), request.getOpenedAt());

        return ResponseEntity.ok().build();
    }
}
