package com.trustdesk.action;

import java.math.BigDecimal;
import java.util.List;

public record ActionDefinition(
        ActionType actionType,
        String description,
        String riskLevel,
        boolean requiresHumanApproval,
        List<String> allowedCategories,
        List<String> requiredFields,
        BigDecimal maxAmountInr
) {}
