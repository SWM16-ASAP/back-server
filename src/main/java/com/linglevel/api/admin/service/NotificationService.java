package com.linglevel.api.admin.service;

import com.google.firebase.messaging.*;
import com.linglevel.api.admin.dto.ArticleReleaseNotificationRequest;
import com.linglevel.api.admin.dto.ArticleReleaseNotificationResponse;
import com.linglevel.api.admin.dto.NotificationBroadcastRequest;
import com.linglevel.api.admin.dto.NotificationBroadcastResponse;
import com.linglevel.api.admin.dto.NotificationSendResponse;
import com.linglevel.api.admin.dto.NotificationSendRequest;
import com.linglevel.api.content.article.entity.Article;
import com.linglevel.api.content.article.repository.ArticleRepository;
import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.recommendation.entity.UserCategoryPreference;
import com.linglevel.api.content.recommendation.repository.UserCategoryPreferenceRepository;
import com.linglevel.api.fcm.dto.FcmMessageRequest;
import com.linglevel.api.fcm.entity.FcmToken;
import com.linglevel.api.fcm.repository.FcmTokenRepository;
import com.linglevel.api.fcm.service.FcmMessagingService;
import com.linglevel.api.i18n.CountryCode;
import com.linglevel.api.i18n.LanguageCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

	private final FcmMessagingService fcmMessagingService;

	private final FcmTokenRepository fcmTokenRepository;

	private final ArticleRepository articleRepository;

	private final UserCategoryPreferenceRepository userCategoryPreferenceRepository;

	public NotificationSendResponse sendNotificationFromRequest(NotificationSendRequest request) {
		return sendLocalizedNotification(request.getTargets(), request.getMessages(), request.getData());
	}

	private void deactivateTokenByFcmToken(String fcmToken) {
		try {
			Optional<FcmToken> tokenEntity = fcmTokenRepository.findByFcmToken(fcmToken);
			if (tokenEntity.isPresent()) {
				FcmToken token = tokenEntity.get();
				token.setIsActive(false);
				token.setUpdatedAt(LocalDateTime.now());
				fcmTokenRepository.save(token);
				log.info("Deactivated invalid FCM token for user: {}, device: {}", token.getUserId(),
						token.getDeviceId());
			}
			else {
				log.warn("FCM token not found in database: {}", maskToken(fcmToken));
			}
		}
		catch (Exception e) {
			log.error("Failed to deactivate token: {}", maskToken(fcmToken), e);
		}
	}

	private String maskToken(String token) {
		if (token == null || token.length() < 8) {
			return "***";
		}
		return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
	}

	/**
	 * 국가별 메시지를 전송합니다.
	 */
	private NotificationSendResponse sendLocalizedNotification(List<String> targetUserIds,
			Map<String, NotificationSendRequest.LocalizedMessage> messages, Map<String, String> data) {

		log.info("Starting localized notification send to {} users", targetUserIds.size());

		// 대상 사용자들의 활성 FCM 토큰 조회
		List<FcmToken> allTokens = new ArrayList<>();
		for (String userId : targetUserIds) {
			List<FcmToken> activeTokens = fcmTokenRepository.findByUserIdAndIsActive(userId, true);
			allTokens.addAll(activeTokens);
		}

		if (allTokens.isEmpty()) {
			log.warn("No FCM tokens found for users: {}", targetUserIds);
			return new NotificationSendResponse("No FCM tokens found for users.", 0, 0,
					new NotificationSendResponse.NotificationSendDetails(Collections.emptyList(),
							Collections.emptyList()));
		}

		// 국가별로 토큰 그룹핑
		Map<CountryCode, List<FcmToken>> tokensByCountry = allTokens.stream()
			.collect(Collectors
				.groupingBy(token -> token.getCountryCode() != null ? token.getCountryCode() : CountryCode.US));

		List<String> sentTokens = new ArrayList<>();
		List<String> failedTokens = new ArrayList<>();

		// 국가별로 메시지 전송
		tokensByCountry.forEach((countryCode, tokens) -> {
			NotificationSendRequest.LocalizedMessage message = messages.get(countryCode.getCode());

			// 해당 국가 메시지가 없으면 US 기본값 사용
			if (message == null) {
				message = messages.get("US");
			}

			// US 메시지도 없으면 스킵
			if (message == null) {
				log.warn("No message found for country: {} and no fallback (US) message", countryCode);
				tokens.forEach(token -> failedTokens.add(token.getFcmToken()));
				return;
			}

			FcmMessageRequest fcmRequest = FcmMessageRequest.builder()
				.title(message.getTitle())
				.body(message.getBody())
				.campaignId("admin-targeted")
				.data(data)
				.build();

			List<String> fcmTokens = tokens.stream().map(FcmToken::getFcmToken).collect(Collectors.toList());

			try {
				if (fcmTokens.size() == 1) {
					fcmMessagingService.sendMessage(fcmTokens.get(0), fcmRequest);
					sentTokens.add(fcmTokens.get(0));
					log.debug("Sent localized message (country: {}) to token: {}", countryCode,
							maskToken(fcmTokens.get(0)));
				}
				else if (!fcmTokens.isEmpty()) {
					BatchResponse response = fcmMessagingService.sendMulticastMessage(fcmTokens, fcmRequest);

					// 개별 응답 처리
					for (int i = 0; i < response.getResponses().size(); i++) {
						String token = fcmTokens.get(i);
						if (response.getResponses().get(i).isSuccessful()) {
							sentTokens.add(token);
						}
						else {
							failedTokens.add(token);
							log.warn("Failed to send localized message to token: {}, error: {}", maskToken(token),
									response.getResponses().get(i).getException().getMessage());
							deactivateTokenByFcmToken(token);
						}
					}
					log.debug("Sent multicast message (country: {}) - Success: {}, Failed: {}", countryCode,
							response.getSuccessCount(), response.getFailureCount());
				}
			}
			catch (Exception e) {
				log.error("Failed to send localized message batch (country: {}), error: {}", countryCode,
						e.getMessage());
				for (String token : fcmTokens) {
					failedTokens.add(token);
					if (e instanceof com.linglevel.api.fcm.exception.FcmException) {
						deactivateTokenByFcmToken(token);
					}
				}
			}
		});

		log.info("Localized notification send completed - Success: {}, Failed: {}", sentTokens.size(),
				failedTokens.size());

		return new NotificationSendResponse("Localized notification sent successfully.", sentTokens.size(),
				failedTokens.size(), new NotificationSendResponse.NotificationSendDetails(sentTokens, failedTokens));
	}

	public NotificationBroadcastResponse sendBroadcastNotification(NotificationBroadcastRequest request) {
		return sendLocalizedBroadcast(request.getMessages(), request.getData());
	}

	/**
	 * 국가별 메시지 브로드캐스트
	 */
	private NotificationBroadcastResponse sendLocalizedBroadcast(
			Map<String, NotificationBroadcastRequest.LocalizedMessage> messages, Map<String, String> data) {

		log.info("Starting localized broadcast notification");

		// 모든 활성 FCM 토큰 조회
		List<FcmToken> allActiveTokens = fcmTokenRepository.findByIsActive(true);

		if (allActiveTokens.isEmpty()) {
			log.warn("No FCM tokens found for broadcast");
			return new NotificationBroadcastResponse("No FCM tokens found for broadcast.", 0, 0, 0,
					new NotificationBroadcastResponse.NotificationBroadcastDetails(0, 0, 0));
		}

		// 국가별로 토큰 그룹핑
		Map<CountryCode, List<FcmToken>> tokensByCountry = allActiveTokens.stream()
			.collect(Collectors
				.groupingBy(token -> token.getCountryCode() != null ? token.getCountryCode() : CountryCode.US));

		int totalTokens = allActiveTokens.size();
		int totalSentCount = 0;
		int totalFailedCount = 0;
		Set<String> successfulUserIds = new HashSet<>();
		Set<String> failedUserIds = new HashSet<>();

		// 국가별로 메시지 전송
		for (Map.Entry<CountryCode, List<FcmToken>> entry : tokensByCountry.entrySet()) {
			CountryCode countryCode = entry.getKey();
			List<FcmToken> tokens = entry.getValue();

			NotificationBroadcastRequest.LocalizedMessage message = messages.get(countryCode.getCode());

			// 해당 국가 메시지가 없으면 US 기본값 사용
			if (message == null) {
				message = messages.get("US");
			}

			// US 메시지도 없으면 스킵
			if (message == null) {
				log.warn("No message found for country: {} and no fallback (US) message", countryCode);
				for (FcmToken token : tokens) {
					failedUserIds.add(token.getUserId());
					totalFailedCount++;
				}
				continue;
			}

			FcmMessageRequest fcmRequest = FcmMessageRequest.builder()
				.title(message.getTitle())
				.body(message.getBody())
				.campaignId("admin-broadcast")
				.data(data)
				.build();

			List<String> fcmTokens = tokens.stream().map(FcmToken::getFcmToken).collect(Collectors.toList());

			Map<String, String> tokenToUserId = tokens.stream()
				.collect(Collectors.toMap(FcmToken::getFcmToken, FcmToken::getUserId, (a, b) -> a));

			try {
				if (fcmTokens.size() == 1) {
					fcmMessagingService.sendMessage(fcmTokens.get(0), fcmRequest);
					successfulUserIds.add(tokenToUserId.get(fcmTokens.get(0)));
					totalSentCount++;
					log.debug("Broadcast localized message (country: {}) sent to user: {}, token: {}", countryCode,
							tokenToUserId.get(fcmTokens.get(0)), maskToken(fcmTokens.get(0)));
				}
				else if (!fcmTokens.isEmpty()) {
					BatchResponse response = fcmMessagingService.sendMulticastMessage(fcmTokens, fcmRequest);

					// 개별 응답 처리
					for (int i = 0; i < response.getResponses().size(); i++) {
						String token = fcmTokens.get(i);
						String userId = tokenToUserId.get(token);

						if (response.getResponses().get(i).isSuccessful()) {
							successfulUserIds.add(userId);
							totalSentCount++;
						}
						else {
							failedUserIds.add(userId);
							totalFailedCount++;
							log.warn("Failed to send localized broadcast to user: {}, token: {}, error: {}", userId,
									maskToken(token), response.getResponses().get(i).getException().getMessage());
							deactivateTokenByFcmToken(token);
						}
					}
					log.debug("Broadcast multicast message (country: {}) - Success: {}, Failed: {}", countryCode,
							response.getSuccessCount(), response.getFailureCount());
				}
			}
			catch (Exception e) {
				log.error("Failed to send broadcast message batch (country: {}), error: {}", countryCode,
						e.getMessage());
				for (String token : fcmTokens) {
					failedUserIds.add(tokenToUserId.get(token));
					totalFailedCount++;
					if (e instanceof com.linglevel.api.fcm.exception.FcmException) {
						deactivateTokenByFcmToken(token);
					}
				}
			}
		}

		// 실패만 한 사용자 계산
		int failedOnlyUsers = (int) failedUserIds.stream()
			.filter(userId -> !successfulUserIds.contains(userId))
			.count();

		int totalUsers = (int) allActiveTokens.stream().map(FcmToken::getUserId).distinct().count();

		log.info(
				"Localized broadcast completed - Total users: {}, Successful users: {}, Failed users: {}, "
						+ "Total sent: {}, Total failed: {}",
				totalUsers, successfulUserIds.size(), failedOnlyUsers, totalSentCount, totalFailedCount);

		return new NotificationBroadcastResponse("Localized broadcast notification sent successfully.", totalUsers,
				totalSentCount, totalFailedCount, new NotificationBroadcastResponse.NotificationBroadcastDetails(
						successfulUserIds.size(), failedOnlyUsers, totalTokens));
	}

	/**
	 * 아티클 출시 알림 전송
	 */
	public ArticleReleaseNotificationResponse sendArticleReleaseNotification(
			ArticleReleaseNotificationRequest request) {
		log.info("Starting article release notification for {} articles", request.getArticles().size());

		int totalSentCount = 0;
		List<ArticleReleaseNotificationResponse.ArticleResult> results = new ArrayList<>();
		Map<String, List<MatchedArticle>> userArticleMatches = new HashMap<>();

		// 1. 각 아티클별로 타겟 사용자 필터링
		for (ArticleReleaseNotificationRequest.ArticleInfo articleInfo : request.getArticles()) {
			List<FcmToken> targetTokens = filterTargetTokens(articleInfo);

			log.info("Article {} matched {} tokens", articleInfo.getArticleId(), targetTokens.size());

			// 각 토큰의 사용자에 대해 매칭 정보 저장
			for (FcmToken token : targetTokens) {
				String userId = token.getUserId();
				LanguageCode userLanguage = convertCountryCodeToLanguageCode(token.getCountryCode());

				int priority = calculatePriority(token, articleInfo, userLanguage);

				MatchedArticle matchedArticle = new MatchedArticle(articleInfo.getArticleId(),
						articleInfo.getTargetCategoryEnum(), priority, userLanguage);

				userArticleMatches.computeIfAbsent(userId, k -> new ArrayList<>()).add(matchedArticle);
			}
		}

		// 2. 각 사용자별로 최고 우선순위 아티클 1개만 선택하여 알림 전송
		Map<String, Integer> articleSentCounts = new HashMap<>();
		Map<String, Integer> articleTargetCounts = new HashMap<>();

		for (Map.Entry<String, List<MatchedArticle>> entry : userArticleMatches.entrySet()) {
			String userId = entry.getKey();
			List<MatchedArticle> matches = entry.getValue();

			// 우선순위가 가장 높은 아티클 선택 (priority 값이 낮을수록 우선순위 높음)
			MatchedArticle topMatch = matches.stream()
				.min(Comparator.comparingInt(MatchedArticle::getPriority))
				.orElse(null);

			if (topMatch != null) {
				String articleId = topMatch.getArticleId();

				articleTargetCounts.merge(articleId, 1, Integer::sum);

				List<FcmToken> userTokens = fcmTokenRepository.findByUserIdAndIsActive(userId, true);

				if (!userTokens.isEmpty()) {
					Optional<Article> articleOpt = articleRepository.findById(articleId);
					if (articleOpt.isPresent()) {
						Article article = articleOpt.get();

						String localizedTitle = getLocalizedNotificationTitle(topMatch.getUserLanguage());
						String categoryName = article.getCategory() != null ? article.getCategory().name().toLowerCase()
								: "unknown";
						String campaignId = "newArticle-" + categoryName;

						FcmMessageRequest fcmRequest = FcmMessageRequest.builder()
							.title(localizedTitle)
							.body(article.getTitle())
							.type("ARTICLE_RELEASE")
							.deepLink("linglevel:///articles/" + article.getId())
							.campaignId(campaignId)
							.build();

						Map<String, String> additionalData = new HashMap<>();
						additionalData.put("articleId", article.getId());
						fcmRequest.setAdditionalData(additionalData);

						List<String> fcmTokens = userTokens.stream()
							.map(FcmToken::getFcmToken)
							.collect(Collectors.toList());

						boolean sent = false;

						try {
							if (fcmTokens.size() == 1) {
								fcmMessagingService.sendMessage(fcmTokens.get(0), fcmRequest);
								sent = true;
								log.debug("Sent article notification to user: {}, article: {}", userId, articleId);
							}
							else if (!fcmTokens.isEmpty()) {
								BatchResponse response = fcmMessagingService.sendMulticastMessage(fcmTokens,
										fcmRequest);

								// 개별 응답 처리
								for (int i = 0; i < response.getResponses().size(); i++) {
									String token = fcmTokens.get(i);
									if (response.getResponses().get(i).isSuccessful()) {
										sent = true;
									}
									else {
										log.warn(
												"Failed to send article notification to user: {}, token: {}, error: {}",
												userId, maskToken(token),
												response.getResponses().get(i).getException().getMessage());
										deactivateTokenByFcmToken(token);
									}
								}

								if (sent) {
									log.debug(
											"Sent article notification multicast to user: {} (Success: {}, Failed: {}), article: {}",
											userId, response.getSuccessCount(), response.getFailureCount(), articleId);
								}
							}
						}
						catch (Exception e) {
							log.error("Failed to send article notification to user: {}, article: {}, error: {}", userId,
									articleId, e.getMessage());
							for (String token : fcmTokens) {
								if (e instanceof com.linglevel.api.fcm.exception.FcmException) {
									deactivateTokenByFcmToken(token);
								}
							}
						}

						if (sent) {
							articleSentCounts.merge(articleId, 1, Integer::sum);
							totalSentCount++;
						}
					}
				}
			}
		}

		// 3. 응답 생성
		for (ArticleReleaseNotificationRequest.ArticleInfo articleInfo : request.getArticles()) {
			String articleId = articleInfo.getArticleId();
			ArticleReleaseNotificationResponse.ArticleResult result = ArticleReleaseNotificationResponse.ArticleResult
				.builder()
				.articleId(articleId)
				.sentCount(articleSentCounts.getOrDefault(articleId, 0))
				.targetUserCount(articleTargetCounts.getOrDefault(articleId, 0))
				.build();
			results.add(result);
		}

		log.info("Article release notification completed - Total sent: {}", totalSentCount);

		return ArticleReleaseNotificationResponse.builder().totalSentCount(totalSentCount).results(results).build();
	}

	/**
	 * 타겟 토큰 필터링
	 */
	private List<FcmToken> filterTargetTokens(ArticleReleaseNotificationRequest.ArticleInfo articleInfo) {
		List<FcmToken> allActiveTokens = fcmTokenRepository.findByIsActive(true);

		return allActiveTokens.stream().filter(token -> {
			LanguageCode userLanguage = convertCountryCodeToLanguageCode(token.getCountryCode());

			// targetLanguageCodes가 null이면 모든 언어 매칭
			if (articleInfo.getTargetLanguageCodes() != null && !articleInfo.getTargetLanguageCodes().isEmpty()) {
				if (!articleInfo.getTargetLanguageCodes().contains(userLanguage)) {
					return false;
				}
			}

			return true;
		}).collect(Collectors.toList());
	}

	/**
	 * 우선순위 계산 Priority 1 (값: 1): 언어 AND 카테고리 모두 매칭 Priority 2 (값: 2): 언어만 매칭
	 */
	private int calculatePriority(FcmToken token, ArticleReleaseNotificationRequest.ArticleInfo articleInfo,
			LanguageCode userLanguage) {
		Optional<UserCategoryPreference> preferenceOpt = userCategoryPreferenceRepository
			.findByUserId(token.getUserId());

		boolean categoryMatch = false;
		if (preferenceOpt.isPresent() && preferenceOpt.get().getPrimaryCategory() != null) {
			ContentCategory targetCategory = articleInfo.getTargetCategoryEnum();
			categoryMatch = preferenceOpt.get().getPrimaryCategory().equals(targetCategory);
		}
		else {
			categoryMatch = true;
		}

		// 언어는 이미 filterTargetTokens에서 필터링되었으므로 항상 매칭됨
		boolean languageMatch = true;

		if (languageMatch && categoryMatch) {
			return 1;
		}
		else if (languageMatch) {
			return 2;
		}
		else {
			return 999;
		}
	}

	/**
	 * CountryCode를 LanguageCode로 변환
	 */
	private LanguageCode convertCountryCodeToLanguageCode(CountryCode countryCode) {
		if (countryCode == null) {
			return LanguageCode.EN;
		}

		switch (countryCode) {
			case KR:
				return LanguageCode.KO;
			case JP:
				return LanguageCode.JA;
			case US:
			default:
				return LanguageCode.EN;
		}
	}

	/**
	 * 아티클 알림 전송
	 */
	private void sendArticleNotification(FcmToken token, Article article, LanguageCode userLanguage) {
		String localizedTitle = getLocalizedNotificationTitle(userLanguage);

		// campaignId 생성: "newArticle-{category}"
		String categoryName = article.getCategory() != null ? article.getCategory().name().toLowerCase() : "unknown";
		String campaignId = "newArticle-" + categoryName;

		FcmMessageRequest fcmRequest = FcmMessageRequest.builder()
			.title(localizedTitle)
			.body(article.getTitle())
			.type("ARTICLE_RELEASE")
			.deepLink("linglevel:///articles/" + article.getId())
			.campaignId(campaignId)
			.build();

		Map<String, String> additionalData = new HashMap<>();
		additionalData.put("articleId", article.getId());
		fcmRequest.setAdditionalData(additionalData);

		fcmMessagingService.sendMessage(token.getFcmToken(), fcmRequest);
	}

	/**
	 * 언어별 알림 제목 로컬라이징
	 */
	private String getLocalizedNotificationTitle(LanguageCode languageCode) {
		switch (languageCode) {
			case KO:
				return "💌 오늘의 아티클 도착";
			case JA:
				return "💌 本日の記事が届きました";
			case EN:
			default:
				return "💌 Today's Article Has Arrived";
		}
	}

	/**
	 * 매칭된 아티클 정보를 담는 내부 클래스
	 */
	@Getter
	private static class MatchedArticle {

		private final String articleId;

		private final ContentCategory category;

		private final int priority;

		private final LanguageCode userLanguage;

		public MatchedArticle(String articleId, ContentCategory category, int priority, LanguageCode userLanguage) {
			this.articleId = articleId;
			this.category = category;
			this.priority = priority;
			this.userLanguage = userLanguage;
		}

	}

}