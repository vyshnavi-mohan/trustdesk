package com.trustdesk.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "final_sale", nullable = false)
    private boolean finalSale;

    protected OrderItem() {
        // Required by JPA
    }

    public OrderItem(
            Order order,
            String sku,
            String name,
            Integer quantity,
            String category,
            boolean finalSale
    ) {
        this.order = order;
        this.sku = sku;
        this.name = name;
        this.quantity = quantity;
        this.category = category;
        this.finalSale = finalSale;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getCategory() {
        return category;
    }

    public boolean isFinalSale() {
        return finalSale;
    }
}
