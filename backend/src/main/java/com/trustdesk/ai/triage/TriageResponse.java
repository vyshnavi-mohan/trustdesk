package com.trustdesk.ai.triage;

import java.util.List;

public record TriageResponse(
        String ticketId,
        TriageCategory category,
        TriagePriority priority,
        RecommendedAction recommendedAction,
        String reasoning,
        List<String> retrievedDocumentIds
) {
}
