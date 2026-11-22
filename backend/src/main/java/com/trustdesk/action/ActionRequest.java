package com.trustdesk.action;

import java.math.BigDecimal;
import java.util.Map;

public record ActionRequest(
        String ticketId,
        ActionType actionType,
        Map<String, String> parameters,
        BigDecimal amount,
        String reason
) {}
