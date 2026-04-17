package com.proxy.monitor.service;

import com.proxy.monitor.model.MetricsSummary;
import com.proxy.monitor.model.ServiceLog;
import com.proxy.monitor.model.ServiceLog.LogStatus;
import com.proxy.monitor.proxy.MicroserviceProxy;
import com.proxy.monitor.repository.LogRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MetricsService: Lógica de negocio para calcular métricas y simular carga.
 *
 * Clean Architecture: El controller no calcula nada, delega aquí.
 * Esta clase orquesta los datos del repositorio y aplica reglas de negocio.
 */
@Service
public class MetricsService {

    private final LogRepository logRepository;
    private final MicroserviceProxy<Map<String, Object>> inventoryProxy;
    private final MicroserviceProxy<Map<String, Object>> ordersProxy;
    private final MicroserviceProxy<Map<String, Object>> paymentsProxy;

    private static final List<String> SERVICES    = List.of("inventory", "orders", "payments");
    private static final List<String> INV_OPS     = List.of("checkStock", "addItem", "removeItem", "listItems");
    private static final List<String> ORDER_OPS   = List.of("createOrder", "listOrders", "updateStatus");
    private static final List<String> PAYMENT_OPS = List.of("processPayment", "listTransactions");

    private final Random random = new Random();

    public MetricsService(
            LogRepository logRepository,
            @Qualifier("inventoryProxy") MicroserviceProxy<Map<String, Object>> inventoryProxy,
            @Qualifier("ordersProxy")    MicroserviceProxy<Map<String, Object>> ordersProxy,
            @Qualifier("paymentsProxy")  MicroserviceProxy<Map<String, Object>> paymentsProxy) {
        this.logRepository  = logRepository;
        this.inventoryProxy = inventoryProxy;
        this.ordersProxy    = ordersProxy;
        this.paymentsProxy  = paymentsProxy;
    }

    /**
     * Calcula el resumen de métricas para cada servicio.
     * Retorna una lista con una MetricsSummary por servicio.
     */
    public List<MetricsSummary> getSummary() {
        return SERVICES.stream()
                .map(this::buildSummaryForService)
                .collect(Collectors.toList());
    }

    private MetricsSummary buildSummaryForService(String serviceId) {
        List<ServiceLog> serviceLogs = logRepository.findByServiceId(serviceId);

        long total   = serviceLogs.size();
        long errors  = serviceLogs.stream().filter(l -> l.getStatus() == LogStatus.ERROR).count();
        long success = total - errors;

        double errorRate = total > 0 ? (errors * 100.0 / total) : 0.0;
        double avgTime   = serviceLogs.stream()
                .mapToLong(ServiceLog::getDurationMs)
                .average()
                .orElse(0.0);

        return new MetricsSummary(serviceId, total, success, errors, errorRate, avgTime);
    }

    /**
     * Genera 50 llamadas aleatorias distribuidas entre los 3 servicios.
     * Útil para poblar el dashboard con datos de prueba.
     *
     * Se ejecuta en el hilo actual (¡puede tardar ~15-20 seg por la latencia simulada!).
     * Por eso retornamos un resumen al final en vez de bloquear al cliente.
     */
    public Map<String, Object> simulateLoad() {
        int total = 50;

        // Lanzamos en un hilo separado para NO bloquear la petición HTTP
        new Thread(() -> {
            for (int i = 0; i < total; i++) {
                try {
                    String service = SERVICES.get(random.nextInt(SERVICES.size()));
                    callRandomOperation(service);
                } catch (Exception ignored) { }
            }
        }).start();

        return Map.of(
                "message",      "Simulación de carga iniciada en segundo plano",
                "totalCalls",   total,
                "status",       "PROCESSING"
        );
    }

    public void clearLogs() {
        logRepository.deleteAll();
    }

    private void callRandomOperation(String service) {
        switch (service) {
            case "inventory" -> inventoryProxy.execute(INV_OPS.get(random.nextInt(INV_OPS.size())));
            case "orders"    -> ordersProxy.execute(ORDER_OPS.get(random.nextInt(ORDER_OPS.size())));
            case "payments"  -> paymentsProxy.execute(PAYMENT_OPS.get(random.nextInt(PAYMENT_OPS.size())));
        }
    }
}
