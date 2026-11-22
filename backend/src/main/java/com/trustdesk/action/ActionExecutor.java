package com.trustdesk.action;

import java.util.Map;

public interface ActionExecutor {

    void execute(
            ActionType actionType,
            Map<String, String> parameters
    );
}