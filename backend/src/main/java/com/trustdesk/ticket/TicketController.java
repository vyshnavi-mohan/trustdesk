package com.trustdesk.ticket;

import com.trustdesk.ticket.dto.TicketResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<Page<TicketResponse>> getAllTickets(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(
                    ticketService.getTicketsByStatus(status, pageable));
        }

        return ResponseEntity.ok(ticketService.getAllTickets(pageable));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicketById(
            @PathVariable String ticketId
    ) {
        return ResponseEntity.ok(
                ticketService.getTicketById(ticketId)
        );
    }
}
