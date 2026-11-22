package com.trustdesk.action;

import com.trustdesk.ticket.Ticket;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "support_actions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_support_actions_idempotency_key",
                        columnNames = "idempotency_key"
                )
        }
)
public class SupportAction {

    @Id
    @Column(name = "action_id", length = 50)
    private String actionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActionStatus status;

    @Column(name = "parameters_json", columnDefinition = "TEXT")
    private String parametersJson;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "requires_human_approval", nullable = false)
    private boolean requiresHumanApproval;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected SupportAction() {
    }

    public SupportAction(
            String actionId,
            Ticket ticket,
            ActionType actionType,
            ActionStatus status,
            String parametersJson,
            String idempotencyKey,
            String reason,
            boolean requiresHumanApproval) {

        this.actionId = actionId;
        this.ticket = ticket;
        this.actionType = actionType;
        this.status = status;
        this.parametersJson = parametersJson;
        this.idempotencyKey = idempotencyKey;
        this.reason = reason;
        this.requiresHumanApproval = requiresHumanApproval;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void approve(String approvedBy) {

        if (status != ActionStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Only actions pending approval can be approved.");
        }

        if (approvedBy == null || approvedBy.isBlank()) {
            throw new IllegalArgumentException(
                    "Approver identity is required.");
        }

        this.status = ActionStatus.APPROVED;
        this.approvedBy = approvedBy;
        this.updatedAt = OffsetDateTime.now();
    }

    public void reject() {

        if (status != ActionStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Only actions pending approval can be rejected.");
        }

        this.status = ActionStatus.REJECTED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markExecuted() {

        if (status != ActionStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only approved actions can be executed.");
        }

        this.status = ActionStatus.EXECUTED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markFailed() {
        if (status != ActionStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only approved actions can be marked as failed.");
        }

        status = ActionStatus.FAILED;
        updatedAt = OffsetDateTime.now();
    }

    public String getActionId() { return actionId; }
    public Ticket getTicket() { return ticket; }
    public ActionType getActionType() { return actionType; }
    public ActionStatus getStatus() { return status; }
    public String getParametersJson() { return parametersJson; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getReason() { return reason; }
    public boolean isRequiresHumanApproval() { return requiresHumanApproval; }
    public String getApprovedBy() { return approvedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
