package com.trustdesk.action;

import com.trustdesk.ai.triage.*;
import com.trustdesk.knowledge.TicketKnowledgeRetrievalService;
import com.trustdesk.ticket.Ticket;
import com.trustdesk.ticket.TicketRepository;
import com.trustdesk.ticket.TicketService;
import com.trustdesk.ticket.dto.TicketResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class SupportActionServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketService ticketService;

    @Mock
    private TicketKnowledgeRetrievalService knowledgeRetrievalService;

    @Mock
    private TriageProvider triageProvider;

    @Mock
    private ActionValidationService validationService;

    @Mock
    private SupportActionRepository actionRepository;

    @Mock
    private ActionExecutor actionExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRejectDifferentRequestUsingExistingIdempotencyKey()
            throws Exception {

        Ticket ticket = mock(Ticket.class);
        when(ticket.getTicketId()).thenReturn("tkt_9001");

        when(ticketRepository.findById("tkt_9001"))
                .thenReturn(Optional.of(ticket));

        TicketResponse ticketResponse = new TicketResponse(
                "tkt_9001",
                "email",
                "Damaged product",
                "Customer reported damaged earbuds.",
                OffsetDateTime.now(),
                "OPEN",
                null,
                null
        );

        when(ticketService.getTicketById("tkt_9001"))
                .thenReturn(ticketResponse);

        when(knowledgeRetrievalService.retrieveForTicket("tkt_9001"))
                .thenReturn(List.of());

        when(triageProvider.analyze(any(TriageInput.class)))
                .thenReturn(new TriageResponse(
                        "tkt_9001",
                        TriageCategory.REFUND,
                        TriagePriority.HIGH,
                        RecommendedAction.CREATE_REPLACEMENT_ORDER,
                        "Damaged product",
                        List.of()
                ));

        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("order_id", "ord_5001");
        parameters.put("sku", "BG-AIRPODS-01");
        parameters.put("idempotency_key", "same-idempotency-key");

        ActionRequest request = new ActionRequest(
                "tkt_9001",
                ActionType.CREATE_REPLACEMENT_ORDER,
                parameters,
                null,
                "Different reason"
        );

        when(validationService.validate(
                request,
                TriageCategory.REFUND
        )).thenReturn(new ActionDefinition(
                ActionType.CREATE_REPLACEMENT_ORDER,
                "Creates a replacement order",
                "medium",
                true,
                List.of("refund", "warranty"),
                List.of("order_id", "sku", "reason", "idempotency_key"),
                null
        ));

        SupportAction existingAction = new SupportAction(
                "existing-action",
                ticket,
                ActionType.CREATE_REPLACEMENT_ORDER,
                ActionStatus.PENDING_APPROVAL,
                objectMapper.writeValueAsString(parameters),
                "same-idempotency-key",
                "Original reason",
                true
        );

        when(actionRepository.findByIdempotencyKey(
                "same-idempotency-key"
        )).thenReturn(Optional.of(existingAction));

        SupportActionService service = new SupportActionService(
                ticketRepository,
                ticketService,
                knowledgeRetrievalService,
                triageProvider,
                validationService,
                actionRepository,
                objectMapper,
                actionExecutor
        );

        // A different reason with the same idempotency key must be rejected.
        assertThrows(
                ActionValidationException.class,
                () -> service.submitAction(request)
        );

// An exact retry with the same key and original payload must succeed.
        ActionRequest retryRequest = new ActionRequest(
                "tkt_9001",
                ActionType.CREATE_REPLACEMENT_ORDER,
                parameters,
                null,
                "Original reason"
        );

        when(validationService.validate(
                retryRequest,
                TriageCategory.REFUND
        )).thenReturn(new ActionDefinition(
                ActionType.CREATE_REPLACEMENT_ORDER,
                "Creates a replacement order",
                "medium",
                true,
                List.of("refund", "warranty"),
                List.of("order_id", "sku", "reason", "idempotency_key"),
                null
        ));

        ActionResponse response = service.submitAction(retryRequest);

        assertEquals("existing-action", response.actionId());
        assertEquals(ActionStatus.PENDING_APPROVAL, response.status());

// Neither request should create a new database record.
        verify(actionRepository, never()).save(any());
    }
}
