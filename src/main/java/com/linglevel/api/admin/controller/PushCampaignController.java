package com.linglevel.api.admin.controller;

import com.linglevel.api.fcm.dto.PushCampaignStats;
import com.linglevel.api.fcm.dto.PushCampaignSummary;
import com.linglevel.api.fcm.service.PushCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/push-campaigns")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Push Campaigns", description = "어드민 전용 푸시 캠페인 통계 API")
public class PushCampaignController {

    private final PushCampaignService pushCampaignService;

    @GetMapping
    @Operation(
            summary = "캠페인 목록 조회",
            description = "푸시 캠페인 목록을 조회합니다. 기간 필터링이 가능합니다."
    )
    public ResponseEntity<List<PushCampaignSummary>> getCampaigns(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate) {

        log.debug("Get campaigns request - startDate: {}, endDate: {}", startDate, endDate);

        List<PushCampaignSummary> summaries = pushCampaignService.getCampaignSummaries(startDate, endDate);

        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/{campaignId}/stats")
    @Operation(
            summary = "캠페인 상세 통계 조회",
            description = "특정 캠페인의 상세 통계를 조회합니다."
    )
    public ResponseEntity<PushCampaignStats> getCampaignStats(@PathVariable String campaignId) {
        log.debug("Get campaign stats request - campaignId: {}", campaignId);

        PushCampaignStats stats = pushCampaignService.getStats(campaignId);

        return ResponseEntity.ok(stats);
    }
}
