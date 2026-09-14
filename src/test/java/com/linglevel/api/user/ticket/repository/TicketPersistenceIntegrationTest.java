package com.linglevel.api.user.ticket.repository;

import com.linglevel.api.common.AbstractMysqlTest;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.entity.UserRole;
import com.linglevel.api.user.repository.UserRepository;
import com.linglevel.api.user.ticket.entity.TicketReservation;
import com.linglevel.api.user.ticket.entity.TicketReservationStatus;
import com.linglevel.api.user.ticket.entity.TicketTransaction;
import com.linglevel.api.user.ticket.entity.UserTicket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/mysql,classpath:db/testmigration/mysql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TicketPersistenceIntegrationTest extends AbstractMysqlTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserTicketRepository userTicketRepository;

	@Autowired
	private TicketReservationRepository ticketReservationRepository;

	@Autowired
	private TicketTransactionRepository ticketTransactionRepository;

	@Test
	void storesWalletReservationAndConfirmedTransaction() {
		User user = userRepository.saveAndFlush(user());
		userTicketRepository.saveAndFlush(UserTicket.builder().userId(user.getId()).balance(9).build());
		TicketReservation reservation = ticketReservationRepository.saveAndFlush(TicketReservation.builder()
			.userId(user.getId()).amount(1).description("custom content").status(TicketReservationStatus.CONFIRMED)
			.build());
		ticketTransactionRepository.saveAndFlush(TicketTransaction.builder()
			.userId(user.getId()).amount(-1).description("custom content").reservationId(reservation.getId()).build());

		UserTicket wallet = userTicketRepository.findById(user.getId()).orElseThrow();
		assertThat(wallet.getBalance()).isEqualTo(9);
		assertThat(wallet.getVersion()).isNotNull();
		assertThat(ticketTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 10))
			.getContent()).singleElement().extracting(TicketTransaction::getReservationId).isEqualTo(reservation.getId());
	}

	private User user() {
		return User.builder().username("ticket-user").email("ticket@example.com").role(UserRole.USER).build();
	}

}
