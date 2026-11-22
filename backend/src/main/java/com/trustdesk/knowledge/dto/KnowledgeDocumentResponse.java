package com.trustdesk.knowledge.dto;

public record KnowledgeDocumentResponse(
        String documentId,
        String title,
        String category,
        String content,
        String sourceFile
) {
}