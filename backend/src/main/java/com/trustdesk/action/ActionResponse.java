package com.trustdesk.action;

import java.time.OffsetDateTime;
import java.util.Map;

public record ActionResponse(
        String actionId,
        String ticketId,
        ActionType actionType,
        ActionStatus status,
        Map<String, String> parameters,
        String reason,
        boolean requiresHumanApproval,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
