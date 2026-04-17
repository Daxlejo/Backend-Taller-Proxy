package com.proxy.monitor.proxy;

import com.proxy.monitor.model.ServiceLog;
import com.proxy.monitor.model.ServiceLog.LogStatus;
import com.proxy.monitor.repository.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * PATRÓN PROXY (también llamado Decorator aquí):
 *
 *  El LoggingProxy "envuelve" cualquier servicio real y añade comportamiento
 *  sin modificar el servicio original. Así funciona:
 *
 *  Cliente → LoggingProxy.execute() → [registra inicio]
 *                                   → ServicioReal.execute()  ← delega la llamada
 *                                   → [registra duración + resultado]
 *                                   → devuelve resultado
 *
 * ¿Por qué es POO avanzado?
 *  1. Implementa la misma interface que el servicio real (MicroserviceProxy<T>)
 *  2. Tiene UNA instancia del servicio real como campo (composición)
 *  3. Delega la ejecución real al servicio interno
 *  4. Añade lógica transversal (logging, timing) SIN tocar el servicio
 *  5. Es genérico: funciona con CUALQUIER MicroserviceProxy<T>
 *
 * @param <T> El tipo de retorno del servicio que envuelve
 */
public class LoggingProxy<T> implements MicroserviceProxy<T> {

    private static final Logger log = LoggerFactory.getLogger(LoggingProxy.class);

    // ── COMPOSICIÓN: el proxy "tiene" al servicio real ───────────────────────
    // Este es el servicio al que le vamos a delegar la llamada real
    private final MicroserviceProxy<T> realService;

    // Repositorio donde guardamos los logs en memoria
    private final LogRepository logRepository;

    /**
     * Constructor: recibe el servicio real y el repositorio de logs.
     * Spring nos los inyectará automáticamente cuando creemos el bean.
     *
     * @param realService   El servicio real (InventoryService, OrderService, etc.)
     * @param logRepository Repositorio en memoria donde se guardan los logs
     */
    public LoggingProxy(MicroserviceProxy<T> realService, LogRepository logRepository) {
        this.realService = realService;
        this.logRepository = logRepository;
    }

    /**
     * MÉTODO CENTRAL: intercepta la llamada, mide tiempo, delega, y registra.
     *
     * Flujo:
     * 1. Marcamos el tiempo de inicio (System.currentTimeMillis)
     * 2. Llamamos al servicio REAL (puede lanzar excepción)
     * 3. Si SUCCESS: calculamos duración y guardamos log exitoso
     * 4. Si ERROR: calculamos duración y guardamos log de error con mensaje
     * 5. Siempre retornamos el resultado (o relanzamos la excepción)
     */
    @Override
    public T execute(String operation, Object... params) {
        long startTime = System.currentTimeMillis(); // 📌 inicio del cronómetro
        String serviceId = realService.getServiceId();

        log.debug("→ [{}] Iniciando operación '{}' con {} parámetros",
                serviceId, operation, params.length);

        try {
            // ── DELEGAR: llamamos al servicio real ───────────────────────────
            T result = realService.execute(operation, params);

            // ── SUCCESS: calculamos duración y guardamos el log ──────────────
            long duration = System.currentTimeMillis() - startTime;

            ServiceLog serviceLog = ServiceLog.builder()
                    .serviceId(serviceId)
                    .operation(operation)
                    .durationMs(duration)
                    .status(LogStatus.SUCCESS)
                    .inputParams(Arrays.asList(params))
                    .response(result)
                    .build();

            logRepository.save(serviceLog);

            log.debug("✓ [{}] '{}' completado en {}ms", serviceId, operation, duration);

            return result; // retornamos el resultado original sin modificarlo

        } catch (Exception e) {
            // ── ERROR: calculamos duración y guardamos el log de error ────────
            long duration = System.currentTimeMillis() - startTime;

            // Resumimos el stack trace: solo las primeras 3 líneas para no saturar
            String shortStackTrace = buildShortStackTrace(e);

            ServiceLog serviceLog = ServiceLog.builder()
                    .serviceId(serviceId)
                    .operation(operation)
                    .durationMs(duration)
                    .status(LogStatus.ERROR)
                    .inputParams(Arrays.asList(params))
                    .response(null)
                    .errorMessage(e.getMessage() + "\n" + shortStackTrace)
                    .build();

            logRepository.save(serviceLog);

            log.error("✗ [{}] '{}' falló en {}ms: {}", serviceId, operation, duration, e.getMessage());

            // Re-lanzamos la excepción para que el Controller pueda manejarla
            throw e;
        }
    }

    /**
     * Delegamos getServiceId() al servicio real.
     * El proxy es "transparente": se comporta como el servicio que envuelve.
     */
    @Override
    public String getServiceId() {
        return realService.getServiceId();
    }

    /**
     * Construye un stack trace resumido (máximo 3 líneas) para los logs.
     * Un stack trace completo puede tener 50+ líneas: innecesario para auditoría.
     *
     * @param e La excepción capturada
     * @return String con las primeras líneas del stack trace
     */
    private String buildShortStackTrace(Exception e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(3, stackTrace.length); // máximo 3 líneas

        for (int i = 0; i < limit; i++) {
            sb.append("  at ").append(stackTrace[i].toString()).append("\n");
        }
        if (stackTrace.length > 3) {
            sb.append("  ... ").append(stackTrace.length - 3).append(" more");
        }
        return sb.toString();
    }
}
