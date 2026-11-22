package com.trustdesk.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TicketRepository extends JpaRepository<Ticket, String> {

    List<Ticket> findByCustomer_CustomerId(String customerId);

    List<Ticket> findByStatus(String status);

    Page<Ticket> findAll(Pageable pageable);

    Page<Ticket> findByStatus(String status, Pageable pageable);
}
