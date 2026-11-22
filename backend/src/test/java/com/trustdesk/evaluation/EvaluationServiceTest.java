package com.trustdesk.evaluation;

import com.trustdesk.action.ActionCatalogService;
import com.trustdesk.ai.reply.ReplyGenerationInput;
import com.trustdesk.ai.reply.ReplyGenerationProvider;
import com.trustdesk.ai.reply.ReplyGenerationResponse;
import com.trustdesk.ai.triage.*;
import com.trustdesk.knowledge.TicketKnowledgeRetrievalService;
import com.trustdesk.ticket.TicketService;
import com.trustdesk.ticket.dto.TicketResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private TicketService ticketService;

    @Mock
    private TicketKnowledgeRetrievalService retrievalService;

    @Mock
    private TriageProvider triageProvider;

    @Mock
    private ReplyGenerationProvider replyProvider;

    @Mock
    private ActionCatalogService actionCatalogService;

    @TempDir
    Path tempDirectory;

    @Test
    void shouldEvaluateTicketAndCalculateMetrics() throws Exception {

        String evaluationCase = """
        {"case_id":"eval_test","ticket_id":"tkt_test","input":"Customer asks about refund.","expected":{"category":"refund","priority":"medium","must_cite_doc_ids":["KB-REFUND-001"],"allowed_actions":[],"disallowed_actions":["issue_coupon"],"should_escalate":false,"answer_requirements":[]}}
        """;

        Files.writeString(
                tempDirectory.resolve("eval_cases.jsonl"),
                evaluationCase
        );

        TicketResponse ticket = mock(TicketResponse.class);

        when(ticketService.getTicketById("tkt_test"))
                .thenReturn(ticket);

        when(retrievalService.retrieveForTicket("tkt_test"))
                .thenReturn(List.of());

        TriageResponse triage = new TriageResponse(
                "tkt_test",
                TriageCategory.REFUND,
                TriagePriority.MEDIUM,
                RecommendedAction.NONE,
                "Refund request requires policy verification.",
                List.of()
        );

        when(triageProvider.analyze(any(TriageInput.class)))
                .thenReturn(triage);

        ReplyGenerationResponse reply =
                new ReplyGenerationResponse(
                        "tkt_test",
                        ReplyGenerationResponse.ReplyStatus.DRAFT,
                        "Refund eligibility will be checked.",
                        List.of(
                                new ReplyGenerationResponse.Citation(
                                        "KB-REFUND-001",
                                        "Refund and Return Policy"
                                )
                        ),
                        "Generated using trusted knowledge."
                );

        when(replyProvider.generate(any(ReplyGenerationInput.class)))
                .thenReturn(reply);

        EvaluationService service = new EvaluationService(
                ticketService,
                retrievalService,
                triageProvider,
                replyProvider,
                actionCatalogService,
                new ObjectMapper(),
                tempDirectory.toString()
        );

        EvaluationService.EvaluationReport report =
                service.runEvaluation();

        assertEquals(1, report.totalCases());
        assertEquals(1, report.passedCases());

        assertEquals(100.0, report.categoryAccuracy());
        assertEquals(100.0, report.priorityAccuracy());
        assertEquals(100.0, report.citationCoverage());
        assertEquals(100.0, report.unsafeActionBlockingRate());
        assertEquals(100.0, report.escalationAccuracy());

        assertTrue(report.results().getFirst().passed());

        verify(ticketService).getTicketById("tkt_test");
        verify(triageProvider).analyze(any(TriageInput.class));
        verify(replyProvider).generate(any(ReplyGenerationInput.class));
    }
}
