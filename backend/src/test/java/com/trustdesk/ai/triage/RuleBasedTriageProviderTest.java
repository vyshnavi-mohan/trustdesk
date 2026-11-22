package com.trustdesk.ai.triage;

import com.trustdesk.ticket.dto.TicketResponse;
import com.trustdesk.knowledge.dto.KnowledgeDocumentResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleBasedTriageProviderTest {

    private final RuleBasedTriageProvider provider =
            new RuleBasedTriageProvider();

    @Test
    void shouldEscalateExpiredReturnForGoldCustomer() {

        TicketResponse ticket = createTicket(
                "gold",
                false,
                LocalDate.of(2026, 6, 21),
                OffsetDateTime.parse("2026-07-02T18:10:00+05:30")
        );

        TriageResponse response = provider.analyze(
                new TriageInput(ticket, List.of())
        );

        assertEquals(TriageCategory.REFUND, response.category());
        assertEquals(TriagePriority.HIGH, response.priority());
        assertEquals(
                RecommendedAction.ESCALATE_TO_HUMAN,
                response.recommendedAction()
        );
    }

    @Test
    void shouldRejectStandardRefundForFinalSaleItem() {

        TicketResponse ticket = createTicket(
                "gold",
                true,
                LocalDate.of(2026, 6, 21),
                OffsetDateTime.parse("2026-07-02T18:10:00+05:30")
        );

        TriageResponse response = provider.analyze(
                new TriageInput(ticket, List.of())
        );

        assertEquals(TriageCategory.REFUND, response.category());
        assertEquals(TriagePriority.LOW, response.priority());
        assertEquals(RecommendedAction.NONE, response.recommendedAction());
    }

    private TicketResponse createTicket(
            String tier,
            boolean finalSale,
            LocalDate eligibleReturnUntil,
            OffsetDateTime createdAt
    ) {
        var customer = new TicketResponse.CustomerDetails(
                "cus_test",
                "Test Customer",
                "test@example.com",
                tier,
                "IN",
                true,
                List.of()
        );

        var item = new TicketResponse.OrderItemDetails(
                "SKU-TEST",
                "Test Product",
                1,
                "electronics",
                finalSale
        );

        var order = new TicketResponse.OrderDetails(
                "ord_test",
                "delivered",
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 14),
                eligibleReturnUntil,
                new BigDecimal("4999.00"),
                "INR",
                "paid",
                "TRACK-TEST",
                List.of(item)
        );

        return new TicketResponse(
                "tkt_test",
                "email",
                "Please refund my purchase",
                "I changed my mind. Please refund my order.",
                createdAt,
                "open",
                customer,
                order
        );
    }

    @Test
    void shouldNotTreatReturnWindowAsExpiredWhenTicketWasCreatedWithinWindow() {

        TicketResponse ticket = createTicket(
                "standard",
                false,
                LocalDate.of(2026, 6, 21),
                OffsetDateTime.parse("2026-06-20T18:10:00+05:30")
        );

        TriageResponse response = provider.analyze(
                new TriageInput(ticket, List.of())
        );

        assertEquals(TriageCategory.REFUND, response.category());
        assertEquals(TriagePriority.MEDIUM, response.priority());
        assertEquals(RecommendedAction.NONE, response.recommendedAction());
    }
}
