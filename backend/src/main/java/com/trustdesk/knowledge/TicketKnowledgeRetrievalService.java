package com.trustdesk.knowledge;

import com.trustdesk.knowledge.dto.KnowledgeDocumentResponse;
import com.trustdesk.ticket.Ticket;
import com.trustdesk.ticket.TicketNotFoundException;
import com.trustdesk.ticket.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TicketKnowledgeRetrievalService {

    private final TicketRepository ticketRepository;
    private final KnowledgeDocumentService knowledgeDocumentService;

    public TicketKnowledgeRetrievalService(
            TicketRepository ticketRepository,
            KnowledgeDocumentService knowledgeDocumentService) {
        this.ticketRepository = ticketRepository;
        this.knowledgeDocumentService = knowledgeDocumentService;
    }

    public List<KnowledgeDocumentResponse> retrieveForTicket(
            String ticketId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        String ticketText = ticket.getSubject() + " " + ticket.getBody();

        return knowledgeDocumentService.searchByTicketText(ticketText);
    }
}