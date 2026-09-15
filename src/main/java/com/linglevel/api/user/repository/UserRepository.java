package com.linglevel.api.user.repository;

import com.linglevel.api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@org.springframework.data.jpa.repository.Query("select u from User u where u.id = :id")
	Optional<User> findForUpdateById(@org.springframework.data.repository.query.Param("id") Long id);

	default Optional<User> findById(String id) {
		try {
			return findById(Long.parseLong(id));
		}
		catch (NumberFormatException exception) {
			return Optional.empty();
		}
	}

	Optional<User> findByUsername(String username);

}
