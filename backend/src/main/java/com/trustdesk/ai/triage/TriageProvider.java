package com.trustdesk.ai.triage;

public interface TriageProvider {

    TriageResponse analyze(TriageInput input);
}
