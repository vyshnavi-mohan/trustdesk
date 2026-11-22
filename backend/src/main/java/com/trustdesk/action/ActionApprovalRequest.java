package com.trustdesk.action;

import jakarta.validation.constraints.NotBlank;

public record ActionApprovalRequest(

        @NotBlank(message = "Decision is required.")
        String decision
) {}