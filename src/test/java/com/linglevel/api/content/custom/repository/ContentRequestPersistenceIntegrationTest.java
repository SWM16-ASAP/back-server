package com.linglevel.api.content.custom.repository;

import com.linglevel.api.common.AbstractMysqlTest;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.custom.entity.ContentRequest;
import com.linglevel.api.content.custom.entity.ContentRequestStatus;
import com.linglevel.api.content.custom.entity.ContentType;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.entity.UserRole;
import com.linglevel.api.user.repository.UserRepository;
import com.linglevel.api.user.ticket.entity.TicketReservation;
import com.linglevel.api.user.ticket.entity.TicketReservationStatus;
import com.linglevel.api.user.ticket.repository.TicketReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/mysql,classpath:db/testmigration/mysql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ContentRequestPersistenceIntegrationTest extends AbstractMysqlTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TicketReservationRepository ticketReservationRepository;

	@Autowired
	private ContentRequestRepository contentRequestRepository;

	@Test
	void storesExternalRequestKeyWithUserAndReservation() {
		User user = userRepository.saveAndFlush(User.builder().username("content-user").role(UserRole.USER).build());
		TicketReservation reservation = ticketReservationRepository.saveAndFlush(TicketReservation.builder()
			.userId(user.getId())
			.amount(1)
			.description("custom content")
			.status(TicketReservationStatus.RESERVED)
			.build());
		ContentRequest request = contentRequestRepository.saveAndFlush(ContentRequest.builder()
			.requestKey("content-request-test-key")
			.userId(user.getId())
			.ticketReservationId(reservation.getId())
			.title("English article")
			.contentType(ContentType.TEXT)
			.targetDifficultyLevels(List.of(DifficultyLevel.A1))
			.status(ContentRequestStatus.PENDING)
			.build());

		assertThat(contentRequestRepository.findByRequestKey(request.getRequestKey())).contains(request);
		assertThat(contentRequestRepository.findByRequestKeyAndUserId(request.getRequestKey(), user.getId()))
			.contains(request);
	}

}
