package com.trustdesk.ticket;

import com.trustdesk.customer.Customer;
import com.trustdesk.order.Order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @Column(name = "ticket_id", length = 50)
    private String ticketId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false, length = 30)
    private String channel;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false, length = 30)
    private String status;

    protected Ticket() {
        // Required by JPA
    }

    public Ticket(
            String ticketId,
            Customer customer,
            Order order,
            String channel,
            String subject,
            String body,
            OffsetDateTime createdAt,
            String status
    ) {
        this.ticketId = ticketId;
        this.customer = customer;
        this.order = order;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
        this.createdAt = createdAt;
        this.status = status;
    }

    public String getTicketId() {
        return ticketId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Order getOrder() {
        return order;
    }

    public String getChannel() {
        return channel;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
    }
}
