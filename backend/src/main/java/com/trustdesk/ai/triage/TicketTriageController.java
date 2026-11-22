package com.trustdesk.ai.triage;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketTriageController {

    private final TicketTriageService triageService;

    public TicketTriageController(TicketTriageService triageService) {
        this.triageService = triageService;
    }

    @PostMapping("/{ticketId}/triage")
    public ResponseEntity<TriageResponse> triageTicket(
            @PathVariable String ticketId) {

        return ResponseEntity.ok(
                triageService.triageTicket(ticketId)
        );
    }
}
