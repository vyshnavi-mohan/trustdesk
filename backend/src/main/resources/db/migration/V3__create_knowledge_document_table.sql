CREATE TABLE knowledge_documents (
                                     document_id VARCHAR(50) PRIMARY KEY,
                                     title VARCHAR(255) NOT NULL,
                                     category VARCHAR(100) NOT NULL,
                                     content TEXT NOT NULL,
                                     source_file VARCHAR(255) NOT NULL,
                                     created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_knowledge_documents_category
    ON knowledge_documents(category);