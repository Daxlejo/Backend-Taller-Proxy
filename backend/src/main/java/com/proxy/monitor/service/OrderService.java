package com.proxy.monitor.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * SERVICIO DE PEDIDOS: Gestión de órdenes de compra.
 *
 * Estados de un pedido: PENDING → CONFIRMED → SHIPPED → DELIVERED
 *                              → CANCELLED
 */
@Service
public class OrderService extends MicroserviceBase {

    // Pedidos simulados en memoria: orderId → datos del pedido
    private final Map<String, Map<String, Object>> orders = new LinkedHashMap<>();

    private static final List<String> PRODUCTS = List.of(
            "laptop", "monitor", "teclado", "mouse", "auriculares"
    );
    private static final List<String> STATUSES = List.of(
            "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED"
    );

    public OrderService() {
        super("orders");
    }

    @Override
    protected Map<String, Object> handleOperation(String operation, Object... params) {
        return switch (operation.toLowerCase()) {
            case "createorder"  -> createOrder(params);
            case "getorder"     -> getOrder(params);
            case "cancelorder"  -> cancelOrder(params);
            case "listorders"   -> listOrders();
            case "updatestatus" -> updateStatus(params);
            default             -> throw unknownOperation(operation);
        };
    }

    private Map<String, Object> createOrder(Object... params) {
        String orderId  = "ORD-" + String.format("%04d", random.nextInt(9999));
        String product  = params.length > 0 ? params[0].toString() : PRODUCTS.get(random.nextInt(PRODUCTS.size()));
        int    quantity = params.length > 1 ? Integer.parseInt(params[1].toString()) : random.nextInt(5) + 1;
        double price    = 50 + random.nextDouble() * 1500;

        Map<String, Object> order = new LinkedHashMap<>();
        order.put("orderId",     orderId);
        order.put("product",     product);
        order.put("quantity",    quantity);
        order.put("unitPrice",   Math.round(price * 100.0) / 100.0);
        order.put("total",       Math.round(price * quantity * 100.0) / 100.0);
        order.put("status",      "PENDING");
        order.put("createdAt",   java.time.LocalDateTime.now().toString());

        orders.put(orderId, order);

        return Map.of(
                "order",   order,
                "message", "Pedido creado exitosamente"
        );
    }

    private Map<String, Object> getOrder(Object... params) {
        String orderId = params.length > 0 ? params[0].toString() : randomOrderId();
        Map<String, Object> order = orders.get(orderId);
        if (order == null) {
            throw new NoSuchElementException("Pedido '" + orderId + "' no encontrado");
        }
        return Map.of("order", order);
    }

    private Map<String, Object> cancelOrder(Object... params) {
        String orderId = params.length > 0 ? params[0].toString() : randomOrderId();
        Map<String, Object> order = orders.get(orderId);
        if (order == null) {
            throw new NoSuchElementException("Pedido '" + orderId + "' no encontrado");
        }
        if ("DELIVERED".equals(order.get("status"))) {
            throw new IllegalStateException("No se puede cancelar un pedido ya entregado");
        }
        order.put("status", "CANCELLED");
        return Map.of(
                "orderId", orderId,
                "status",  "CANCELLED",
                "message", "Pedido cancelado exitosamente"
        );
    }

    private Map<String, Object> listOrders() {
        return Map.of(
                "orders", new ArrayList<>(orders.values()),
                "total",  orders.size()
        );
    }

    private Map<String, Object> updateStatus(Object... params) {
        String orderId = params.length > 0 ? params[0].toString() : randomOrderId();
        String newStatus = params.length > 1 ? params[1].toString()
                : STATUSES.get(random.nextInt(STATUSES.size()));
        Map<String, Object> order = orders.get(orderId);
        if (order == null) {
            throw new NoSuchElementException("Pedido '" + orderId + "' no encontrado");
        }
        order.put("status", newStatus);
        return Map.of("orderId", orderId, "newStatus", newStatus);
    }

    private String randomOrderId() {
        if (orders.isEmpty()) {
            createOrder(new Object[0]);
        }
        List<String> ids = new ArrayList<>(orders.keySet());
        return ids.get(random.nextInt(ids.size()));
    }
}
