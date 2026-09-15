package com.linglevel.api.content.custom.repository;

import com.linglevel.api.content.custom.entity.ContentRequest;
import com.linglevel.api.content.custom.entity.ContentRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContentRequestRepository extends JpaRepository<ContentRequest, Long> {

	Page<ContentRequest> findByUserIdAndStatusNot(Long userId, ContentRequestStatus status, Pageable pageable);

	Page<ContentRequest> findByUserIdAndStatus(Long userId, ContentRequestStatus status, Pageable pageable);

	Optional<ContentRequest> findByRequestKey(String requestKey);

	@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@org.springframework.data.jpa.repository.Query("select r from ContentRequest r where r.requestKey = :key")
	Optional<ContentRequest> findForUpdateByRequestKey(
			@org.springframework.data.repository.query.Param("key") String key);

	Optional<ContentRequest> findByRequestKeyAndUserId(String requestKey, Long userId);

}
