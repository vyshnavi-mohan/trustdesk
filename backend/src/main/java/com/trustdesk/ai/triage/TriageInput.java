package com.trustdesk.ai.triage;

import com.trustdesk.knowledge.dto.KnowledgeDocumentResponse;
import com.trustdesk.ticket.dto.TicketResponse;

import java.util.List;

public record TriageInput(
        TicketResponse ticket,
        List<KnowledgeDocumentResponse> knowledgeDocuments
) {
}
