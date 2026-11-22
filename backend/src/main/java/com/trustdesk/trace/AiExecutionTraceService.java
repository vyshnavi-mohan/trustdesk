package com.trustdesk.trace;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AiExecutionTraceService {

    private final AiExecutionTraceRepository repository;

    public AiExecutionTraceService(
            AiExecutionTraceRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiExecutionTrace record(
            String ticketId,
            String runType,
            List<String> documentIds,
            List<String> toolActions,
            String guardrailResult,
            String finalStatus) {

        AiExecutionTrace trace = new AiExecutionTrace(
                UUID.randomUUID().toString(),
                ticketId,
                runType,
                String.join(",", documentIds),
                String.join(",", toolActions),
                guardrailResult,
                finalStatus,
                OffsetDateTime.now()
        );

        return repository.save(trace);
    }

    @Transactional(readOnly = true)
    public List<AiExecutionTrace> getTracesForTicket(String ticketId) {
        return repository.findByTicketIdOrderByCreatedAtDesc(ticketId);
    }
}