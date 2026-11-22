package com.trustdesk.ticket;

import com.trustdesk.order.OrderItem;
import com.trustdesk.ticket.dto.TicketResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public List<TicketResponse> getAllTickets() {
        return ticketRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TicketResponse> getTicketsByStatus(String status) {
        return ticketRepository.findByStatus(status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TicketResponse getTicketById(String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        return toResponse(ticket);
    }

    private TicketResponse toResponse(Ticket ticket) {

        var customer = ticket.getCustomer();

        TicketResponse.CustomerDetails customerDetails =
                new TicketResponse.CustomerDetails(
                        customer.getCustomerId(),
                        customer.getName(),
                        customer.getEmail(),
                        customer.getTier(),
                        customer.getCountry(),
                        customer.isVerified(),
                        List.copyOf(customer.getTags())
                );

        TicketResponse.OrderDetails orderDetails = null;

        if (ticket.getOrder() != null) {

            var order = ticket.getOrder();

            List<TicketResponse.OrderItemDetails> items =
                    order.getOrderItems()
                            .stream()
                            .map(this::toOrderItemDetails)
                            .toList();

            orderDetails = new TicketResponse.OrderDetails(
                    order.getOrderId(),
                    order.getStatus(),
                    order.getPlacedAt(),
                    order.getDeliveredAt(),
                    order.getEligibleReturnUntil(),
                    order.getTotal(),
                    order.getCurrency(),
                    order.getPaymentStatus(),
                    order.getTrackingNumber(),
                    items
            );
        }

        return new TicketResponse(
                ticket.getTicketId(),
                ticket.getChannel(),
                ticket.getSubject(),
                ticket.getBody(),
                ticket.getCreatedAt(),
                ticket.getStatus(),
                customerDetails,
                orderDetails
        );
    }

    private TicketResponse.OrderItemDetails toOrderItemDetails(
            OrderItem item
    ) {
        return new TicketResponse.OrderItemDetails(
                item.getSku(),
                item.getName(),
                item.getQuantity(),
                item.getCategory(),
                item.isFinalSale()
        );
    }

    public Page<TicketResponse> getAllTickets(Pageable pageable) {
        return ticketRepository.findAll(pageable)
                .map(this::toResponse);
    }

    public Page<TicketResponse> getTicketsByStatus(
            String status, Pageable pageable) {
        return ticketRepository.findByStatus(status, pageable)
                .map(this::toResponse);
    }
}
