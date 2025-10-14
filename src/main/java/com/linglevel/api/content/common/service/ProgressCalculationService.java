package com.linglevel.api.content.common.service;

import org.springframework.stereotype.Service;

/**
 * 진행률 계산을 위한 공통 서비스
 * V2 Progress 시스템에서 정규화된 진행률(0-100%) 계산 담당
 */
@Service
public class ProgressCalculationService {

    /**
     * 정규화된 진행률 계산 (0-100%)
     *
     * @param currentChunkNumber 현재 청크 번호
     * @param totalChunks        해당 난이도의 전체 청크 개수
     * @return 진행률 (0.0 ~ 100.0)
     */
    public double calculateNormalizedProgress(int currentChunkNumber, long totalChunks) {
        if (totalChunks <= 0) {
            return 0.0;
        }
        return (currentChunkNumber / (double) totalChunks) * 100.0;
    }

    /**
     * max 진행률 업데이트 여부 판단
     *
     * @param currentMax  현재 최대 진행률
     * @param newProgress 새로운 진행률
     * @return 업데이트 필요 여부
     */
    public boolean shouldUpdateMaxProgress(Double currentMax, double newProgress) {
        return currentMax == null || newProgress > currentMax;
    }

    /**
     * 완료 여부 판단 (>= 100%)
     *
     * @param maxProgress 최대 진행률
     * @return 완료 여부
     */
    public boolean isCompleted(Double maxProgress) {
        return maxProgress != null && maxProgress >= 100.0;
    }

    /**
     * isCompleted 플래그 업데이트 (한번 true가 되면 계속 유지)
     *
     * @param currentCompleted 현재 완료 상태
     * @param newCompleted     새로운 완료 상태
     * @return 업데이트된 완료 상태
     */
    public boolean updateCompletedFlag(Boolean currentCompleted, boolean newCompleted) {
        return (currentCompleted != null && currentCompleted) || newCompleted;
    }
}