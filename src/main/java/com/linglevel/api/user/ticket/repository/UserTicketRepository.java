package com.linglevel.api.user.ticket.repository;

import com.linglevel.api.user.ticket.entity.UserTicket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTicketRepository extends JpaRepository<UserTicket, Long> {

	default UserTicket getReferenceByUserId(String userId) {
		return getReferenceById(Long.parseLong(userId));
	}

}
