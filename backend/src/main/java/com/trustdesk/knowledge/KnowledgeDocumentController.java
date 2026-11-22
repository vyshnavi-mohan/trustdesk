package com.trustdesk.knowledge;

import com.trustdesk.knowledge.dto.KnowledgeDocumentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService service;
    private final TicketKnowledgeRetrievalService ticketKnowledgeRetrievalService;

    public KnowledgeDocumentController(
            KnowledgeDocumentService service,
            TicketKnowledgeRetrievalService ticketKnowledgeRetrievalService) {

        this.service = service;
        this.ticketKnowledgeRetrievalService = ticketKnowledgeRetrievalService;
    }

    @GetMapping
    public ResponseEntity<List<KnowledgeDocumentResponse>> getAllDocuments(
            @RequestParam(required = false) String category) {

        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(
                    service.getDocumentsByCategory(category));
        }

        return ResponseEntity.ok(service.getAllDocuments());
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<KnowledgeDocumentResponse> getDocumentById(
            @PathVariable String documentId) {

        return ResponseEntity.ok(
                service.getDocumentById(documentId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<KnowledgeDocumentResponse>> searchDocuments(
            @RequestParam String keyword) {

        return ResponseEntity.ok(
                service.searchDocuments(keyword));
    }

    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<List<KnowledgeDocumentResponse>>
    getRelevantDocumentsForTicket(
            @PathVariable String ticketId) {

        return ResponseEntity.ok(
                ticketKnowledgeRetrievalService.retrieveForTicket(ticketId));
    }
}