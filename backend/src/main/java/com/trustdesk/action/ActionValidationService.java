package com.trustdesk.action;

import com.trustdesk.ai.triage.TriageCategory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

@Service
public class ActionValidationService {

    private final ActionCatalogService actionCatalogService;

    public ActionValidationService(ActionCatalogService actionCatalogService) {
        this.actionCatalogService = actionCatalogService;
    }

    public ActionDefinition validate(ActionRequest request, TriageCategory category) {

        if (request == null) {
            throw new ActionValidationException("Action request is required.");
        }

        if (request.actionType() == null) {
            throw new ActionValidationException("Action type is required.");
        }

        ActionDefinition definition =
                actionCatalogService.getDefinition(request.actionType());

        if (category == null || !definition.allowedCategories()
                .contains(category.name().toLowerCase(Locale.ROOT))) {
            throw new ActionValidationException(
                    "Action " + request.actionType()
                            + " is not allowed for the ticket category.");
        }

        if (request.reason() == null || request.reason().isBlank()) {
            throw new ActionValidationException("Action reason is required.");
        }

        Map<String, String> parameters = request.parameters();

        if (parameters == null) {
            throw new ActionValidationException("Action parameters are required.");
        }

        for (String requiredField : definition.requiredFields()) {

            switch (requiredField) {

                case "reason" -> {
                    if (request.reason() == null || request.reason().isBlank()) {
                        throw new ActionValidationException(
                                "Action reason is required.");
                    }
                }

                case "amount" -> {
                    if (request.amount() == null
                            || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new ActionValidationException(
                                "A valid positive amount is required.");
                    }
                }

                case "idempotency_key" -> {
                    String key = parameters.get(requiredField);

                    if (key == null || key.isBlank()) {
                        throw new ActionValidationException(
                                "Idempotency key is required.");
                    }
                }

                default -> {
                    String value = parameters.get(requiredField);

                    if (value == null || value.isBlank()) {
                        throw new ActionValidationException(
                                "Missing required action parameter: " + requiredField);
                    }
                }
            }
        }

        validateCouponAmount(request, definition);

        return definition;
    }

    private void validateCouponAmount(
            ActionRequest request,
            ActionDefinition definition) {

        if (definition.maxAmountInr() == null) {
            return;
        }

        BigDecimal amount = request.amount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ActionValidationException(
                    "A valid positive amount is required.");
        }

        if (amount.compareTo(definition.maxAmountInr()) > 0) {
            throw new ActionValidationException(
                    "Amount exceeds the permitted limit of INR "
                            + definition.maxAmountInr());
        }
    }
}
