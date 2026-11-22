package com.trustdesk.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "knowledge_documents")
public class KnowledgeDocument {

    @Id
    @Column(name = "document_id")
    private String documentId;

    private String title;

    private String category;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "source_file", nullable = false)
    private String sourceFile;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected KnowledgeDocument() {
    }

    public KnowledgeDocument(
            String documentId,
            String title,
            String category,
            String content,
            String sourceFile) {

        this.documentId = documentId;
        this.title = title;
        this.category = category;
        this.content = content;
        this.sourceFile = sourceFile;
        this.createdAt = OffsetDateTime.now();
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getContent() {
        return content;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}