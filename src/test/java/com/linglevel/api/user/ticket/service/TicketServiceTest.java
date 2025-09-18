package com.linglevel.api.user.ticket.service;

import com.linglevel.api.user.ticket.dto.TicketBalanceResponse;
import com.linglevel.api.user.ticket.dto.TicketTransactionResponse;
import com.linglevel.api.user.ticket.entity.TicketTransaction;
import com.linglevel.api.user.ticket.entity.TransactionStatus;
import com.linglevel.api.user.ticket.entity.UserTicket;
import com.linglevel.api.user.ticket.exception.TicketException;
import com.linglevel.api.user.ticket.repository.TicketTransactionRepository;
import com.linglevel.api.user.ticket.repository.UserTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private UserTicketRepository userTicketRepository;

    @Mock
    private TicketTransactionRepository ticketTransactionRepository;

    @InjectMocks
    private TicketService ticketService;

    private final String userId = "test-user-id";
    private UserTicket userTicket;
    private TicketTransaction transaction;

    @BeforeEach
    void setUp() {
        userTicket = UserTicket.builder()
                .id("ticket-id")
                .userId(userId)
                .balance(10)
                .version(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        transaction = TicketTransaction.builder()
                .id("transaction-id")
                .userId(userId)
                .amount(-5)
                .description("Test transaction")
                .status(TransactionStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    class 티켓잔고조회테스트 {

        @Test
        void 기존사용자_티켓잔고조회_성공() {
            // given
            when(userTicketRepository.findByUserId(userId)).thenReturn(Optional.of(userTicket));

            // when
            TicketBalanceResponse response = ticketService.getTicketBalance(userId);

            // then
            assertThat(response.getBalance()).isEqualTo(10);
            assertThat(response.getUpdatedAt()).isEqualTo(userTicket.getUpdatedAt());
            verify(userTicketRepository).findByUserId(userId);
        }

        @Test
        void 신규사용자_티켓생성및잔고조회_성공() {
            // given
            UserTicket newUserTicket = UserTicket.builder()
                    .userId(userId)
                    .balance(3)
                    .build();

            when(userTicketRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(userTicketRepository.save(any(UserTicket.class))).thenReturn(newUserTicket);

            // when
            TicketBalanceResponse response = ticketService.getTicketBalance(userId);

            // then
            assertThat(response.getBalance()).isEqualTo(3);
            verify(userTicketRepository).findByUserId(userId);
            verify(userTicketRepository).save(any(UserTicket.class));
            verify(ticketTransactionRepository).save(any(TicketTransaction.class));
        }
    }

    @Nested
    class 티켓거래내역조회테스트 {

        @Test
        void 티켓거래내역조회_성공() {
            // given
            when(userTicketRepository.findByUserId(userId)).thenReturn(Optional.of(userTicket));

            Page<TicketTransaction> transactionPage = new PageImpl<>(
                    List.of(transaction),
                    PageRequest.of(0, 10),
                    1L
            );
            when(ticketTransactionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    eq(userId), eq(TransactionStatus.CONFIRMED), any(PageRequest.class)))
                    .thenReturn(transactionPage);

            // when
            Page<TicketTransactionResponse> result = ticketService.getTicketTransactions(userId, 1, 10);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAmount()).isEqualTo(-5);
            assertThat(result.getContent().get(0).getDescription()).isEqualTo("Test transaction");
            verify(ticketTransactionRepository).findByUserIdAndStatusOrderByCreatedAtDesc(
                    eq(userId), eq(TransactionStatus.CONFIRMED), any(PageRequest.class));
        }
    }

    @Nested
    class 티켓예약테스트 {

        @Test
        void 티켓예약_성공() {
            // given
            when(userTicketRepository.findByUserId(userId)).thenReturn(Optional.of(userTicket));
            when(userTicketRepository.save(any(UserTicket.class))).thenReturn(userTicket);

            // when
            String reservationId = ticketService.reserveTicket(userId, 5, "Test reservation");

            // then
            assertThat(reservationId).isNotNull();
            assertThat(UUID.fromString(reservationId)).isNotNull(); // UUID 형식 검증
            verify(userTicketRepository).save(any(UserTicket.class));
            verify(ticketTransactionRepository).save(any(TicketTransaction.class));
        }

        @Test
        void 잔고부족으로_티켓예약_실패() {
            // given
            when(userTicketRepository.findByUserId(userId)).thenReturn(Optional.of(userTicket));

            // when & then
            assertThatThrownBy(() -> ticketService.reserveTicket(userId, 15, "Test reservation"))
                    .isInstanceOf(TicketException.class);

            verify(userTicketRepository, never()).save(any(UserTicket.class));
            verify(ticketTransactionRepository, never()).save(any(TicketTransaction.class));
        }
    }

    @Nested
    class 예약확정테스트 {

        @Test
        void 예약확정_성공() {
            // given
            String reservationId = "test-reservation-id";
            TicketTransaction reservedTransaction = TicketTransaction.builder()
                    .id("transaction-id")
                    .userId(userId)
                    .amount(-5)
                    .description("Reserved transaction")
                    .status(TransactionStatus.RESERVED)
                    .reservationId(reservationId)
                    .build();

            when(ticketTransactionRepository.findByReservationIdAndStatus(reservationId, TransactionStatus.RESERVED))
                    .thenReturn(Optional.of(reservedTransaction));

            // when
            ticketService.confirmReservation(reservationId);

            // then
            verify(ticketTransactionRepository).save(any(TicketTransaction.class));
        }

        @Test
        void 존재하지않는예약_확정시_예외발생() {
            // given
            String reservationId = "non-existent-reservation-id";
            when(ticketTransactionRepository.findByReservationIdAndStatus(reservationId, TransactionStatus.RESERVED))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> ticketService.confirmReservation(reservationId))
                    .isInstanceOf(TicketException.class);
        }
    }

    @Nested
    class 예약취소테스트 {

        @Test
        void 예약취소_성공() {
            // given
            String reservationId = "test-reservation-id";
            TicketTransaction reservedTransaction = TicketTransaction.builder()
                    .id("transaction-id")
                    .userId(userId)
                    .amount(-5)
                    .description("Reserved transaction")
                    .status(TransactionStatus.RESERVED)
                    .reservationId(reservationId)
                    .build();

            when(ticketTransactionRepository.findByReservationIdAndStatus(reservationId, TransactionStatus.RESERVED))
                    .thenReturn(Optional.of(reservedTransaction));
            when(userTicketRepository.findByUserId(userId)).thenReturn(Optional.of(userTicket));

            // when
            ticketService.cancelReservation(reservationId);

            // then
            verify(userTicketRepository).save(any(UserTicket.class));
            verify(ticketTransactionRepository).save(any(TicketTransaction.class));
        }
    }

    @Nested
    class 티켓사용테스트 {

        @Test
        void 티켓사용_성공() {
            // given
            when(userTicketRepository.findByUserId(userId)).thenReturn(Optional.of(userTicket));
            when(userTicketRepository.save(any(UserTicket.class))).thenReturn(userTicket);

            // when
            int remainingBalance = ticketService.spendTicket(userId, 5, "Test spend");

            // then
            assertThat(remainingBalance).isEqualTo(5); // 10 - 5 = 5
            verify(userTicketRepository).save(any(UserTicket.class));
            verify(ticketTransactionRepository).save(any(TicketTransaction.class));
        }

        @Test
        void 잔고부족으로_티켓사용_실패() {
            // given
            when(userTicketRepository.findByUserId(userId)).thenReturn(Optional.of(userTicket));

            // when & then
            assertThatThrownBy(() -> ticketService.spendTicket(userId, 15, "Test spend"))
                    .isInstanceOf(TicketException.class);
        }
    }

    @Nested
    class 티켓지급테스트 {

        @Test
        void 티켓지급_성공() {
            // given
            when(userTicketRepository.findByUserId(userId)).thenReturn(Optional.of(userTicket));
            when(userTicketRepository.save(any(UserTicket.class))).thenReturn(userTicket);

            // when
            int newBalance = ticketService.grantTicket(userId, 5, "Test grant");

            // then
            assertThat(newBalance).isEqualTo(15); // 10 + 5 = 15
            verify(userTicketRepository).save(any(UserTicket.class));
            verify(ticketTransactionRepository).save(any(TicketTransaction.class));
        }
    }
}