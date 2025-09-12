package com.linglevel.api.user.ticket.repository;

import com.linglevel.api.user.ticket.entity.UserTicket;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserTicketRepository extends MongoRepository<UserTicket, String> {
    Optional<UserTicket> findByUserId(String userId);
}