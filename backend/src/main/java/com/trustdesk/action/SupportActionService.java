package com.trustdesk.action;

import com.trustdesk.ai.triage.TriageInput;
import com.trustdesk.ai.triage.TriageProvider;
import com.trustdesk.ai.triage.TriageResponse;
import com.trustdesk.knowledge.TicketKnowledgeRetrievalService;
import com.trustdesk.ticket.Ticket;
import com.trustdesk.ticket.TicketRepository;
import com.trustdesk.ticket.TicketNotFoundException;
import com.trustdesk.ticket.TicketService;
import com.trustdesk.ticket.dto.TicketResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SupportActionService {

    private final TicketRepository ticketRepository;
    private final TicketService ticketService;
    private final TicketKnowledgeRetrievalService knowledgeRetrievalService;
    private final TriageProvider triageProvider;
    private final ActionValidationService validationService;
    private final SupportActionRepository actionRepository;
    private final ObjectMapper objectMapper;
    private final ActionExecutor actionExecutor;

    public SupportActionService(
            TicketRepository ticketRepository,
            TicketService ticketService,
            TicketKnowledgeRetrievalService knowledgeRetrievalService,
            TriageProvider triageProvider,
            ActionValidationService validationService,
            SupportActionRepository actionRepository,
            ObjectMapper objectMapper,
            ActionExecutor actionExecutor) {
        this.ticketRepository = ticketRepository;
        this.ticketService = ticketService;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.triageProvider = triageProvider;
        this.validationService = validationService;
        this.actionRepository = actionRepository;
        this.objectMapper = objectMapper;
        this.actionExecutor = actionExecutor;
    }

    @Transactional
    public ActionResponse submitAction(ActionRequest request) {

        if (request == null) {
            throw new ActionValidationException(
                    "Action request is required.");
        }

        if (request.ticketId() == null || request.ticketId().isBlank()) {
            throw new ActionValidationException(
                    "Ticket ID is required.");
        }

        if (request.actionType() == null) {
            throw new ActionValidationException(
                    "Action type is required.");
        }

        Ticket ticket = ticketRepository.findById(request.ticketId())
                .orElseThrow(() ->
                        new TicketNotFoundException(request.ticketId()));

        TicketResponse ticketResponse =
                ticketService.getTicketById(request.ticketId());

        var documents =
                knowledgeRetrievalService.retrieveForTicket(request.ticketId());

        TriageResponse triage = triageProvider.analyze(
                new TriageInput(ticketResponse, documents));

        if (triage.recommendedAction() == null
                || !triage.recommendedAction().name()
                .equals(request.actionType().name())) {
            throw new ActionValidationException(
                    "The requested action does not match the action recommended by ticket triage."
            );
        }

        ActionDefinition definition =
                validationService.validate(request, triage.category());

        String idempotencyKey =
                request.parameters().get("idempotency_key");

        var existingAction =
                actionRepository.findByIdempotencyKey(idempotencyKey);

        if (existingAction.isPresent()) {

            SupportAction existing = existingAction.get();

            boolean sameRequest =
                    existing.getTicket().getTicketId().equals(request.ticketId())
                            && existing.getActionType() == request.actionType()
                            && existing.getParametersJson().equals(
                            objectMapper.writeValueAsString(request.parameters()))
                            && existing.getReason().equals(request.reason());

            if (!sameRequest) {
                throw new ActionValidationException(
                        "Idempotency key has already been used for a different action request.");
            }

            return toResponse(existing);
        }

        String parametersJson =
                objectMapper.writeValueAsString(request.parameters());

        ActionStatus initialStatus = definition.requiresHumanApproval()
                ? ActionStatus.PENDING_APPROVAL
                : ActionStatus.APPROVED;

        SupportAction action = new SupportAction(
                UUID.randomUUID().toString(),
                ticket,
                request.actionType(),
                initialStatus,
                parametersJson,
                idempotencyKey,
                request.reason(),
                definition.requiresHumanApproval()
        );

        SupportAction savedAction = actionRepository.save(action);

        return toResponse(savedAction);
    }


    @Transactional
    public ActionResponse reviewAction(
            String actionId,
            ActionApprovalRequest request) {

        if (request == null || request.decision() == null
                || request.decision().isBlank()) {
            throw new ActionValidationException(
                    "Approval decision is required.");
        }

        SupportAction action = actionRepository.findByIdWithLock(actionId)
                .orElseThrow(() -> new ActionValidationException(
                        "Action not found: " + actionId));

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new ActionValidationException(
                    "Authenticated approver identity is required.");
        }

        String approver = authentication.getName();

        switch (request.decision().trim().toUpperCase()) {
            case "APPROVE" -> action.approve(approver);
            case "REJECT" -> action.reject();
            default -> throw new ActionValidationException(
                    "Decision must be APPROVE or REJECT.");
        }

        return toResponse(action);
    }


    @Transactional
    public ActionResponse executeAction(String actionId) {

        SupportAction action = actionRepository.findByIdWithLock(actionId)
                .orElseThrow(() -> new ActionValidationException(
                        "Action not found: " + actionId));

        // Repeated execution requests must not execute twice.
        if (action.getStatus() == ActionStatus.EXECUTED) {
            return toResponse(action);
        }

        if (action.getStatus() != ActionStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only approved actions can be executed.");
        }

        try {
            var jsonNode = objectMapper.readTree(
                    action.getParametersJson()
            );

            Map<String, String> parameters = new LinkedHashMap<>();

            jsonNode.properties().forEach(entry ->
                    parameters.put(entry.getKey(), entry.getValue().asText())
            );

            actionExecutor.execute(
                    action.getActionType(),
                    parameters
            );

            action.markExecuted();

            return toResponse(action);

        } catch (RuntimeException exception) {
            action.markFailed();
            return toResponse(action);
        }
    }

    private ActionResponse toResponse(SupportAction action) {
        try {
            var jsonNode = objectMapper.readTree(
                    action.getParametersJson()
            );

            Map<String, String> parameters = new LinkedHashMap<>();

            jsonNode.properties().forEach(entry ->
                    parameters.put(entry.getKey(), entry.getValue().asText())
            );

            return new ActionResponse(
                    action.getActionId(),
                    action.getTicket().getTicketId(),
                    action.getActionType(),
                    action.getStatus(),
                    parameters,
                    action.getReason(),
                    action.isRequiresHumanApproval(),
                    action.getCreatedAt(),
                    action.getUpdatedAt()
            );

        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Unable to read stored action parameters.",
                    exception
            );
        }
    }
}
