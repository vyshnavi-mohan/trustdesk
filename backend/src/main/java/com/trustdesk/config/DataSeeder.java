package com.trustdesk.config;

import com.trustdesk.customer.Customer;
import com.trustdesk.customer.CustomerRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.trustdesk.order.Order;
import com.trustdesk.order.OrderItem;
import com.trustdesk.order.OrderRepository;
import com.trustdesk.order.OrderItemRepository;
import com.trustdesk.ticket.Ticket;
import com.trustdesk.ticket.TicketRepository;

import java.time.OffsetDateTime;

import java.math.BigDecimal;

@Component
public class DataSeeder implements ApplicationRunner {

    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TicketRepository ticketRepository;

    @Value("${trustdesk.data.directory}")
    private String dataDirectory;

    public DataSeeder(
            CustomerRepository customerRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ObjectMapper objectMapper,
            TicketRepository ticketRepository
    ) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.objectMapper = objectMapper;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        loadCustomers();
        loadOrders();
        loadTickets();
    }

    private void loadCustomers() throws Exception {

        File customersFile = new File(dataDirectory, "customers.json");

        JsonNode customers = objectMapper.readTree(customersFile);

        for (JsonNode node : customers) {

            String customerId = node.path("customer_id").asText();

            if (customerRepository.existsById(customerId)) {
                continue;
            }

            List<String> tags = new ArrayList<>();

            for (JsonNode tag : node.path("tags")) {
                tags.add(tag.asText());
            }

            Customer customer = new Customer(
                    customerId,
                    node.path("name").asText(),
                    node.path("email").asText(),
                    node.path("tier").asText(),
                    node.path("country").asText(),
                    LocalDate.parse(node.path("created_at").asText()),
                    node.path("verified").asBoolean(),
                    tags
            );

            customerRepository.save(customer);
        }

        System.out.println("Customer data loading completed.");
    }

    private void loadOrders() throws Exception {

        File ordersFile = new File(dataDirectory, "orders.json");

        JsonNode orders = objectMapper.readTree(ordersFile);

        for (JsonNode node : orders) {

            String orderId = node.path("order_id").asText();

            if (orderRepository.existsById(orderId)) {
                continue;
            }

            String customerId = node.path("customer_id").asText();

            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Customer not found: " + customerId
                    ));

            LocalDate deliveredAt = node.hasNonNull("delivered_at")
                    ? LocalDate.parse(node.path("delivered_at").asText())
                    : null;

            LocalDate eligibleReturnUntil = node.hasNonNull("eligible_return_until")
                    ? LocalDate.parse(node.path("eligible_return_until").asText())
                    : null;

            Order order = new Order(
                    orderId,
                    customer,
                    node.path("status").asText(),
                    LocalDate.parse(node.path("placed_at").asText()),
                    deliveredAt,
                    eligibleReturnUntil,
                    node.path("total").decimalValue(),
                    node.path("currency").asText(),
                    node.path("payment_status").asText(),
                    node.path("tracking_number").asText(null)
            );

            Order savedOrder = orderRepository.save(order);

            List<OrderItem> items = new ArrayList<>();

            for (JsonNode itemNode : node.path("items")) {

                OrderItem item = new OrderItem(
                        savedOrder,
                        itemNode.path("sku").asText(),
                        itemNode.path("name").asText(),
                        itemNode.path("quantity").asInt(),
                        itemNode.path("category").asText(),
                        itemNode.path("final_sale").asBoolean()
                );

                items.add(item);
            }

            orderItemRepository.saveAll(items);
        }

        System.out.println("Order and order-item data loading completed.");
    }

    private void loadTickets() throws Exception {

        File ticketsFile = new File(dataDirectory, "tickets.json");

        JsonNode tickets = objectMapper.readTree(ticketsFile);

        for (JsonNode node : tickets) {

            String ticketId = node.path("ticket_id").asText();

            if (ticketRepository.existsById(ticketId)) {
                continue;
            }

            String customerId = node.path("customer_id").asText();

            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Customer not found: " + customerId
                    ));

            Order order = null;

            if (node.hasNonNull("order_id")) {
                String orderId = node.path("order_id").asText();

                order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Order not found: " + orderId
                        ));
            }

            Ticket ticket = new Ticket(
                    ticketId,
                    customer,
                    order,
                    node.path("channel").asText(),
                    node.path("subject").asText(),
                    node.path("body").asText(),
                    OffsetDateTime.parse(node.path("created_at").asText()),
                    node.path("status").asText()
            );

            ticketRepository.save(ticket);
        }

        System.out.println("Ticket data loading completed.");
    }
}
