package com.trustdesk.ticket.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record TicketResponse(
        String ticketId,
        String channel,
        String subject,
        String body,
        OffsetDateTime createdAt,
        String status,
        CustomerDetails customer,
        OrderDetails order
) {

    public record CustomerDetails(
            String customerId,
            String name,
            String email,
            String tier,
            String country,
            boolean verified,
            List<String> tags
    ) {}

    public record OrderDetails(
            String orderId,
            String status,
            LocalDate placedAt,
            LocalDate deliveredAt,
            LocalDate eligibleReturnUntil,
            BigDecimal total,
            String currency,
            String paymentStatus,
            String trackingNumber,
            List<OrderItemDetails> items
    ) {}

    public record OrderItemDetails(
            String sku,
            String name,
            Integer quantity,
            String category,
            boolean finalSale
    ) {}
}
