package com.linglevel.api.content.feed.repository;

import com.linglevel.api.content.feed.entity.FeedSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedSourceRepository extends JpaRepository<FeedSource, Long> {

	boolean existsByUrl(String url);

	Optional<FeedSource> findByUrl(String url);

	List<FeedSource> findByIsActiveTrue();

	Optional<FeedSource> findById(Long id);

	default Optional<FeedSource> findById(String id) {
		return findById(Long.valueOf(id));
	}

	default boolean existsById(String id) {
		return existsById(Long.valueOf(id));
	}

	default void deleteById(String id) {
		deleteById(Long.valueOf(id));
	}

}
