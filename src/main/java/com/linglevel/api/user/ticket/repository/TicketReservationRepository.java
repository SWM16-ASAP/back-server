package com.linglevel.api.user.ticket.repository;

import com.linglevel.api.user.ticket.entity.TicketReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketReservationRepository extends JpaRepository<TicketReservation, Long> {

}
