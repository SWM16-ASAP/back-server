package com.linglevel.api.fcm.repository;

import com.linglevel.api.fcm.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

	Optional<FcmToken> findByUserIdAndDeviceId(Long userId, String deviceId);

	default Optional<FcmToken> findByUserIdAndDeviceId(String userId, String deviceId) {
		return findByUserIdAndDeviceId(Long.valueOf(userId), deviceId);
	}

	List<FcmToken> findByUserId(Long userId);

	default List<FcmToken> findByUserId(String userId) {
		return findByUserId(Long.valueOf(userId));
	}

	Optional<FcmToken> findByFcmToken(String fcmToken);

	Optional<FcmToken> findFirstByFcmToken(String fcmToken);

	List<FcmToken> findByUserIdAndIsActive(Long userId, Boolean isActive);

	default List<FcmToken> findByUserIdAndIsActive(String userId, Boolean isActive) {
		return findByUserIdAndIsActive(Long.valueOf(userId), isActive);
	}

	List<FcmToken> findByIsActive(Boolean isActive);

	List<FcmToken> findAllByFcmTokenIn(List<String> fcmTokens);

	@Transactional
	long deleteByUpdatedAtBeforeAndIsActive(LocalDateTime cutoff, Boolean isActive);

}
