package com.trustdesk.ai.reply;

import com.trustdesk.ai.triage.TriageResponse;
import com.trustdesk.knowledge.dto.KnowledgeDocumentResponse;
import com.trustdesk.ticket.dto.TicketResponse;

import java.util.List;

public record ReplyGenerationInput(
        TicketResponse ticket,
        TriageResponse triage,
        List<KnowledgeDocumentResponse> knowledgeDocuments
) {}
