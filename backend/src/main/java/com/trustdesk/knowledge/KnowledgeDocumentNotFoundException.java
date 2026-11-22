package com.trustdesk.knowledge;

public class KnowledgeDocumentNotFoundException extends RuntimeException {

    public KnowledgeDocumentNotFoundException(String documentId) {
        super("Knowledge document not found with ID: " + documentId);
    }
}