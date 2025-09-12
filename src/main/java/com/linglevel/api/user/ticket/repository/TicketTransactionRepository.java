package com.linglevel.api.user.ticket.repository;

import com.linglevel.api.user.ticket.entity.TicketTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TicketTransactionRepository extends MongoRepository<TicketTransaction, String> {
    Page<TicketTransaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}