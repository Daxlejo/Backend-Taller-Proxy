package com.proxy.monitor.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * SERVICIO DE PAGOS: Procesamiento de transacciones.
 *
 * ⚠️ FALLA INTENCIONAL: El 10% de las llamadas a processPayment fallan.
 * Esto simula la inestabilidad real de pasarelas de pago (timeouts, rechazos).
 * Causa que el PaymentService tenga mayor errorRate → se muestra en ROJO en el frontend.
 */
@Service
public class PaymentService extends MicroserviceBase {

    // Probabilidad de falla: 10%
    private static final double FAILURE_RATE = 0.10;

    private final Map<String, Map<String, Object>> transactions = new LinkedHashMap<>();

    private static final List<String> METHODS = List.of(
            "CREDIT_CARD", "DEBIT_CARD", "NEQUI", "DAVIPLATA", "PSE", "CASH"
    );
    private static final List<String> FAILURE_REASONS = List.of(
            "Fondos insuficientes",
            "Tarjeta rechazada por el banco",
            "Timeout de la pasarela de pago",
            "CVV incorrecto",
            "Transacción sospechosa bloqueada"
    );

    public PaymentService() {
        super("payments");
    }

    @Override
    protected Map<String, Object> handleOperation(String operation, Object... params) {
        return switch (operation.toLowerCase()) {
            case "processpayment" -> processPayment(params);
            case "refund"         -> refund(params);
            case "getstatus"      -> getStatus(params);
            case "listtransactions" -> listTransactions();
            default               -> throw unknownOperation(operation);
        };
    }

    /**
     * Procesa un pago con 10% de probabilidad de falla intencional.
     *
     * random.nextDouble() → número entre 0.0 y 1.0
     * Si cae en el 10% inferior (< 0.10) → simulamos falla
     */
    private Map<String, Object> processPayment(Object... params) {
        // ── FALLA INTENCIONAL del 10% ─────────────────────────────────────────
        if (random.nextDouble() < FAILURE_RATE) {
            String reason = FAILURE_REASONS.get(random.nextInt(FAILURE_REASONS.size()));
            throw new RuntimeException("Pago rechazado: " + reason);
        }

        String txId   = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        double amount = params.length > 0
                ? Double.parseDouble(params[0].toString())
                : Math.round((50 + random.nextDouble() * 2000) * 100.0) / 100.0;
        String method = params.length > 1
                ? params[1].toString()
                : METHODS.get(random.nextInt(METHODS.size()));

        Map<String, Object> tx = new LinkedHashMap<>();
        tx.put("transactionId",  txId);
        tx.put("amount",         amount);
        tx.put("currency",       "COP");
        tx.put("method",         method);
        tx.put("status",         "APPROVED");
        tx.put("authCode",       "AUTH-" + String.format("%06d", random.nextInt(999999)));
        tx.put("processedAt",    java.time.LocalDateTime.now().toString());

        transactions.put(txId, tx);

        return Map.of(
                "transaction", tx,
                "message",     "Pago procesado exitosamente"
        );
    }

    /** Realiza un reembolso de una transacción existente */
    private Map<String, Object> refund(Object... params) {
        String txId = params.length > 0 ? params[0].toString() : randomTxId();
        Map<String, Object> tx = transactions.get(txId);
        if (tx == null) {
            throw new NoSuchElementException("Transacción '" + txId + "' no encontrada");
        }
        if ("REFUNDED".equals(tx.get("status"))) {
            throw new IllegalStateException("Esta transacción ya fue reembolsada");
        }
        tx.put("status",     "REFUNDED");
        tx.put("refundedAt", java.time.LocalDateTime.now().toString());

        return Map.of(
                "transactionId", txId,
                "status",        "REFUNDED",
                "amount",        tx.get("amount"),
                "message",       "Reembolso procesado exitosamente"
        );
    }

    private Map<String, Object> getStatus(Object... params) {
        String txId = params.length > 0 ? params[0].toString() : randomTxId();
        Map<String, Object> tx = transactions.get(txId);
        if (tx == null) {
            throw new NoSuchElementException("Transacción '" + txId + "' no encontrada");
        }
        return Map.of("transaction", tx);
    }

    private Map<String, Object> listTransactions() {
        return Map.of(
                "transactions", new ArrayList<>(transactions.values()),
                "total",        transactions.size()
        );
    }

    private String randomTxId() {
        if (transactions.isEmpty()) return "TXN-00000000";
        List<String> ids = new ArrayList<>(transactions.keySet());
        return ids.get(random.nextInt(ids.size()));
    }
}
