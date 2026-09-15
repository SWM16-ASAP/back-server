package com.linglevel.api.user.repository;

import com.linglevel.api.common.AbstractMysqlTest;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/mysql,classpath:db/testmigration/mysql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryIntegrationTest extends AbstractMysqlTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	void createsUserWithGeneratedIdAndDefaults() {
		User saved = userRepository.saveAndFlush(user("google_firebase-id"));

		assertThat(saved.getId()).isPositive();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getDeleted()).isFalse();
		assertThat(userRepository.findByUsername("google_firebase-id")).contains(saved);
	}

	@Test
	void rejectsDuplicateUsername() {
		userRepository.saveAndFlush(user("google_firebase-id"));

		assertThatThrownBy(() -> userRepository.saveAndFlush(user("google_firebase-id")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void keepsDeletedUserRecordAfterUsernameChanges() {
		User deletedUser = userRepository.saveAndFlush(user("google_firebase-id"));
		deletedUser.setDeleted(true);
		deletedUser.setUsername("deleted_2026-09-14_google_firebase-id");
		userRepository.saveAndFlush(deletedUser);

		User rejoinedUser = userRepository.saveAndFlush(user("google_firebase-id"));

		assertThat(rejoinedUser.getId()).isNotEqualTo(deletedUser.getId());
		assertThat(userRepository.findAll()).hasSize(2);
	}

	private User user(String username) {
		return User.builder()
			.username(username)
			.email("user@example.com")
			.displayName("User")
			.provider("google")
			.role(UserRole.USER)
			.build();
	}

}
