package com.linglevel.api.content.feed.repository;

import com.linglevel.api.content.feed.entity.Feed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface FeedRepository extends JpaRepository<Feed, Long> {

	boolean existsByUrl(String url);

	Optional<Feed> findByUrl(String url);

	List<Feed> findByDeletedFalse();

	Optional<Feed> findByIdAndDeletedFalse(Long id);

	default Optional<Feed> findByIdAndDeletedFalse(String id) {
		return findByIdAndDeletedFalse(Long.valueOf(id));
	}

	@Modifying
	@Transactional
	@Query("update Feed f set f.viewCount = f.viewCount + 1 where f.id = :id")
	void incrementViewCount(@Param("id") Long id);

	default void incrementViewCount(String id) {
		incrementViewCount(Long.valueOf(id));
	}

	@Modifying
	@Transactional
	@Query("update Feed f set f.viewCount = f.viewCount + 1 where f.url = :url")
	void incrementViewCountByUrl(@Param("url") String url);

}
