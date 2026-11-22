package com.trustdesk.trace;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ai_execution_traces")
public class AiExecutionTrace {

    @Id
    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "ticket_id", nullable = false)
    private String ticketId;

    @Column(name = "run_type", nullable = false)
    private String runType;

    @Column(name = "retrieved_document_ids")
    private String retrievedDocumentIds;

    @Column(name = "tool_actions")
    private String toolActions;

    @Column(name = "guardrail_result", nullable = false)
    private String guardrailResult;

    @Column(name = "final_status", nullable = false)
    private String finalStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AiExecutionTrace() {
    }

    public AiExecutionTrace(
            String traceId,
            String ticketId,
            String runType,
            String retrievedDocumentIds,
            String toolActions,
            String guardrailResult,
            String finalStatus,
            OffsetDateTime createdAt) {

        this.traceId = traceId;
        this.ticketId = ticketId;
        this.runType = runType;
        this.retrievedDocumentIds = retrievedDocumentIds;
        this.toolActions = toolActions;
        this.guardrailResult = guardrailResult;
        this.finalStatus = finalStatus;
        this.createdAt = createdAt;
    }

    public String getTraceId() { return traceId; }
    public String getTicketId() { return ticketId; }
    public String getRunType() { return runType; }
    public String getRetrievedDocumentIds() { return retrievedDocumentIds; }
    public String getToolActions() { return toolActions; }
    public String getGuardrailResult() { return guardrailResult; }
    public String getFinalStatus() { return finalStatus; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
