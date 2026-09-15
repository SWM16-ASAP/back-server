package com.linglevel.api.common.auth;

import com.linglevel.api.auth.jwt.RefreshToken;
import com.linglevel.api.auth.repository.RefreshTokenRepository;
import com.linglevel.api.common.AbstractMysqlTest;
import com.linglevel.api.fcm.entity.FcmPlatform;
import com.linglevel.api.fcm.entity.FcmToken;
import com.linglevel.api.fcm.repository.FcmTokenRepository;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.entity.UserRole;
import com.linglevel.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/mysql,classpath:db/testmigration/mysql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthPersistenceIntegrationTest extends AbstractMysqlTest {

	@Autowired
	RefreshTokenRepository refreshTokens;

	@Autowired
	FcmTokenRepository fcmTokens;

	@Autowired
	UserRepository users;

	@Test
	void refreshTokenRequiresExistingUserAndUniqueTokenId() {
		RefreshToken orphan = RefreshToken.builder()
			.tokenId(UUID.randomUUID().toString())
			.userId(Long.MAX_VALUE)
			.expiresAt(LocalDateTime.now().plusDays(1))
			.build();
		assertThatThrownBy(() -> refreshTokens.saveAndFlush(orphan))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void duplicateRefreshTokenIdIsRejected() {
		User user = users.saveAndFlush(User.builder().username("reader").role(UserRole.USER).build());
		String tokenId = UUID.randomUUID().toString();
		refreshTokens.saveAndFlush(token(user.getId(), tokenId, LocalDateTime.now().plusDays(1)));
		RefreshToken duplicate = token(user.getId(), tokenId, LocalDateTime.now().plusDays(1));
		assertThatThrownBy(() -> refreshTokens.saveAndFlush(duplicate))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void expiredRefreshTokensAreDeletedByCleanupQuery() {
		User user = users.saveAndFlush(User.builder().username("reader2").role(UserRole.USER).build());
		RefreshToken expired = refreshTokens
			.saveAndFlush(token(user.getId(), UUID.randomUUID().toString(), LocalDateTime.now().minusDays(1)));
		RefreshToken valid = refreshTokens
			.saveAndFlush(token(user.getId(), UUID.randomUUID().toString(), LocalDateTime.now().plusDays(1)));

		long deleted = refreshTokens.deleteByExpiresAtBefore(LocalDateTime.now());

		assertThat(deleted).isEqualTo(1);
		assertThat(refreshTokens.findById(expired.getId())).isEmpty();
		assertThat(refreshTokens.findById(valid.getId())).isPresent();
	}

	@Test
	void duplicateFcmTokenValueIsRejected() {
		User user = users.saveAndFlush(User.builder().username("reader3").role(UserRole.USER).build());
		fcmTokens.saveAndFlush(fcmToken(user.getId(), "device-1", "same-token"));
		FcmToken duplicate = fcmToken(user.getId(), "device-2", "same-token");
		assertThatThrownBy(() -> fcmTokens.saveAndFlush(duplicate))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void staleInactiveFcmTokensAreDeletedByCleanupQueryButActiveOnesAreKept() {
		User user = users.saveAndFlush(User.builder().username("reader4").role(UserRole.USER).build());
		FcmToken staleInactive = fcmToken(user.getId(), "device-old", "old-token");
		staleInactive.setUpdatedAt(LocalDateTime.now().minusDays(91));
		staleInactive.setIsActive(false);
		fcmTokens.saveAndFlush(staleInactive);
		FcmToken staleButActive = fcmToken(user.getId(), "device-still-logged-in", "still-active-token");
		staleButActive.setUpdatedAt(LocalDateTime.now().minusDays(91));
		fcmTokens.saveAndFlush(staleButActive);
		FcmToken fresh = fcmToken(user.getId(), "device-new", "new-token");
		fcmTokens.saveAndFlush(fresh);

		long deleted = fcmTokens.deleteByUpdatedAtBeforeAndIsActive(LocalDateTime.now().minusDays(90), false);

		assertThat(deleted).isEqualTo(1);
		assertThat(fcmTokens.findById(staleInactive.getId())).isEmpty();
		assertThat(fcmTokens.findById(staleButActive.getId())).isPresent();
		assertThat(fcmTokens.findById(fresh.getId())).isPresent();
	}

	private RefreshToken token(Long userId, String tokenId, LocalDateTime expiresAt) {
		return RefreshToken.builder().tokenId(tokenId).userId(userId).expiresAt(expiresAt).build();
	}

	private FcmToken fcmToken(Long userId, String deviceId, String fcmTokenValue) {
		LocalDateTime now = LocalDateTime.now();
		return FcmToken.builder()
			.userId(userId)
			.deviceId(deviceId)
			.fcmToken(fcmTokenValue)
			.platform(FcmPlatform.ANDROID)
			.createdAt(now)
			.updatedAt(now)
			.isActive(true)
			.build();
	}

}
