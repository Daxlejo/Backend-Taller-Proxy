package com.proxy.monitor.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * MODELO: Representa un registro (log) de una llamada a un microservicio.
 *
 * POO aplicado:
 * - Encapsulación: todos los campos son privados, acceso via getters/setters
 * - Builder pattern: permite construir el objeto paso a paso (ver clase interna
 * Builder)
 *
 * Cada vez que el LoggingProxy intercepta una llamada, crea un ServiceLog
 * con toda la info relevante para auditoría.
 */
public class ServiceLog {

    private String requestId; // UUID único por cada request
    private String serviceId; // "inventory" | "orders" | "payments"
    private String operation; // Ej: "checkStock", "createOrder"
    private long durationMs; // Tiempo de respuesta en milisegundos
    private LogStatus status; // SUCCESS o ERROR
    private LocalDateTime timestamp; // Fecha y hora exacta de la llamada
    private List<Object> inputParams;// Parámetros enviados al servicio
    private Object response; // Respuesta del servicio (si fue exitosa)
    private String errorMessage; // Mensaje de error (si falló)

    // ── Constructor privado (solo el Builder puede instanciar) ──────────────
    private ServiceLog() {
    }

    // ── Getters ─────────────────────────────────────────────────────────────
    public String getRequestId() {
        return requestId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getOperation() {
        return operation;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public LogStatus getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public List<Object> getInputParams() {
        return inputParams != null ? List.copyOf(inputParams) : List.of();
    }

    public Object getResponse() {
        return response;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public enum LogStatus {
        SUCCESS, ERROR
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ServiceLog log;

        private Builder() {
            log = new ServiceLog();
            // Auto-generamos requestId y timestamp en la construcción
            log.requestId = UUID.randomUUID().toString();
            log.timestamp = LocalDateTime.now();
        }

        public Builder serviceId(String serviceId) {
            log.serviceId = serviceId;
            return this;
        }

        public Builder operation(String operation) {
            log.operation = operation;
            return this;
        }

        public Builder durationMs(long durationMs) {
            log.durationMs = durationMs;
            return this;
        }

        public Builder status(LogStatus status) {
            log.status = status;
            return this;
        }

        public Builder inputParams(List<Object> inputParams) {
            log.inputParams = inputParams;
            return this;
        }

        public Builder response(Object response) {
            log.response = response;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            log.errorMessage = errorMessage;
            return this;
        }

        // build() es el método final que devuelve el objeto construido
        public ServiceLog build() {
            return log;
        }
    }

    @Override
    public String toString() {
        return String.format("[%s] %s::%s → %s (%dms) | requestId=%s",
                timestamp, serviceId, operation, status, durationMs, requestId);
    }
}
