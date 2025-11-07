package com.linglevel.api.content.feed.service;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.feed.entity.Feed;
import com.linglevel.api.content.recommendation.entity.UserCategoryPreference;
import com.linglevel.api.content.recommendation.repository.UserCategoryPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Feed 추천 서비스
 *
 * 사용자의 카테고리 선호도를 기반으로 Feed 추천 점수를 계산합니다.
 * - 카테고리 매칭: 80%
 * - 품질 (평균 읽기 시간): 10%
 * - 신선도 (발행 시간): 10%
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedRecommendationService {

    private final UserCategoryPreferenceRepository userCategoryPreferenceRepository;

    // 가중치 상수
    private static final double CATEGORY_WEIGHT = 0.8;  // 80%
    private static final double QUALITY_WEIGHT = 0.1;   // 10%
    private static final double FRESHNESS_WEIGHT = 0.1; // 10%

    /**
     * Feed 목록을 추천 점수순으로 정렬
     */
    public List<Feed> sortByRecommendation(List<Feed> feeds, String userId) {
        // 사용자 선호도 조회
        Optional<UserCategoryPreference> preferenceOpt = userCategoryPreferenceRepository.findByUserId(userId);

        if (preferenceOpt.isEmpty() || preferenceOpt.get().getCategoryScores() == null) {
            // 선호도 없으면 최신순 정렬
            return feeds.stream()
                    .sorted(Comparator.comparing(
                            Feed::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())
                    ))
                    .collect(Collectors.toList());
        }

        UserCategoryPreference preference = preferenceOpt.get();
        Map<ContentCategory, Double> categoryScores = preference.getCategoryScores();

        // Feed별 추천 점수 계산 및 정렬
        return feeds.stream()
                .map(feed -> new FeedWithScore(feed, calculateScore(feed, categoryScores)))
                .sorted(Comparator.comparingDouble(FeedWithScore::getScore).reversed())
                .map(FeedWithScore::getFeed)
                .collect(Collectors.toList());
    }

    /**
     * Feed의 추천 점수 계산
     */
    private double calculateScore(Feed feed, Map<ContentCategory, Double> categoryScores) {
        double categoryScore = calculateCategoryScore(feed, categoryScores);
        double qualityScore = calculateQualityScore(feed);
        double freshnessScore = calculateFreshnessScore(feed);

        return (categoryScore * CATEGORY_WEIGHT)
             + (qualityScore * QUALITY_WEIGHT)
             + (freshnessScore * FRESHNESS_WEIGHT);
    }

    /**
     * 카테고리 매칭 점수 계산 (0.0 ~ 1.0)
     */
    private double calculateCategoryScore(Feed feed, Map<ContentCategory, Double> categoryScores) {
        if (feed.getCategory() == null) {
            return 0.0; // 카테고리 없으면 0점
        }

        Double score = categoryScores.get(feed.getCategory());
        return score != null ? score : 0.0;
    }

    /**
     * 품질 점수 계산 (0.0 ~ 1.0)
     * avgReadTimeSeconds를 기반으로 계산
     */
    private double calculateQualityScore(Feed feed) {
        if (feed.getAvgReadTimeSeconds() == null || feed.getAvgReadTimeSeconds() <= 0) {
            return 0.5; // 중립적 점수
        }

        double avgReadTime = feed.getAvgReadTimeSeconds();

        // 읽기 시간 구간별 점수
        if (avgReadTime < 15) {
            return 0.2; // 15초 미만: 낮은 품질
        } else if (avgReadTime < 30) {  // 30초
            return 0.5; // 짧게 읽힘
        } else if (avgReadTime < 60) {  // 1분
            return 0.8; // 적절한 길이
        } else {
            return 1.0; // 1분 이상: 높은 품질 (깊이 있는 콘텐츠)
        }
    }

    /**
     * 신선도 점수 계산 (0.0 ~ 1.0)
     * 최근 발행일수록 높은 점수
     */
    private double calculateFreshnessScore(Feed feed) {
        if (feed.getPublishedAt() == null) {
            return 0.5; // 중립적 점수
        }

        Instant now = Instant.now();
        Instant publishedAt = feed.getPublishedAt();
        long daysSincePublished = java.time.Duration.between(publishedAt, now).toDays();

        // 발행 기간별 점수
        if (daysSincePublished < 1) {
            return 1.0; // 1일 이내: 매우 신선
        } else if (daysSincePublished < 7) {
            return 0.8; // 1주일 이내: 신선
        } else if (daysSincePublished < 30) {
            return 0.5; // 1개월 이내: 보통
        } else if (daysSincePublished < 90) {
            return 0.3; // 3개월 이내: 오래됨
        } else {
            return 0.1; // 3개월 이상: 매우 오래됨
        }
    }

    /**
     * Feed와 점수를 함께 저장하는 내부 클래스
     */
    private static class FeedWithScore {
        private final Feed feed;
        private final double score;

        public FeedWithScore(Feed feed, double score) {
            this.feed = feed;
            this.score = score;
        }

        public Feed getFeed() {
            return feed;
        }

        public double getScore() {
            return score;
        }
    }
}
