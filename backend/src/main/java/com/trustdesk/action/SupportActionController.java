package com.trustdesk.action;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/actions")
public class SupportActionController {

    private final SupportActionService supportActionService;

    public SupportActionController(
            SupportActionService supportActionService) {
        this.supportActionService = supportActionService;
    }

    // Get all actions or filter by status
    @GetMapping
    public ResponseEntity<List<ActionResponse>> getActions(
            @RequestParam(required = false) ActionStatus status) {
        return ResponseEntity.ok(
                supportActionService.getActions(status));
    }

    // Get action history for a ticket
    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<List<ActionResponse>> getActionsByTicket(
            @PathVariable String ticketId) {
        return ResponseEntity.ok(
                supportActionService.getActionsByTicket(ticketId));
    }

    @PostMapping
    public ResponseEntity<ActionResponse> submitAction(
            @RequestBody ActionRequest request) {
        ActionResponse response =
                supportActionService.submitAction(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{actionId}/review")
    public ResponseEntity<ActionResponse> reviewAction(
            @PathVariable String actionId,
            @Valid @RequestBody ActionApprovalRequest request) {
        ActionResponse response =
                supportActionService.reviewAction(actionId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{actionId}/execute")
    public ResponseEntity<ActionResponse> executeAction(
            @PathVariable String actionId) {
        ActionResponse response =
                supportActionService.executeAction(actionId);
        return ResponseEntity.ok(response);
    }
}
