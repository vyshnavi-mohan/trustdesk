package com.trustdesk.ai.reply;

import com.trustdesk.ai.guardrail.GuardrailFinding;
import com.trustdesk.ai.guardrail.GuardrailResult;
import com.trustdesk.ai.guardrail.GuardrailService;
import com.trustdesk.ai.triage.RecommendedAction;
import com.trustdesk.ai.triage.TriageCategory;
import com.trustdesk.ai.triage.TriageInput;
import com.trustdesk.ai.triage.TriagePriority;
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
    private final GuardrailService guardrailService;

    public TicketReplyService(
            TicketService ticketService,
            TicketKnowledgeRetrievalService knowledgeRetrievalService,
            TriageProvider triageProvider,
            ReplyGenerationProvider replyGenerationProvider,
            AiExecutionTraceService traceService,
            GuardrailService guardrailService) {

        this.ticketService = ticketService;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.triageProvider = triageProvider;
        this.replyGenerationProvider = replyGenerationProvider;
        this.traceService = traceService;
        this.guardrailService = guardrailService;
    }

    @Transactional(readOnly = true)
    public ReplyGenerationResponse generateDraftReply(String ticketId) {

        TicketResponse ticket =
                ticketService.getTicketById(ticketId);

        // Preserve original retrieval results for audit history.
        List<KnowledgeDocumentResponse> retrievedDocuments =
                knowledgeRetrievalService.retrieveForTicket(ticketId);

        GuardrailResult guardrail =
                guardrailService.evaluate(ticket, retrievedDocuments);

        // Never expose untrusted documents to either provider.
        List<KnowledgeDocumentResponse> trustedDocuments =
                guardrail.trustedDocuments();

        TriageResponse triage = triageProvider.analyze(
                new TriageInput(ticket, trustedDocuments)
        );

        ReplyGenerationResponse response;

        if (guardrail.requiresHumanReview()) {

            boolean accountSecurityIssue = guardrail.findings().contains(
                    GuardrailFinding.UNVERIFIED_ACCOUNT_CHANGE
            );

            TriageCategory category = accountSecurityIssue
                    ? TriageCategory.ACCOUNT_SECURITY
                    : TriageCategory.GENERAL;

            triage = new TriageResponse(
                    ticketId,
                    category,
                    accountSecurityIssue
                            ? TriagePriority.HIGH
                            : TriagePriority.MEDIUM,
                    RecommendedAction.ESCALATE_TO_HUMAN,
                    "A security guardrail was triggered. "
                            + "Human review is required.",
                    trustedDocuments.stream()
                            .map(KnowledgeDocumentResponse::documentId)
                            .toList()
            );

            // Do not invoke the normal reply-generation provider.
            response = createGuardrailEscalationResponse(
                    ticketId,
                    guardrail
            );

        } else {

            ReplyGenerationInput input = new ReplyGenerationInput(
                    ticket,
                    triage,
                    trustedDocuments
            );

            response = replyGenerationProvider.generate(input);
        }

        List<String> documentIds = retrievedDocuments.stream()
                .map(KnowledgeDocumentResponse::documentId)
                .toList();

        List<String> recommendedActions = List.of(
                "RECOMMENDED:" + triage.recommendedAction().name()
        );

        String guardrailTrace = formatGuardrailResult(guardrail);

        traceService.record(
                ticketId,
                "DRAFT_REPLY",
                documentIds,
                recommendedActions,
                guardrailTrace,
                response.status().name()
        );

        return response;
    }

    private ReplyGenerationResponse createGuardrailEscalationResponse(
            String ticketId,
            GuardrailResult guardrail) {

        String draftReply;

        if (guardrail.findings().contains(
                GuardrailFinding.UNVERIFIED_ACCOUNT_CHANGE)) {

            draftReply = "We cannot process this account change until "
                    + "your identity has been verified. "
                    + "Your request has been referred to our support team "
                    + "for further review.";

        } else if (guardrail.findings().contains(
                GuardrailFinding.SECRET_DISCLOSURE_REQUEST)) {

            draftReply = "We cannot provide confidential internal information, "
                    + "system instructions, credentials or API keys. "
                    + "Your request has been referred to our support team "
                    + "for review.";

        } else if (guardrail.findings().contains(
                GuardrailFinding.CONCEALED_COUPON_REQUEST)) {

            draftReply = "We cannot bypass approval requirements or process "
                    + "concealed coupon requests. "
                    + "Your request has been referred to our support team "
                    + "for review.";

        } else {

            draftReply = "Your request requires additional review by our "
                    + "support team before we can proceed. "
                    + "No restricted action has been performed.";
        }

        return new ReplyGenerationResponse(
                ticketId,
                ReplyGenerationResponse.ReplyStatus.ESCALATION_REQUIRED,
                draftReply,
                List.of(),
                "Reply generation was stopped by a security guardrail. "
                        + "Findings: " + guardrail.findings()
        );
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
