package com.trustdesk.trace;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traces")
public class AiExecutionTraceController {

    private final AiExecutionTraceService traceService;

    public AiExecutionTraceController(AiExecutionTraceService traceService) {
        this.traceService = traceService;
    }

    @GetMapping("/tickets/{ticketId}")
    public List<AiExecutionTrace> getTracesForTicket(
            @PathVariable String ticketId) {
        return traceService.getTracesForTicket(ticketId);
    }
}
