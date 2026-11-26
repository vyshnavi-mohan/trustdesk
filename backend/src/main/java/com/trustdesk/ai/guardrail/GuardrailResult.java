package com.trustdesk.ai.guardrail;

import com.trustdesk.knowledge.dto.KnowledgeDocumentResponse;

import java.util.List;

public record GuardrailResult(

        GuardrailStatus status,

        List<GuardrailFinding> findings,

        List<KnowledgeDocumentResponse> trustedDocuments,

        boolean requiresHumanReview

) {

    public GuardrailResult {
        findings = List.copyOf(findings);
        trustedDocuments = List.copyOf(trustedDocuments);
    }

}
