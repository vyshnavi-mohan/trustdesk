package com.trustdesk.action;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportActionRepository
        extends JpaRepository<SupportAction, String> {

    Optional<SupportAction> findByIdempotencyKey(String idempotencyKey);

    List<SupportAction> findByTicket_TicketId(String ticketId);

    List<SupportAction> findByStatus(ActionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM SupportAction a WHERE a.actionId = :actionId")
    Optional<SupportAction> findByIdWithLock(
            @Param("actionId") String actionId);
}
