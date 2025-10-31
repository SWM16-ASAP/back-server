package com.linglevel.api.fcm.service;

import com.linglevel.api.fcm.dto.PushCampaignStats;
import com.linglevel.api.fcm.dto.PushCampaignSummary;
import com.linglevel.api.fcm.entity.PushLog;
import com.linglevel.api.fcm.repository.PushLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushCampaignService {

    private final PushLogRepository pushLogRepository;

    /**
     * 특정 캠페인의 상세 통계 조회
     */
    public PushCampaignStats getStats(String campaignId) {
        List<PushLog> logs = pushLogRepository.findByCampaignId(campaignId);

        if (logs.isEmpty()) {
            return PushCampaignStats.builder()
                    .campaignId(campaignId)
                    .totalSent(0)
                    .sentSuccess(0)
                    .totalOpened(0)
                    .deliveryRate(0.0)
                    .openRate(0.0)
                    .build();
        }

        int totalSent = logs.size();
        int sentSuccess = (int) logs.stream()
                .filter(log -> Boolean.TRUE.equals(log.getSentSuccess()))
                .count();
        int totalOpened = (int) logs.stream()
                .filter(log -> log.getOpenedAt() != null)
                .count();

        double deliveryRate = totalSent > 0 ? (double) sentSuccess / totalSent : 0.0;
        double openRate = sentSuccess > 0 ? (double) totalOpened / sentSuccess : 0.0;

        LocalDateTime firstSentAt = logs.stream()
                .map(PushLog::getSentAt)
                .min(Comparator.naturalOrder())
                .orElse(null);

        return PushCampaignStats.builder()
                .campaignId(campaignId)
                .firstSentAt(firstSentAt)
                .totalSent(totalSent)
                .sentSuccess(sentSuccess)
                .totalOpened(totalOpened)
                .deliveryRate(deliveryRate)
                .openRate(openRate)
                .build();
    }

    /**
     * 캠페인 목록 조회 (기간 필터링 가능)
     */
    public List<PushCampaignSummary> getCampaignSummaries(LocalDateTime startDate, LocalDateTime endDate) {
        List<PushLog> logs;

        if (startDate != null && endDate != null) {
            logs = pushLogRepository.findBySentAtBetween(startDate, endDate);
        } else {
            logs = pushLogRepository.findAll();
        }

        // campaignId로 그룹핑하여 통계 계산
        Map<String, List<PushLog>> logsByCampaign = logs.stream()
                .collect(Collectors.groupingBy(PushLog::getCampaignId));

        return logsByCampaign.entrySet().stream()
                .map(entry -> {
                    String campaignId = entry.getKey();
                    List<PushLog> campaignLogs = entry.getValue();

                    int totalSent = campaignLogs.size();
                    int sentSuccess = (int) campaignLogs.stream()
                            .filter(log -> Boolean.TRUE.equals(log.getSentSuccess()))
                            .count();
                    int totalOpened = (int) campaignLogs.stream()
                            .filter(log -> log.getOpenedAt() != null)
                            .count();

                    double openRate = sentSuccess > 0 ? (double) totalOpened / sentSuccess : 0.0;

                    LocalDateTime firstSentAt = campaignLogs.stream()
                            .map(PushLog::getSentAt)
                            .min(Comparator.naturalOrder())
                            .orElse(null);

                    String campaignType = extractCampaignType(campaignId);

                    return PushCampaignSummary.builder()
                            .campaignId(campaignId)
                            .campaignType(campaignType)
                            .firstSentAt(firstSentAt)
                            .totalSent(totalSent)
                            .sentSuccess(sentSuccess)
                            .totalOpened(totalOpened)
                            .openRate(openRate)
                            .build();
                })
                .sorted(Comparator.comparing(PushCampaignSummary::getFirstSentAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * campaignId에서 타입 추출 (예: "article-tech-123" -> "article")
     */
    private String extractCampaignType(String campaignId) {
        if (campaignId == null) {
            return "unknown";
        }

        int firstDash = campaignId.indexOf('-');
        if (firstDash > 0) {
            return campaignId.substring(0, firstDash);
        }

        return "unknown";
    }
}
