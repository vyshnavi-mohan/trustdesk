package com.trustdesk.customer;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @Column(name = "customer_id", length = 50)
    private String customerId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 30)
    private String tier;

    @Column(nullable = false, length = 10)
    private String country;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @Column(nullable = false)
    private boolean verified;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "customer_tags",
            joinColumns = @JoinColumn(name = "customer_id")
    )
    @Column(name = "tag", nullable = false, length = 100)
    private List<String> tags = new ArrayList<>();

    protected Customer() {
        // Required by JPA
    }

    public Customer(
            String customerId,
            String name,
            String email,
            String tier,
            String country,
            LocalDate createdAt,
            boolean verified,
            List<String> tags
    ) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.tier = tier;
        this.country = country;
        this.createdAt = createdAt;
        this.verified = verified;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getTier() {
        return tier;
    }

    public String getCountry() {
        return country;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public boolean isVerified() {
        return verified;
    }

    public List<String> getTags() {
        return tags;
    }
}
