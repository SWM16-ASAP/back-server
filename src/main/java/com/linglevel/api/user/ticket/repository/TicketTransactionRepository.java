package com.linglevel.api.user.ticket.repository;

import com.linglevel.api.user.ticket.entity.TicketTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketTransactionRepository extends JpaRepository<TicketTransaction, Long> {

	Page<TicketTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	List<TicketTransaction> findByUserIdAndAmountAndCreatedAtBetween(Long userId, Integer amount,
			LocalDateTime startDateTime, LocalDateTime endDateTime);

	default List<TicketTransaction> findByUserIdAndAmountAndCreatedAtBetween(String userId, Integer amount,
			LocalDateTime startDateTime, LocalDateTime endDateTime) {
		return findByUserIdAndAmountAndCreatedAtBetween(Long.parseLong(userId), amount, startDateTime, endDateTime);
	}

}
