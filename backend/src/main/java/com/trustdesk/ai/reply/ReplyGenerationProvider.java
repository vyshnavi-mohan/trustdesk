package com.trustdesk.ai.reply;

public interface ReplyGenerationProvider {

    ReplyGenerationResponse generate(ReplyGenerationInput input);
}
