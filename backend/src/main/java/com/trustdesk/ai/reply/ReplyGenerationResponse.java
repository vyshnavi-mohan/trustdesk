package com.trustdesk.ai.reply;

import java.util.List;

public record ReplyGenerationResponse(
        String ticketId,
        ReplyStatus status,
        String draftReply,
        List<Citation> citations,
        String reasoning
) {

    public enum ReplyStatus {
        DRAFT,
        ESCALATION_REQUIRED
    }

    public record Citation(
            String documentId,
            String title
    ) {}
}
