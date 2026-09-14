package com.linglevel.api.user.ticket.service;

import com.linglevel.api.user.ticket.entity.TicketReservation;
import com.linglevel.api.user.ticket.entity.TicketReservationStatus;
import com.linglevel.api.user.ticket.entity.UserTicket;
import com.linglevel.api.user.ticket.exception.TicketException;
import com.linglevel.api.user.ticket.repository.TicketReservationRepository;
import com.linglevel.api.user.ticket.repository.TicketTransactionRepository;
import com.linglevel.api.user.ticket.repository.UserTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

	@Mock
	private UserTicketRepository userTicketRepository;
	@Mock
	private TicketTransactionRepository ticketTransactionRepository;
	@Mock
	private TicketReservationRepository ticketReservationRepository;
	@InjectMocks
	private TicketService ticketService;

	private UserTicket wallet;

	@BeforeEach
	void setUp() {
		wallet = UserTicket.builder().userId(1L).balance(10).build();
	}

	@Test
	void reservesTicketAndReducesAvailableBalance() {
		TicketReservation saved = TicketReservation.builder().id(7L).userId(1L).amount(3)
			.description("custom content").status(TicketReservationStatus.RESERVED).build();
		when(userTicketRepository.findById(1L)).thenReturn(Optional.of(wallet));
		when(ticketReservationRepository.save(any())).thenReturn(saved);

		assertThat(ticketService.reserveTicket("1", 3, "custom content")).isEqualTo("7");
		assertThat(wallet.getBalance()).isEqualTo(7);
	}

	@Test
	void confirmsReservationAndWritesFinalTransaction() {
		TicketReservation reservation = TicketReservation.builder().id(7L).userId(1L).amount(3)
			.description("custom content").status(TicketReservationStatus.RESERVED).build();
		when(ticketReservationRepository.findById(7L)).thenReturn(Optional.of(reservation));

		ticketService.confirmReservation("7");

		assertThat(reservation.getStatus()).isEqualTo(TicketReservationStatus.CONFIRMED);
		ArgumentCaptor<com.linglevel.api.user.ticket.entity.TicketTransaction> captor = ArgumentCaptor
			.forClass(com.linglevel.api.user.ticket.entity.TicketTransaction.class);
		verify(ticketTransactionRepository).save(captor.capture());
		assertThat(captor.getValue().getAmount()).isEqualTo(-3);
	}

	@Test
	void rejectsReservationWhenBalanceIsInsufficient() {
		when(userTicketRepository.findById(1L)).thenReturn(Optional.of(wallet));

		assertThatThrownBy(() -> ticketService.reserveTicket("1", 11, "custom content"))
			.isInstanceOf(TicketException.class);
		verifyNoInteractions(ticketReservationRepository);
	}

}
