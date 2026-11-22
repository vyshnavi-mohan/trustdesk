CREATE TABLE support_actions (
                                 action_id VARCHAR(50) PRIMARY KEY,
                                 ticket_id VARCHAR(50) NOT NULL,
                                 action_type VARCHAR(50) NOT NULL,
                                 status VARCHAR(30) NOT NULL,
                                 parameters_json TEXT,
                                 idempotency_key VARCHAR(100) NOT NULL,
                                 reason TEXT NOT NULL,
                                 requires_human_approval BOOLEAN NOT NULL DEFAULT FALSE,
                                 approved_by VARCHAR(100),
                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                 CONSTRAINT fk_support_actions_ticket
                                     FOREIGN KEY (ticket_id)
                                         REFERENCES tickets(ticket_id),

                                 CONSTRAINT uk_support_actions_idempotency_key
                                     UNIQUE (idempotency_key)
);

CREATE INDEX idx_support_actions_ticket_id
    ON support_actions(ticket_id);

CREATE INDEX idx_support_actions_status
    ON support_actions(status);
