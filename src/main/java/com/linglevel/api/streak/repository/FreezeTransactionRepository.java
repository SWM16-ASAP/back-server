package com.linglevel.api.streak.repository;

import com.linglevel.api.streak.entity.FreezeTransaction;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.time.Instant;
import java.util.List;

public interface FreezeTransactionRepository extends JpaRepository<FreezeTransaction, Long> {

	boolean existsByUserIdAndAmountAndEffectiveDate(Long userId, Integer amount, java.time.LocalDate date);

	default boolean existsByUserIdAndAmountAndEffectiveDate(String userId, Integer amount, java.time.LocalDate date) {
		return existsByUserIdAndAmountAndEffectiveDate(Long.valueOf(userId), amount, date);
	}

	Page<FreezeTransaction> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

	@Query("select f from FreezeTransaction f where f.userId = :userId and f.amount = :amount and f.createdAt >= :start and f.createdAt < :end order by f.createdAt, f.id")
	List<FreezeTransaction> findInWindow(@Param("userId") Long userId, @Param("amount") int amount,
			@Param("start") Instant start, @Param("end") Instant end);

	default Page<FreezeTransaction> findByUserIdOrderByCreatedAtDesc(String u, Pageable p) {
		return findByUserIdOrderByCreatedAtDescIdDesc(Long.valueOf(u), p);
	}

	default boolean existsByUserIdAndAmountAndCreatedAtBetween(String u, int a, Instant s, Instant e) {
		return !findInWindow(Long.valueOf(u), a, s, e).isEmpty();
	}

	default List<FreezeTransaction> findByUserIdAndAmountAndCreatedAtBetween(String u, int a, Instant s, Instant e) {
		return findInWindow(Long.valueOf(u), a, s, e);
	}

}
