package com.linglevel.api.user.repository;

import com.linglevel.api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

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
