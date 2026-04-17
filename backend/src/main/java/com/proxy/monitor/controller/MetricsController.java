package com.proxy.monitor.controller;

import com.proxy.monitor.model.ServiceLog.LogStatus;
import com.proxy.monitor.repository.LogRepository;
import com.proxy.monitor.service.MetricsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * REST Controller para métricas y observabilidad.
 *
 * Endpoints:
 * GET /api/metrics/summary → resumen por servicio (tarjetas del dashboard)
 * GET /api/metrics/logs → logs filtrados con paginación (tabla)
 * POST /api/metrics/simulate-load → genera 50 llamadas aleatorias
 */
@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService metricsService;
    private final LogRepository logRepository;

    public MetricsController(MetricsService metricsService, LogRepository logRepository) {
        this.metricsService = metricsService;
        this.logRepository = logRepository;
    }

    /**
     * GET /api/metrics/summary
     * Retorna: lista de MetricsSummary (una por servicio)
     * El frontend usa esto para las 3 tarjetas del dashboard.
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {
        return ResponseEntity.ok(metricsService.getSummary());
    }

    /**
     * GET
     * /api/metrics/logs?service=inventory&status=ERROR&from=...&to=...&page=0&size=20
     *
     * @param service filtro de servicio (opcional)
     * @param status  filtro de estado SUCCESS|ERROR (opcional)
     * @param from    fecha desde ISO (opcional) — ej: 2026-04-17T00:00:00
     * @param to      fecha hasta ISO (opcional)
     * @param page    número de página (default 0)
     * @param size    registros por página (default 20)
     *
     * @DateTimeFormat(iso = ...) le dice a Spring cómo parsear la fecha del query
     *                     param
     */
    @GetMapping("/logs")
    public ResponseEntity<?> getLogs(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Convertimos el String status al enum LogStatus (null si no viene el param)
        LogStatus logStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                logStatus = LogStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Status inválido. Use: SUCCESS o ERROR"));
            }
        }

        Map<String, Object> result = logRepository.findWithFilters(
                service, logStatus, from, to, page, size);

        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/metrics/simulate-load
     * Genera 50 llamadas aleatorias para poblar el dashboard con datos.
     * El frontend tiene un botón "Simular Carga" que llama este endpoint.
     */
    @PostMapping("/simulate-load")
    public ResponseEntity<?> simulateLoad() {
        Map<String, Object> result = metricsService.simulateLoad();
        return ResponseEntity.ok(result);
    }

    /**
     * DELETE /api/metrics/logs
     * Limpia los logs acumulados (util para dashboards en vivo)
     */
    @DeleteMapping("/logs")
    public ResponseEntity<?> clearLogs() {
        metricsService.clearLogs();
        return ResponseEntity.ok(Map.of("message", "Logs limpiados exitosamente"));
    }

    /**
     * GET /api/metrics/health
     * Endpoint de salud: útil para verificar que el backend está vivo en Render.
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "proxy-monitor",
                "totalLogs", logRepository.count()));
    }
}
