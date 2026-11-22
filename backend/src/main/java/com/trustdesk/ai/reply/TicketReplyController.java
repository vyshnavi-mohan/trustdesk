package com.trustdesk.ai.reply;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketReplyController {

    private final TicketReplyService ticketReplyService;

    public TicketReplyController(TicketReplyService ticketReplyService) {
        this.ticketReplyService = ticketReplyService;
    }

    @PostMapping("/{ticketId}/draft-reply")
    public ResponseEntity<ReplyGenerationResponse> generateDraftReply(
            @PathVariable String ticketId) {

        return ResponseEntity.ok(
                ticketReplyService.generateDraftReply(ticketId));
    }
}
