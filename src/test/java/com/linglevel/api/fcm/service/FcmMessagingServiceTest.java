package com.linglevel.api.fcm.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.linglevel.api.fcm.dto.FcmMessageRequest;
import com.linglevel.api.fcm.repository.FcmTokenRepository;
import com.linglevel.api.fcm.repository.PushLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FcmMessagingServiceTest {

	@Mock
	private FirebaseMessaging firebaseMessaging;

	@Mock
	private FcmTokenRepository fcmTokenRepository;

	@Mock
	private PushLogRepository pushLogRepository;

	@Mock
	private PushLogService pushLogService;

	@InjectMocks
	private FcmMessagingService fcmMessagingService;

	@BeforeEach
	void disableFcm() {
		ReflectionTestUtils.setField(fcmMessagingService, "fcmEnabled", false);
	}

	@Test
	void disabledFcmSkipsSingleMessageDelivery() {
		String pushId = fcmMessagingService.sendMessage("test-token", messageRequest());

		assertThat(pushId).isNotBlank();
		verifyNoInteractions(firebaseMessaging, fcmTokenRepository, pushLogRepository, pushLogService);
	}

	@Test
	void disabledFcmTreatsMulticastAsSuccessfulWithoutDelivery() {
		BatchResponse response = fcmMessagingService.sendMulticastMessage(List.of("token-1", "token-2"),
				messageRequest());

		assertThat(response.getSuccessCount()).isEqualTo(2);
		assertThat(response.getFailureCount()).isZero();
		assertThat(response.getResponses()).isEmpty();
		verifyNoInteractions(firebaseMessaging, fcmTokenRepository, pushLogRepository, pushLogService);
	}

	private FcmMessageRequest messageRequest() {
		return FcmMessageRequest.builder().title("title").body("body").campaignId("test").build();
	}

}
