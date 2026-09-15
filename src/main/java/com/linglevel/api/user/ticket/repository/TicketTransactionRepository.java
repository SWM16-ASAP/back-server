package com.linglevel.api.user.ticket.repository;

import com.linglevel.api.user.ticket.entity.TicketTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketTransactionRepository extends JpaRepository<TicketTransaction, Long> {

	Page<TicketTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	@org.springframework.data.jpa.repository.Query("select t from TicketTransaction t where t.userId = :userId and t.amount = :amount and t.createdAt >= :start and t.createdAt < :end")
	List<TicketTransaction> findByUserIdAndAmountAndCreatedAtBetween(
			@org.springframework.data.repository.query.Param("userId") Long userId,
			@org.springframework.data.repository.query.Param("amount") Integer amount,
			@org.springframework.data.repository.query.Param("start") LocalDateTime startDateTime,
			@org.springframework.data.repository.query.Param("end") LocalDateTime endDateTime);

	default List<TicketTransaction> findByUserIdAndAmountAndCreatedAtBetween(String userId, Integer amount,
			LocalDateTime startDateTime, LocalDateTime endDateTime) {
		return findByUserIdAndAmountAndCreatedAtBetween(Long.parseLong(userId), amount, startDateTime, endDateTime);
	}

}
