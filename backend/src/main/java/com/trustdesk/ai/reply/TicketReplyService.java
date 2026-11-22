
package com.trustdesk.ai.reply;

import com.trustdesk.ai.triage.TriageInput;
import com.trustdesk.ai.triage.TriageProvider;
import com.trustdesk.ai.triage.TriageResponse;
import com.trustdesk.knowledge.TicketKnowledgeRetrievalService;
import com.trustdesk.knowledge.dto.KnowledgeDocumentResponse;
import com.trustdesk.ticket.TicketService;
import com.trustdesk.ticket.dto.TicketResponse;
import com.trustdesk.trace.AiExecutionTraceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketReplyService {

    private final TicketService ticketService;
    private final TicketKnowledgeRetrievalService knowledgeRetrievalService;
    private final TriageProvider triageProvider;
    private final ReplyGenerationProvider replyGenerationProvider;
    private final AiExecutionTraceService traceService;

    public TicketReplyService(
            TicketService ticketService,
            TicketKnowledgeRetrievalService knowledgeRetrievalService,
            TriageProvider triageProvider,
            ReplyGenerationProvider replyGenerationProvider,
            AiExecutionTraceService traceService) {
        this.ticketService = ticketService;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.triageProvider = triageProvider;
        this.replyGenerationProvider = replyGenerationProvider;
        this.traceService = traceService;
    }

    @Transactional(readOnly = true)
    public ReplyGenerationResponse generateDraftReply(String ticketId) {

        TicketResponse ticket = ticketService.getTicketById(ticketId);

        List<KnowledgeDocumentResponse> documents =
                knowledgeRetrievalService.retrieveForTicket(ticketId);

        TriageResponse triage = triageProvider.analyze(
                new TriageInput(ticket, documents));

        ReplyGenerationInput input =
                new ReplyGenerationInput(ticket, triage, documents);

        ReplyGenerationResponse response =
                replyGenerationProvider.generate(input);

        List<String> documentIds = documents.stream()
                .map(KnowledgeDocumentResponse::documentId)
                .toList();

        List<String> recommendedActions = List.of(
                "RECOMMENDED:" + triage.recommendedAction().name()
        );

        String guardrailResult =
                response.status() ==
                        ReplyGenerationResponse.ReplyStatus.ESCALATION_REQUIRED
                        ? "REVIEW_REQUIRED"
                        : "PASSED";

        traceService.record(
                ticketId,
                "DRAFT_REPLY",
                documentIds,
                recommendedActions,
                guardrailResult,
                response.status().name()
        );

        return response;
    }
}
