package com.linglevel.api.common.log;

import com.linglevel.api.common.AbstractMysqlTest;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.content.recommendation.entity.ContentAccessLog;
import com.linglevel.api.content.recommendation.repository.ContentAccessLogRepository;
import com.linglevel.api.fcm.entity.PushLog;
import com.linglevel.api.fcm.repository.PushLogRepository;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.entity.UserRole;
import com.linglevel.api.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/mysql,classpath:db/testmigration/mysql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LogPersistenceIntegrationTest extends AbstractMysqlTest {

	@Autowired
	ContentAccessLogRepository accessLogs;

	@Autowired
	PushLogRepository pushLogs;

	@Autowired
	UserRepository users;

	@Autowired
	EntityManager entityManager;

	@Test
	void contentAccessLogRequiresExistingUser() {
		ContentAccessLog orphan = ContentAccessLog.builder()
			.userId(Long.MAX_VALUE)
			.contentId(1L)
			.contentType(ContentType.ARTICLE)
			.accessedAt(Instant.now())
			.build();
		assertThatThrownBy(() -> accessLogs.saveAndFlush(orphan))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void oldContentAccessLogsAreDeletedByCleanupQuery() {
		User user = users.saveAndFlush(User.builder().username("reader").role(UserRole.USER).build());
		ContentAccessLog old = accessLogs.saveAndFlush(ContentAccessLog.builder()
			.userId(user.getId())
			.contentId(1L)
			.contentType(ContentType.ARTICLE)
			.accessedAt(Instant.now().minusSeconds(200 * 24 * 3600L))
			.build());
		ContentAccessLog recent = accessLogs.saveAndFlush(ContentAccessLog.builder()
			.userId(user.getId())
			.contentId(2L)
			.contentType(ContentType.ARTICLE)
			.accessedAt(Instant.now())
			.build());

		accessLogs.deleteByAccessedAtBefore(Instant.now().minusSeconds(120L * 24 * 3600));

		assertThat(accessLogs.findById(old.getId())).isEmpty();
		assertThat(accessLogs.findById(recent.getId())).isPresent();
	}

	@Test
	void pushLogCampaignIdIsUniqueAndRequiresExistingUser() {
		User user = users.saveAndFlush(User.builder().username("reader2").role(UserRole.USER).build());
		String campaignId = UUID.randomUUID().toString();
		pushLogs.saveAndFlush(pushLog(user.getId(), campaignId));
		PushLog duplicate = pushLog(user.getId(), campaignId);
		assertThatThrownBy(() -> pushLogs.saveAndFlush(duplicate))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

		PushLog orphan = pushLog(Long.MAX_VALUE, UUID.randomUUID().toString());
		assertThatThrownBy(() -> pushLogs.saveAndFlush(orphan))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void pushLogOptimisticLockRejectsStaleUpdate() {
		User user = users.saveAndFlush(User.builder().username("reader3").role(UserRole.USER).build());
		PushLog saved = pushLogs.saveAndFlush(pushLog(user.getId(), UUID.randomUUID().toString()));

		PushLog first = pushLogs.findById(saved.getId()).orElseThrow();
		entityManager.detach(first);
		PushLog second = pushLogs.findById(saved.getId()).orElseThrow();
		entityManager.detach(second);

		first.setOpenedAt(LocalDateTime.now());
		pushLogs.saveAndFlush(first);

		second.setOpenedAt(LocalDateTime.now());
		assertThatThrownBy(() -> pushLogs.saveAndFlush(second))
			.isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
	}

	@Test
	void oldPushLogsAreDeletedByCleanupQuery() {
		User user = users.saveAndFlush(User.builder().username("reader4").role(UserRole.USER).build());
		PushLog old = pushLog(user.getId(), UUID.randomUUID().toString());
		old.setCreatedAt(LocalDateTime.now().minusDays(181));
		pushLogs.saveAndFlush(old);
		PushLog recent = pushLogs.saveAndFlush(pushLog(user.getId(), UUID.randomUUID().toString()));

		long deleted = pushLogs.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(180));

		assertThat(deleted).isEqualTo(1);
		assertThat(pushLogs.findById(old.getId())).isEmpty();
		assertThat(pushLogs.findById(recent.getId())).isPresent();
	}

	private PushLog pushLog(Long userId, String campaignId) {
		LocalDateTime now = LocalDateTime.now();
		return PushLog.builder()
			.campaignId(campaignId)
			.userId(userId)
			.sentAt(now)
			.sentSuccess(true)
			.createdAt(now)
			.build();
	}

}
