package com.proxy.monitor.model;

/**
 * DTO (Data Transfer Object): Objeto que transporta datos resumidos al frontend.
 *
 * NO es una entidad de BD, es solo un "contenedor de datos" para la respuesta HTTP.
 * El frontend usa esto para mostrar las tarjetas del dashboard.
 *
 * POO aplicado:
 *  - Encapsulación con getters/setters
 *  - Constructor con todos los campos (alternativa al Builder cuando son pocos campos)
 */
public record MetricsSummary(
    String serviceId,
    long totalCalls,
    long successCalls,
    long errorCalls,
    double errorRate,
    double avgResponseTimeMs,
    boolean hasProblems
) {
    public MetricsSummary(String serviceId, long totalCalls, long successCalls,
                          long errorCalls, double errorRate, double avgResponseTimeMs) {
        this(
            serviceId, 
            totalCalls, 
            successCalls, 
            errorCalls, 
            Math.round(errorRate * 100.0) / 100.0, 
            Math.round(avgResponseTimeMs * 100.0) / 100.0, 
            (errorRate > 15.0)
        );
    }
}
