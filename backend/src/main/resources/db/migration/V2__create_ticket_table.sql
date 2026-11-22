CREATE TABLE tickets (
                         ticket_id VARCHAR(50) PRIMARY KEY,
                         customer_id VARCHAR(50) NOT NULL,
                         order_id VARCHAR(50),
                         channel VARCHAR(30) NOT NULL,
                         subject VARCHAR(255) NOT NULL,
                         body TEXT NOT NULL,
                         created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                         status VARCHAR(30) NOT NULL,

                         CONSTRAINT fk_tickets_customer
                             FOREIGN KEY (customer_id)
                                 REFERENCES customers(customer_id),

                         CONSTRAINT fk_tickets_order
                             FOREIGN KEY (order_id)
                                 REFERENCES orders(order_id)
);

CREATE INDEX idx_tickets_customer_id
    ON tickets(customer_id);

CREATE INDEX idx_tickets_order_id
    ON tickets(order_id);

CREATE INDEX idx_tickets_created_at
    ON tickets(created_at);
