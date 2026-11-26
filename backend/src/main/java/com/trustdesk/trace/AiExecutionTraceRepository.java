package com.trustdesk.trace;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiExecutionTraceRepository
        extends JpaRepository<AiExecutionTrace, String> {

    List<AiExecutionTrace> findByTicketIdOrderByCreatedAtDesc(
            String ticketId);

    List<AiExecutionTrace> findAllByOrderByCreatedAtDesc();
}