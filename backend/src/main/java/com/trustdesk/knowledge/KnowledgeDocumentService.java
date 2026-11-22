package com.trustdesk.knowledge;

import com.trustdesk.knowledge.dto.KnowledgeDocumentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class KnowledgeDocumentService {

    private final KnowledgeDocumentRepository repository;

    public KnowledgeDocumentService(
            KnowledgeDocumentRepository repository) {
        this.repository = repository;
    }

    public List<KnowledgeDocumentResponse> getAllDocuments() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public KnowledgeDocumentResponse getDocumentById(
            String documentId) {

        KnowledgeDocument document = repository.findById(documentId)
                .orElseThrow(() -> new KnowledgeDocumentNotFoundException(
                        documentId));

        return toResponse(document);
    }

    public List<KnowledgeDocumentResponse> getDocumentsByCategory(
            String category) {

        return repository.findByCategory(category)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private KnowledgeDocumentResponse toResponse(
            KnowledgeDocument document) {

        return new KnowledgeDocumentResponse(
                document.getDocumentId(),
                document.getTitle(),
                document.getCategory(),
                document.getContent(),
                document.getSourceFile()
        );
    }

    public List<KnowledgeDocumentResponse> searchDocuments(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException(
                    "Search keyword cannot be empty");
        }

        return repository.searchByKeyword(keyword.trim())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<KnowledgeDocumentResponse> searchByTicketText(
            String ticketText) {

        if (ticketText == null || ticketText.isBlank()) {
            throw new IllegalArgumentException(
                    "Ticket text cannot be empty");
        }

        return repository.searchByTicketText(ticketText.trim())
                .stream()
                .map(this::toResponse)
                .toList();
    }
}