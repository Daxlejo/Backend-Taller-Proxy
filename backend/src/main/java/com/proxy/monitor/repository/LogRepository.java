package com.proxy.monitor.repository;

import com.proxy.monitor.model.ServiceLog;
import com.proxy.monitor.model.ServiceLog.LogStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * REPOSITORY: Capa de acceso a datos (en este caso, en memoria).
 *
 * Clean Architecture: El repository es el único punto que sabe CÓMO
 * se almacenan los datos. Si mañana quisieras usar PostgreSQL, solo
 * cambias esta clase — el resto del sistema no se toca.
 *
 * ¿Por qué ConcurrentLinkedDeque?
 *  - Deque = estructura de datos doble-cola (podemos agregar/quitar de ambos extremos)
 *  - Concurrent = thread-safe: múltiples hilos pueden leer/escribir sin conflictos
 *  - En Spring, los requests HTTP llegan en hilos separados → necesitamos thread safety
 *
 * @Repository: marca esta clase como componente de datos para Spring
 */
@Repository
public class LogRepository {

    // Máximo de logs a guardar en memoria (evita OutOfMemoryError)
    private static final int MAX_LOGS = 1000;

    // Estructura thread-safe para guardar logs en memoria
    private final ConcurrentLinkedDeque<ServiceLog> logs = new ConcurrentLinkedDeque<>();

    /**
     * Guarda un log nuevo. Si superamos el límite, eliminamos el más viejo.
     * Esto implementa una estrategia FIFO (First In, First Out).
     */
    public void save(ServiceLog log) {
        if (logs.size() >= MAX_LOGS) {
            logs.pollLast(); // elimina el log más antiguo (al final del deque)
        }
        logs.addFirst(log); // agrega el nuevo al inicio (más reciente primero)
    }

    /**
     * Retorna todos los logs ordenados del más reciente al más antiguo.
     * Usamos snapshot (new ArrayList) para evitar ConcurrentModificationException.
     */
    public List<ServiceLog> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(logs));
    }

    /**
     * Limpia todos los logs.
     */
    public void deleteAll() {
        logs.clear();
    }

    /**
     * Búsqueda con filtros opcionales y paginación.
     *
     * Usamos Java Streams para filtrar de forma declarativa y legible.
     * Cada filter() agrega una condición solo si el parámetro no es null/blank.
     *
     * @param serviceId  Filtrar por servicio ("inventory", "orders", "payments") o null para todos
     * @param status     Filtrar por estado (SUCCESS/ERROR) o null para todos
     * @param from       Fecha/hora desde (inclusive) o null sin límite inferior
     * @param to         Fecha/hora hasta (inclusive) o null sin límite superior
     * @param page       Número de página (0-indexed)
     * @param size       Cantidad de registros por página
     */
    public Map<String, Object> findWithFilters(String serviceId, LogStatus status,
                                                LocalDateTime from, LocalDateTime to,
                                                int page, int size) {
        // 1. Filtramos con Streams (encadenamos condiciones)
        List<ServiceLog> filtered = logs.stream()
                .filter(log -> serviceId == null || serviceId.isBlank()
                        || log.getServiceId().equalsIgnoreCase(serviceId))
                .filter(log -> status == null
                        || log.getStatus() == status)
                .filter(log -> from == null
                        || !log.getTimestamp().isBefore(from))
                .filter(log -> to == null
                        || !log.getTimestamp().isAfter(to))
                .collect(Collectors.toList());

        // 2. Calculamos paginación
        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<ServiceLog> pageContent = filtered.subList(fromIndex, toIndex);

        // 3. Retornamos resultado con metadatos de paginación
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", pageContent);
        result.put("page", page);
        result.put("size", size);
        result.put("totalElements", totalElements);
        result.put("totalPages", totalPages);
        result.put("hasNext", page < totalPages - 1);

        return result;
    }

    /**
     * Retorna todos los logs de un servicio específico.
     * Usado para calcular métricas por servicio.
     */
    public List<ServiceLog> findByServiceId(String serviceId) {
        return logs.stream()
                .filter(log -> log.getServiceId().equalsIgnoreCase(serviceId))
                .collect(Collectors.toList());
    }

    /**
     * Retorna los IDs únicos de servicios que tienen logs registrados.
     */
    public Set<String> findDistinctServiceIds() {
        return logs.stream()
                .map(ServiceLog::getServiceId)
                .collect(Collectors.toSet());
    }

    /** Total de logs almacenados */
    public int count() {
        return logs.size();
    }
}
