package com.trustdesk.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SupportActionTest {

    @Test
    void shouldApprovePendingAction() {
        SupportAction action = new SupportAction(
                "action-001",
                null,
                ActionType.CREATE_REPLACEMENT_ORDER,
                ActionStatus.PENDING_APPROVAL,
                "{}",
                "idem-001",
                "Damaged product",
                true
        );

        action.approve("support-agent");

        assertEquals(ActionStatus.APPROVED, action.getStatus());
        assertEquals("support-agent", action.getApprovedBy());
    }

    @Test
    void shouldNotExecutePendingAction() {
        SupportAction action = new SupportAction(
                "action-002",
                null,
                ActionType.START_REFUND_REVIEW,
                ActionStatus.PENDING_APPROVAL,
                "{}",
                "idem-002",
                "Duplicate charge",
                true
        );

        assertThrows(
                IllegalStateException.class,
                action::markExecuted
        );
    }

    @Test
    void shouldNotApproveRejectedAction() {
        SupportAction action = new SupportAction(
                "action-003",
                null,
                ActionType.CREATE_REPLACEMENT_ORDER,
                ActionStatus.REJECTED,
                "{}",
                "idem-003",
                "Damaged product",
                true
        );

        assertThrows(
                IllegalStateException.class,
                () -> action.approve("support-agent")
        );
    }

    @Test
    void shouldMarkApprovedActionAsFailed() {
        SupportAction action = new SupportAction(
                "action-004",
                null,
                ActionType.OPEN_CARRIER_INVESTIGATION,
                ActionStatus.APPROVED,
                "{}",
                "idem-004",
                "Carrier investigation",
                false
        );

        action.markFailed();

        assertEquals(ActionStatus.FAILED, action.getStatus());

        assertThrows(
                IllegalStateException.class,
                action::markExecuted
        );
    }

    @Test
    void shouldNotMarkPendingActionAsFailed() {
        SupportAction action = new SupportAction(
                "action-005",
                null,
                ActionType.START_REFUND_REVIEW,
                ActionStatus.PENDING_APPROVAL,
                "{}",
                "idem-005",
                "Refund review",
                true
        );

        assertThrows(
                IllegalStateException.class,
                action::markFailed
        );

        assertEquals(
                ActionStatus.PENDING_APPROVAL,
                action.getStatus()
        );
    }
}
