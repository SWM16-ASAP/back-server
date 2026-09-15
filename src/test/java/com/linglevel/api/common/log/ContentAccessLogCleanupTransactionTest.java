package com.linglevel.api.common.log;

import com.linglevel.api.common.AbstractMysqlTest;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.content.recommendation.entity.ContentAccessLog;
import com.linglevel.api.content.recommendation.repository.ContentAccessLogRepository;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.entity.UserRole;
import com.linglevel.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the production call shape: the cleanup scheduler has no
 * {@code @Transactional} of its own, so the repository's derived delete query must carry
 * its own transaction. A {@code @DataJpaTest} without {@code Propagation.NOT_SUPPORTED}
 * would hide this — its ambient test transaction supplies the EntityManager the delete
 * needs regardless of whether the repository method is annotated.
 */
@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/mysql,classpath:db/testmigration/mysql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ContentAccessLogCleanupTransactionTest extends AbstractMysqlTest {

	@Autowired
	ContentAccessLogRepository accessLogs;

	@Autowired
	UserRepository users;

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void deleteByAccessedAtBeforeWorksWithNoAmbientTransaction() {
		User user = users.save(User.builder().username("probe-user").role(UserRole.USER).build());
		ContentAccessLog old = accessLogs.save(ContentAccessLog.builder()
			.userId(user.getId())
			.contentId(1L)
			.contentType(ContentType.ARTICLE)
			.accessedAt(Instant.now().minusSeconds(200 * 24 * 3600L))
			.build());

		accessLogs.deleteByAccessedAtBefore(Instant.now().minusSeconds(120L * 24 * 3600));

		assertThat(accessLogs.findById(old.getId())).isEmpty();
	}

}
