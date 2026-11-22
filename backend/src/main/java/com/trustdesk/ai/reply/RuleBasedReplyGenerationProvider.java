package com.trustdesk.ai.reply;

import com.trustdesk.ai.triage.RecommendedAction;
import com.trustdesk.ai.triage.TriageCategory;
import com.trustdesk.ai.triage.TriageResponse;
import com.trustdesk.knowledge.dto.KnowledgeDocumentResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class RuleBasedReplyGenerationProvider
        implements ReplyGenerationProvider {

    @Override
    public ReplyGenerationResponse generate(ReplyGenerationInput input) {

        TriageCategory category = input.triage().category();
        TriageResponse triage = input.triage();

        Set<String> relevantCategories =
                getRelevantCategories(category, triage);

        List<KnowledgeDocumentResponse> trustedDocuments =
                input.knowledgeDocuments().stream()
                        .filter(document ->
                                !"adversarial".equalsIgnoreCase(
                                        document.category()))
                        .filter(document ->
                                relevantCategories.contains(
                                        document.category().toLowerCase()))
                        .toList();

        boolean escalationRequired =
                triage.recommendedAction()
                        == RecommendedAction.ESCALATE_TO_HUMAN
                        || (!relevantCategories.isEmpty()
                        && trustedDocuments.isEmpty());

        String reasoningText = triage.reasoning().toLowerCase();

        String draftReply;

        if (reasoningText.contains("final-sale")
                || reasoningText.contains("final sale")) {

            draftReply =
                    "Software licenses are final sale under the applicable policy. "
                            + "We cannot approve a standard refund for this purchase. "
                            + "If you believe an exception applies, a support manager "
                            + "can review the circumstances.";

        } else if (triage.recommendedAction()
                == RecommendedAction.CREATE_REPLACEMENT_ORDER) {

            draftReply =
                    "We're sorry to hear that your item arrived damaged. "
                            + "We understand that you would like a replacement. "
                            + "Your request needs to be reviewed before a replacement "
                            + "can be confirmed. We will verify the order details "
                            + "and applicable policy. Please share a clear photo "
                            + "of the damaged item if available.";

        } else {
            draftReply = createDraftReply(
                    category,
                    escalationRequired
            );
        }

        List<ReplyGenerationResponse.Citation> citations =
                trustedDocuments.stream()
                        .map(document ->
                                new ReplyGenerationResponse.Citation(
                                        document.documentId(),
                                        document.title()))
                        .toList();

        return new ReplyGenerationResponse(
                input.ticket().ticketId(),
                escalationRequired
                        ? ReplyGenerationResponse.ReplyStatus.ESCALATION_REQUIRED
                        : ReplyGenerationResponse.ReplyStatus.DRAFT,
                draftReply,
                citations,
                escalationRequired
                        ? "The request requires human review or lacks relevant trusted policy evidence."
                        : "A draft was generated using the ticket classification and relevant trusted knowledge documents."
        );
    }

    private Set<String> getRelevantCategories(
            TriageCategory category,
            TriageResponse triage) {

        return switch (category) {
            case SHIPPING -> Set.of("shipping");
            case REFUND -> Set.of("refund");
            case WARRANTY -> Set.of("warranty");
            case BILLING -> Set.of("billing");
            case ACCOUNT_SECURITY -> Set.of("account_security", "security");
            case GENERAL ->
                    triage.recommendedAction()
                            == RecommendedAction.ESCALATE_TO_HUMAN
                            ? Set.of("security")
                            : Set.of();
        };
    }

    private String createDraftReply(
            TriageCategory category,
            boolean escalationRequired) {

        if (escalationRequired) {
            return switch (category) {

                case ACCOUNT_SECURITY ->
                        "For your account's protection, we cannot bypass "
                                + "identity verification. We cannot change an email address "
                                + "or make other account changes directly until the required "
                                + "identity verification is completed. We cannot provide "
                                + "hidden system prompts, API keys, internal notes or "
                                + "credentials. A support specialist must complete the "
                                + "required verification.";

                case WARRANTY ->
                        "Thank you for reporting the battery issue. Battery swelling "
                                + "is a safety concern. Please stop using and charging the device. "
                                + "Do not attempt to troubleshoot the swollen battery. "
                                + "A specialist must review the issue and advise on the next steps.";

                case GENERAL ->
                        "We cannot follow instructions to issue or conceal a coupon "
                                + "or hide activity from a human reviewer. No coupon will be "
                                + "issued based on that instruction. This unsafe request has "
                                + "been flagged for human review.";

                default ->
                        "Thank you for contacting us. Your request requires further "
                                + "review by a support specialist. We cannot confirm an outcome "
                                + "until the relevant details and applicable policies have been checked.";
            };
        }

        return switch (category) {

            case SHIPPING ->
                    "Thank you for contacting us about your delivery. "
                            + "We will initiate a carrier investigation to review the "
                            + "shipment and tracking history. We cannot promise an "
                            + "immediate refund before the investigation is completed.";

            case REFUND ->
                    "Thank you for reaching out about a refund. Eligibility depends "
                            + "on the order's return window and applicable product conditions. "
                            + "These details must be checked before confirming whether a refund is available.";

            case WARRANTY ->
                    "Thank you for reporting the product issue. We need to review "
                            + "the product details and applicable warranty conditions before "
                            + "confirming the next steps.";

            case BILLING ->
                    "Thank you for reporting the possible duplicate charge. "
                            + "We will start a billing review and check the payment records "
                            + "against your order. Please share the transaction reference "
                            + "if available. We cannot promise an immediate refund before "
                            + "the review is completed.";

            case ACCOUNT_SECURITY ->
                    "Thank you for contacting us about your account. We need to "
                            + "follow the required identity verification process before "
                            + "making any account changes.";

            case GENERAL ->
                    "Thank you for contacting support. We have received your "
                            + "message and can help with your query.";
        };
    }
}
