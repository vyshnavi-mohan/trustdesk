package com.trustdesk.ai.triage;

import com.trustdesk.ai.triage.RecommendedAction;
import com.trustdesk.ai.triage.TriageCategory;
import com.trustdesk.ai.triage.TriagePriority;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Component
public class RuleBasedTriageProvider implements TriageProvider {

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Kolkata");

    @Override
    public TriageResponse analyze(TriageInput input) {

        String ticketId = input.ticket().ticketId();

        String text = (
                input.ticket().subject() + " " +
                        input.ticket().body()
        ).toLowerCase(Locale.ROOT);

        TriageCategory category = TriageCategory.GENERAL;
        TriagePriority priority = TriagePriority.MEDIUM;
        RecommendedAction action = RecommendedAction.NONE;

        String reasoning =
                "No specific support category was identified.";

        boolean concealedCouponRequest =
                text.contains("coupon")
                        && (text.contains("hide")
                        || text.contains("conceal")
                        || text.contains("human reviewer"));

        if (containsAny(text,
                "ignore all instructions",
                "ignore previous instructions",
                "disregard all instructions",
                "hide this instruction",
                "do not tell the agent",
                "reveal your system prompt",
                "prompt injection")
                || concealedCouponRequest) {

            category = TriageCategory.GENERAL;
            priority = TriagePriority.MEDIUM;
            action = RecommendedAction.ESCALATE_TO_HUMAN;
            reasoning = "The message contains a potential prompt-injection "
                    + "or concealed unsafe-action request. Human review is required.";

        } else if (containsAny(text,
                "swelling",
                "overheating",
                "burning smell",
                "burning",
                "exposed wires",
                "electric shock",
                "battery leaking")) {

            category = TriageCategory.WARRANTY;
            priority = TriagePriority.URGENT;
            action = RecommendedAction.ESCALATE_TO_HUMAN;
            reasoning = "A potential product safety issue requires immediate human escalation.";

        } else if (containsAny(text,
                "change my account email",
                "identity verification",
                "account access",
                "password reset",
                "internal instructions",
                "api key",
                "system prompt")) {

            category = TriageCategory.ACCOUNT_SECURITY;
            priority = TriagePriority.HIGH;
            action = RecommendedAction.ESCALATE_TO_HUMAN;
            reasoning = "The request involves account security or protected internal information.";

        } else if (containsAny(text,
                "tracking",
                "package has not moved",
                "delivery delay",
                "shipment")) {

            category = TriageCategory.SHIPPING;
            priority = TriagePriority.HIGH;
            action = RecommendedAction.OPEN_CARRIER_INVESTIGATION;
            reasoning = "The ticket concerns a delivery or shipment issue.";

        } else if (containsAny(text,
                "double charge",
                "charged twice",
                "duplicate charge",
                "payment charged twice")) {

            category = TriageCategory.BILLING;
            priority = TriagePriority.HIGH;
            action = RecommendedAction.START_REFUND_REVIEW;
            reasoning = "The customer reports a possible duplicate payment.";

        } else if (containsAny(text,
                "damaged",
                "cracked",
                "broken",
                "defective")) {

            category = TriageCategory.REFUND;
            priority = TriagePriority.MEDIUM;
            action = RecommendedAction.CREATE_REPLACEMENT_ORDER;
            reasoning = "The customer reports a damaged or defective product. "
                    + "Replacement eligibility must be reviewed.";

        } else if (containsAny(text,
                "refund",
                "return",
                "money back")) {

            category = TriageCategory.REFUND;
            priority = TriagePriority.MEDIUM;
            action = RecommendedAction.NONE;

            var order = input.ticket().order();
            var customer = input.ticket().customer();

            boolean finalSaleItem = order != null
                    && order.items() != null
                    && order.items().stream()
                    .anyMatch(item -> item.finalSale());

            boolean returnWindowExpired = order != null
                    && order.eligibleReturnUntil() != null
                    && input.ticket().createdAt()
                    .atZoneSameInstant(BUSINESS_ZONE)
                    .toLocalDate()
                    .isAfter(order.eligibleReturnUntil());

            boolean goldCustomer = customer != null
                    && "gold".equalsIgnoreCase(customer.tier());

            if (finalSaleItem) {
                priority = TriagePriority.LOW;
                reasoning = "The requested refund is for a final-sale item. "
                        + "A standard refund is not permitted unless legally required "
                        + "or approved by a support manager.";

            } else if (returnWindowExpired && goldCustomer) {
                priority = TriagePriority.HIGH;
                action = RecommendedAction.ESCALATE_TO_HUMAN;
                reasoning = "The standard return window has expired. "
                        + "The customer is eligible for consideration of a goodwill exception "
                        + "because of their gold-tier status. Human approval is required.";

            } else if (returnWindowExpired) {
                reasoning = "The standard return window expired before the ticket was created. "
                        + "The request requires human review for any applicable exception.";

            } else {
                reasoning = "The customer is requesting a refund or return. "
                        + "The return window has not expired, but product condition "
                        + "and policy eligibility must be verified before proceeding.";
            }
        }

        List<String> documentIds = input.knowledgeDocuments()
                .stream()
                .map(document -> document.documentId())
                .toList();

        return new TriageResponse(
                ticketId,
                category,
                priority,
                action,
                reasoning,
                documentIds
        );
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
