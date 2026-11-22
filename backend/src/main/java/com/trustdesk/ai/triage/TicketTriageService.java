
package com.trustdesk.ai.triage;

import com.trustdesk.knowledge.TicketKnowledgeRetrievalService;
import com.trustdesk.knowledge.dto.KnowledgeDocumentResponse;
import com.trustdesk.ticket.TicketService;
import com.trustdesk.ticket.dto.TicketResponse;
import com.trustdesk.trace.AiExecutionTraceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketTriageService {

    private final TicketService ticketService;
    private final TicketKnowledgeRetrievalService knowledgeRetrievalService;
    private final TriageProvider triageProvider;
    private final AiExecutionTraceService traceService;

    public TicketTriageService(
            TicketService ticketService,
            TicketKnowledgeRetrievalService knowledgeRetrievalService,
            TriageProvider triageProvider,
            AiExecutionTraceService traceService) {
        this.ticketService = ticketService;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.triageProvider = triageProvider;
        this.traceService = traceService;
    }

    @Transactional(readOnly = true)
    public TriageResponse triageTicket(String ticketId) {

        TicketResponse ticketResponse =
                ticketService.getTicketById(ticketId);

        List<KnowledgeDocumentResponse> knowledgeDocuments =
                knowledgeRetrievalService.retrieveForTicket(ticketId);

        TriageInput input = new TriageInput(
                ticketResponse,
                knowledgeDocuments
        );

        TriageResponse response = triageProvider.analyze(input);

        List<String> documentIds = knowledgeDocuments.stream()
                .map(KnowledgeDocumentResponse::documentId)
                .toList();

        List<String> recommendedActions = List.of(
                "RECOMMENDED:" + response.recommendedAction().name()
        );

        String guardrailResult =
                response.recommendedAction() == RecommendedAction.ESCALATE_TO_HUMAN
                        ? "REVIEW_REQUIRED"
                        : "PASSED";

        String finalStatus =
                response.recommendedAction() == RecommendedAction.ESCALATE_TO_HUMAN
                        ? "ESCALATION_REQUIRED"
                        : "COMPLETED";

        traceService.record(
                ticketId,
                "TRIAGE",
                documentIds,
                recommendedActions,
                guardrailResult,
                finalStatus
        );

        return response;
    }
}
