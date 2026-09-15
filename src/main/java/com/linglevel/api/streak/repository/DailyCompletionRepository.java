package com.linglevel.api.streak.repository;

import com.linglevel.api.streak.entity.DailyCompletion;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;

public interface DailyCompletionRepository extends JpaRepository<DailyCompletion, Long> {

	boolean existsByUserIdAndCompletionDate(Long userId, LocalDate date);

	Optional<DailyCompletion> findByUserIdAndCompletionDate(Long userId, LocalDate date);

	Optional<DailyCompletion> findTopByUserIdAndCompletionDateBeforeOrderByCompletionDateDesc(Long userId,
			LocalDate date);

	long countByUserId(Long userId);

	List<DailyCompletion> findByUserIdAndCompletionDateBetween(Long userId, LocalDate start, LocalDate end);

	List<DailyCompletion> findByUserIdAndCompletionDateGreaterThanEqual(Long userId, LocalDate start);

	List<DailyCompletion> findByUserIdOrderByCompletionDateAsc(Long userId);

	List<DailyCompletion> findByUserIdAndCompletionDateGreaterThanEqualOrderByCompletionDateAsc(Long userId,
			LocalDate start);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select d from DailyCompletion d where d.userId = :userId and d.completionDate = :date")
	Optional<DailyCompletion> findForUpdate(@Param("userId") Long userId, @Param("date") LocalDate date);

	default Optional<DailyCompletion> findForUpdate(String userId, LocalDate date) {
		return findForUpdate(Long.valueOf(userId), date);
	}

	default boolean existsByUserIdAndCompletionDate(String u, LocalDate d) {
		return existsByUserIdAndCompletionDate(Long.valueOf(u), d);
	}

	default Optional<DailyCompletion> findByUserIdAndCompletionDate(String u, LocalDate d) {
		return findByUserIdAndCompletionDate(Long.valueOf(u), d);
	}

	default Optional<DailyCompletion> findTopByUserIdAndCompletionDateBeforeOrderByCompletionDateDesc(String u,
			LocalDate d) {
		return findTopByUserIdAndCompletionDateBeforeOrderByCompletionDateDesc(Long.valueOf(u), d);
	}

	default long countByUserId(String u) {
		return countByUserId(Long.valueOf(u));
	}

	default List<DailyCompletion> findByUserIdAndCompletionDateBetween(String u, LocalDate s, LocalDate e) {
		return findByUserIdAndCompletionDateBetween(Long.valueOf(u), s, e);
	}

	default List<DailyCompletion> findByUserIdAndCompletionDateAfter(String u, LocalDate s) {
		return findByUserIdAndCompletionDateGreaterThanEqual(Long.valueOf(u), s);
	}

	default List<DailyCompletion> findByUserIdOrderByCompletionDateAsc(String u) {
		return findByUserIdOrderByCompletionDateAsc(Long.valueOf(u));
	}

	default List<DailyCompletion> findByUserIdAndCompletionDateGreaterThanEqualOrderByCompletionDateAsc(String u,
			LocalDate s) {
		return findByUserIdAndCompletionDateGreaterThanEqualOrderByCompletionDateAsc(Long.valueOf(u), s);
	}

}
