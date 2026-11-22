CREATE TABLE ai_execution_traces (
                                     trace_id VARCHAR(50) PRIMARY KEY,
                                     ticket_id VARCHAR(50) NOT NULL,
                                     run_type VARCHAR(30) NOT NULL,
                                     retrieved_document_ids TEXT,
                                     tool_actions TEXT,
                                     guardrail_result VARCHAR(50) NOT NULL,
                                     final_status VARCHAR(50) NOT NULL,
                                     created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                     CONSTRAINT fk_ai_trace_ticket
                                         FOREIGN KEY (ticket_id)
                                             REFERENCES tickets(ticket_id)
);

CREATE INDEX idx_ai_traces_ticket_id
    ON ai_execution_traces(ticket_id);

CREATE INDEX idx_ai_traces_created_at
    ON ai_execution_traces(created_at);