package com.trustdesk.action;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ActionCatalogService {

    private final Map<ActionType, ActionDefinition> definitions =
            new EnumMap<>(ActionType.class);

    public ActionCatalogService(
            ObjectMapper objectMapper,
            @Value("${trustdesk.data.directory}") String dataDirectory) {

        Path catalogPath = Path.of(dataDirectory, "tool_actions.json");

        try {
            JsonNode root = objectMapper.readTree(catalogPath);

            for (JsonNode node : root) {
                ActionType actionType = ActionType.valueOf(
                        node.path("tool_name").asText().toUpperCase(Locale.ROOT));

                List<String> allowedCategories = readStringList(
                        node.path("allowed_categories"));

                List<String> requiredFields = readStringList(
                        node.path("required_fields"));

                BigDecimal maxAmount = node.hasNonNull("max_amount_inr")
                        ? node.path("max_amount_inr").decimalValue()
                        : null;

                ActionDefinition definition = new ActionDefinition(
                        actionType,
                        node.path("description").asText(),
                        node.path("risk_level").asText(),
                        node.path("requires_human_approval").asBoolean(),
                        allowedCategories,
                        requiredFields,
                        maxAmount
                );

                definitions.put(actionType, definition);
            }

        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Unable to load action catalog from " + catalogPath,
                    exception);
        }
    }

    public ActionDefinition getDefinition(ActionType actionType) {
        ActionDefinition definition = definitions.get(actionType);

        if (definition == null) {
            throw new IllegalArgumentException(
                    "Unsupported action type: " + actionType);
        }

        return definition;
    }

    public List<ActionDefinition> getAllDefinitions() {
        return List.copyOf(definitions.values());
    }

    private List<String> readStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        node.forEach(value -> values.add(value.asText()));
        return values;
    }
}
