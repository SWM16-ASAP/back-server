package com.linglevel.api.user.ticket.repository;

import com.linglevel.api.user.ticket.entity.TicketTransaction;
import com.linglevel.api.user.ticket.entity.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataMongoTest
class TicketTransactionRepositoryTest {

    @Autowired
    private TicketTransactionRepository ticketTransactionRepository;

    private final String testUserId = "test-user-id";
    private final String otherUserId = "other-user-id";
    private final String testReservationId = "test-reservation-id";

    @BeforeEach
    void setUp() {
        ticketTransactionRepository.deleteAll();
    }

    @Test
    void 사용자ID로_거래내역조회_성공() {
        // given
        TicketTransaction transaction1 = createTransaction(testUserId, -5, "Transaction 1", TransactionStatus.CONFIRMED);
        TicketTransaction transaction2 = createTransaction(testUserId, 10, "Transaction 2", TransactionStatus.CONFIRMED);
        TicketTransaction transaction3 = createTransaction(testUserId, -3, "Transaction 3", TransactionStatus.RESERVED);
        TicketTransaction transaction4 = createTransaction(otherUserId, -2, "Other user transaction", TransactionStatus.CONFIRMED);

        ticketTransactionRepository.saveAll(List.of(transaction1, transaction2, transaction3, transaction4));

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<TicketTransaction> result = ticketTransactionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(testUserId, TransactionStatus.CONFIRMED, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        // 해당 사용자의 CONFIRMED 상태 거래만 포함되어야 함
        result.getContent().forEach(transaction -> {
            assertThat(transaction.getUserId()).isEqualTo(testUserId);
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.CONFIRMED);
        });
    }

    @Test
    void 예약ID와상태로_거래조회_성공() {
        // given
        TicketTransaction reservedTransaction = TicketTransaction.builder()
                .userId(testUserId)
                .amount(-5)
                .description("Reserved transaction")
                .status(TransactionStatus.RESERVED)
                .reservationId(testReservationId)
                .build();

        TicketTransaction confirmedTransaction = TicketTransaction.builder()
                .userId(testUserId)
                .amount(-3)
                .description("Confirmed transaction")
                .status(TransactionStatus.CONFIRMED)
                .reservationId("other-reservation-id")
                .build();

        ticketTransactionRepository.saveAll(List.of(reservedTransaction, confirmedTransaction));

        // when
        Optional<TicketTransaction> result = ticketTransactionRepository
                .findByReservationIdAndStatus(testReservationId, TransactionStatus.RESERVED);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getDescription()).isEqualTo("Reserved transaction");
        assertThat(result.get().getReservationId()).isEqualTo(testReservationId);
        assertThat(result.get().getStatus()).isEqualTo(TransactionStatus.RESERVED);
    }

    @Test
    void 존재하지않는예약ID로_조회시_빈Optional반환() {
        // when
        Optional<TicketTransaction> result = ticketTransactionRepository
                .findByReservationIdAndStatus("non-existent-reservation", TransactionStatus.RESERVED);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void 페이징처리_확인() {
        // given
        for (int i = 1; i <= 25; i++) {
            TicketTransaction transaction = createTransaction(testUserId, -i, "Transaction " + i, TransactionStatus.CONFIRMED);
            ticketTransactionRepository.save(transaction);
        }

        Pageable firstPage = PageRequest.of(0, 10);
        Pageable secondPage = PageRequest.of(1, 10);

        // when
        Page<TicketTransaction> firstResult = ticketTransactionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(testUserId, TransactionStatus.CONFIRMED, firstPage);
        Page<TicketTransaction> secondResult = ticketTransactionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(testUserId, TransactionStatus.CONFIRMED, secondPage);

        // then
        assertThat(firstResult.getContent()).hasSize(10);
        assertThat(secondResult.getContent()).hasSize(10);
        assertThat(firstResult.getTotalElements()).isEqualTo(25);
        assertThat(firstResult.getTotalPages()).isEqualTo(3);

        // 첫 번째 페이지와 두 번째 페이지의 내용이 다른지 확인
        assertThat(firstResult.getContent().get(0).getDescription())
                .isNotEqualTo(secondResult.getContent().get(0).getDescription());
    }

    @Test
    void 거래상태별_필터링_확인() {
        // given
        TicketTransaction confirmedTransaction = createTransaction(testUserId, -5, "Confirmed", TransactionStatus.CONFIRMED);
        TicketTransaction reservedTransaction = createTransaction(testUserId, -3, "Reserved", TransactionStatus.RESERVED);
        TicketTransaction cancelledTransaction = createTransaction(testUserId, -2, "Cancelled", TransactionStatus.CANCELLED);

        ticketTransactionRepository.saveAll(List.of(confirmedTransaction, reservedTransaction, cancelledTransaction));

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<TicketTransaction> confirmedResult = ticketTransactionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(testUserId, TransactionStatus.CONFIRMED, pageable);
        Page<TicketTransaction> reservedResult = ticketTransactionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(testUserId, TransactionStatus.RESERVED, pageable);

        // then
        assertThat(confirmedResult.getContent()).hasSize(1);
        assertThat(confirmedResult.getContent().get(0).getDescription()).isEqualTo("Confirmed");

        assertThat(reservedResult.getContent()).hasSize(1);
        assertThat(reservedResult.getContent().get(0).getDescription()).isEqualTo("Reserved");
    }

    private TicketTransaction createTransaction(String userId, int amount, String description, TransactionStatus status) {
        return TicketTransaction.builder()
                .userId(userId)
                .amount(amount)
                .description(description)
                .status(status)
                .build();
    }
}