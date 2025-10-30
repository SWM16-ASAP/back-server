package com.linglevel.api.user.ticket.repository;

import com.linglevel.api.user.ticket.entity.TicketTransaction;
import com.linglevel.api.user.ticket.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketTransactionRepository extends MongoRepository<TicketTransaction, String> {

    Page<TicketTransaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<TicketTransaction> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, TransactionStatus status, Pageable pageable);

    List<TicketTransaction> findByReservationId(String reservationId);

    Optional<TicketTransaction> findByReservationIdAndStatus(String reservationId, TransactionStatus status);

    List<TicketTransaction> findByUserIdAndAmountAndCreatedAtBetween(String userId, Integer amount, LocalDateTime startDateTime, LocalDateTime endDateTime);
}