package com.trustdesk.order;

import com.trustdesk.customer.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "order_id", length = 50)
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(
            mappedBy = "order",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL
    )
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "placed_at", nullable = false)
    private LocalDate placedAt;

    @Column(name = "delivered_at")
    private LocalDate deliveredAt;

    @Column(name = "eligible_return_until")
    private LocalDate eligibleReturnUntil;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_status", nullable = false, length = 30)
    private String paymentStatus;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    protected Order() {
        // Required by JPA
    }

    public Order(
            String orderId,
            Customer customer,
            String status,
            LocalDate placedAt,
            LocalDate deliveredAt,
            LocalDate eligibleReturnUntil,
            BigDecimal total,
            String currency,
            String paymentStatus,
            String trackingNumber
    ) {
        this.orderId = orderId;
        this.customer = customer;
        this.status = status;
        this.placedAt = placedAt;
        this.deliveredAt = deliveredAt;
        this.eligibleReturnUntil = eligibleReturnUntil;
        this.total = total;
        this.currency = currency;
        this.paymentStatus = paymentStatus;
        this.trackingNumber = trackingNumber;
    }

    public String getOrderId() {
        return orderId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getPlacedAt() {
        return placedAt;
    }

    public LocalDate getDeliveredAt() {
        return deliveredAt;
    }

    public LocalDate getEligibleReturnUntil() {
        return eligibleReturnUntil;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }
}
