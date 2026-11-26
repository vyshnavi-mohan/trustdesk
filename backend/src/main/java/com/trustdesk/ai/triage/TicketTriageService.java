package com.trustdesk.ai.triage;

import com.trustdesk.ai.guardrail.GuardrailFinding;
import com.trustdesk.ai.guardrail.GuardrailResult;
import com.trustdesk.ai.guardrail.GuardrailService;
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
    private final GuardrailService guardrailService;

    public TicketTriageService(
            TicketService ticketService,
            TicketKnowledgeRetrievalService knowledgeRetrievalService,
            TriageProvider triageProvider,
            AiExecutionTraceService traceService,
            GuardrailService guardrailService) {

        this.ticketService = ticketService;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.triageProvider = triageProvider;
        this.traceService = traceService;
        this.guardrailService = guardrailService;
    }

    @Transactional(readOnly = true)
    public TriageResponse triageTicket(String ticketId) {

        TicketResponse ticket =
                ticketService.getTicketById(ticketId);

        // Preserve the original retrieval results for audit history.
        List<KnowledgeDocumentResponse> retrievedDocuments =
                knowledgeRetrievalService.retrieveForTicket(ticketId);

        GuardrailResult guardrail =
                guardrailService.evaluate(ticket, retrievedDocuments);

        // Only trusted documents are passed to the triage provider.
        TriageInput input = new TriageInput(
                ticket,
                guardrail.trustedDocuments()
        );

        TriageResponse response = triageProvider.analyze(input);

        // Security guardrails take precedence over normal triage.
        if (guardrail.requiresHumanReview()) {

            boolean accountSecurityIssue = guardrail.findings().contains(
                    GuardrailFinding.UNVERIFIED_ACCOUNT_CHANGE
            );

            response = new TriageResponse(
                    ticketId,
                    accountSecurityIssue
                            ? TriageCategory.ACCOUNT_SECURITY
                            : TriageCategory.GENERAL,
                    accountSecurityIssue
                            ? TriagePriority.HIGH
                            : TriagePriority.MEDIUM,
                    RecommendedAction.ESCALATE_TO_HUMAN,
                    "A security guardrail was triggered. "
                            + "The request requires human review before proceeding."
                            + " Findings: " + guardrail.findings(),
                    guardrail.trustedDocuments().stream()
                            .map(KnowledgeDocumentResponse::documentId)
                            .toList()
            );
        }

        List<String> documentIds = retrievedDocuments.stream()
                .map(KnowledgeDocumentResponse::documentId)
                .toList();

        List<String> recommendedActions = List.of(
                "RECOMMENDED:" + response.recommendedAction().name()
        );

        String guardrailTrace = formatGuardrailResult(guardrail);

        String finalStatus =
                response.recommendedAction()
                        == RecommendedAction.ESCALATE_TO_HUMAN
                        ? "ESCALATION_REQUIRED"
                        : "COMPLETED";

        traceService.record(
                ticketId,
                "TRIAGE",
                documentIds,
                recommendedActions,
                guardrailTrace,
                finalStatus
        );

        return response;
    }

    private String formatGuardrailResult(GuardrailResult result) {

        if (result.findings().isEmpty()) {
            return "PASSED";
        }

        return result.status().name()
                + ":"
                + String.join(
                ",",
                result.findings().stream()
                        .map(Enum::name)
                        .toList()
        );
    }
}
