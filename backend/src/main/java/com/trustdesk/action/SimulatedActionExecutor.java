package com.trustdesk.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SimulatedActionExecutor implements ActionExecutor {

    private static final Logger log =
            LoggerFactory.getLogger(SimulatedActionExecutor.class);

    @Override
    public void execute(
            ActionType actionType,
            Map<String, String> parameters) {

        switch (actionType) {
            case CREATE_REPLACEMENT_ORDER ->
                    log.info(
                            "SIMULATED: Replacement order creation for order {} and SKU {}",
                            parameters.get("order_id"),
                            parameters.get("sku")
                    );

            case START_REFUND_REVIEW ->
                    log.info(
                            "SIMULATED: Refund review started for order {}",
                            parameters.get("order_id")
                    );

            case OPEN_CARRIER_INVESTIGATION ->
                    log.info(
                            "SIMULATED: Carrier investigation opened for order {} with tracking number {}",
                            parameters.get("order_id"),
                            parameters.get("tracking_number")
                    );

            case ESCALATE_TO_HUMAN ->
                    log.info(
                            "SIMULATED: Ticket {} escalated to queue {}",
                            parameters.get("ticket_id"),
                            parameters.get("queue")
                    );

            case ISSUE_COUPON ->
                    log.info(
                            "SIMULATED: Coupon of INR {} issued for customer {}",
                            parameters.get("amount"),
                            parameters.get("customer_id")
                    );

            case LOCK_ACCOUNT ->
                    log.info(
                            "SIMULATED: Account locked for customer {}",
                            parameters.get("customer_id")
                    );

            default -> throw new UnsupportedOperationException(
                    "Simulation is not implemented for action: " + actionType
            );
        }
    }
}
