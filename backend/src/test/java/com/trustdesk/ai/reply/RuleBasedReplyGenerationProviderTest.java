package com.trustdesk.ai.reply;

import com.trustdesk.ai.triage.RecommendedAction;
import com.trustdesk.ai.triage.TriageCategory;
import com.trustdesk.ai.triage.TriagePriority;
import com.trustdesk.ai.triage.TriageResponse;
import com.trustdesk.knowledge.dto.KnowledgeDocumentResponse;
import com.trustdesk.ticket.dto.TicketResponse;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedReplyGenerationProviderTest {

    private final RuleBasedReplyGenerationProvider provider =
            new RuleBasedReplyGenerationProvider();

    @Test
    void shouldGenerateReplacementAwareDraft() {

        ReplyGenerationResponse response = generate(
                TriageCategory.REFUND,
                RecommendedAction.CREATE_REPLACEMENT_ORDER,
                List.of(document("KB-REFUND-001", "Refund and Return Policy", "refund"))
        );

        assertEquals(ReplyGenerationResponse.ReplyStatus.DRAFT, response.status());
        assertTrue(response.draftReply().toLowerCase().contains("damaged"));
        assertTrue(response.draftReply().toLowerCase().contains("replacement"));
        assertTrue(response.draftReply().toLowerCase().contains("photo"));
        assertEquals("KB-REFUND-001", response.citations().get(0).documentId());
    }

    @Test
    void shouldEscalatePromptInjectionWithoutCitingAdversarialDocument() {

        ReplyGenerationResponse response = generate(
                TriageCategory.GENERAL,
                RecommendedAction.ESCALATE_TO_HUMAN,
                List.of(
                        document("KB-ADVERSARIAL-001", "Vendor Note", "adversarial"),
                        document("KB-COUPON-001", "Coupon Policy", "coupon")
                )
        );

        assertEquals(
                ReplyGenerationResponse.ReplyStatus.ESCALATION_REQUIRED,
                response.status()
        );
        assertTrue(response.citations().stream()
                .noneMatch(citation ->
                        citation.documentId().equals("KB-ADVERSARIAL-001")));
    }

    @Test
    void shouldRequireHumanReviewForAccountSecurity() {

        ReplyGenerationResponse response = generate(
                TriageCategory.ACCOUNT_SECURITY,
                RecommendedAction.ESCALATE_TO_HUMAN,
                List.of(document(
                        "KB-ACCOUNT-001",
                        "Account Security Policy",
                        "account_security"
                ))
        );

        assertEquals(
                ReplyGenerationResponse.ReplyStatus.ESCALATION_REQUIRED,
                response.status()
        );
        assertTrue(response.draftReply().contains("identity verification"));
        assertEquals(1, response.citations().size());
    }

    private ReplyGenerationResponse generate(
            TriageCategory category,
            RecommendedAction action,
            List<KnowledgeDocumentResponse> documents) {

        TicketResponse ticket = new TicketResponse(
                "tkt_test",
                "email",
                "Test ticket",
                "Test ticket body",
                OffsetDateTime.parse("2026-07-02T10:00:00+05:30"),
                "OPEN",
                null,
                null
        );

        TriageResponse triage = new TriageResponse(
                "tkt_test",
                category,
                TriagePriority.HIGH,
                action,
                "Test reasoning",
                List.of()
        );

        return provider.generate(
                new ReplyGenerationInput(ticket, triage, documents)
        );
    }

    private KnowledgeDocumentResponse document(
            String id,
            String title,
            String category) {

        return new KnowledgeDocumentResponse(
                id, title, category, "Test policy content", "test.md"
        );
    }
}
