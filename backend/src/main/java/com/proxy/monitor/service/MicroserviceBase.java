package com.proxy.monitor.service;

import com.proxy.monitor.proxy.MicroserviceProxy;

import java.util.Map;
import java.util.Random;

/**
 * CLASE ABSTRACTA BASE: Define la estructura común de todos los microservicios.
 *
 * Clean Architecture: Esta clase pertenece a la capa de Servicios/Casos de Uso.
 *
 * POO - Herencia y Abstracción:
 *  - Es ABSTRACTA: no se puede instanciar directamente (new MicroserviceBase() → ERROR)
 *  - Define el campo serviceId y el método getServiceId() una sola vez
 *  - Las subclases HEREDAN estos atributos y solo implementan su lógica propia
 *  - Implementa MicroserviceProxy<Map<String,Object>>: todos los servicios retornan un Map
 *
 * ¿Por qué Map<String, Object> como tipo retorno?
 *  - Cada operación puede retornar datos de estructura diferente
 *  - Un Map es flexible: {"product": "laptop", "stock": 15, "available": true}
 *  - Se serializa automáticamente a JSON por Jackson (librería incluida en Spring)
 *
 * Herencia en acción:
 *  MicroserviceBase
 *      ├── InventoryService  → implementa handleOperation()
 *      ├── OrderService      → implementa handleOperation()
 *      └── PaymentService    → implementa handleOperation() + fallas aleatorias
 */
public abstract class MicroserviceBase implements MicroserviceProxy<Map<String, Object>> {

    protected final String serviceId;
    protected final Random random = new Random(); // para simular datos y tiempos

    protected MicroserviceBase(String serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * Template Method Pattern:
     * execute() define el flujo general, handleOperation() es implementado por cada subclase.
     *
     * Esto es Template Method: la clase base define el "esqueleto" del algoritmo
     * y las subclases rellenan los pasos específicos.
     */
    @Override
    public Map<String, Object> execute(String operation, Object... params) {
        // Simular latencia realista (50ms - 300ms) para que las métricas sean interesantes
        simulateLatency();
        // Delegar la lógica específica a cada subclase
        return handleOperation(operation, params);
    }

    /**
     * MÉTODO ABSTRACTO: cada subclase DEBE implementar este método.
     * Define qué hace cada servicio con cada operación.
     *
     * @param operation nombre de la operación
     * @param params    parámetros de la operación
     * @return Map con el resultado de la operación
     */
    protected abstract Map<String, Object> handleOperation(String operation, Object... params);

    @Override
    public String getServiceId() {
        return serviceId;
    }

    /**
     * Simula latencia de red/procesamiento realista.
     * Los servicios reales tienen tiempos variables; esta simulación lo refleja.
     */
    protected void simulateLatency() {
        try {
            // latencia entre 50ms y 350ms
            Thread.sleep(50 + random.nextInt(300));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Crea una respuesta de error estándar.
     * Reutilizado por las subclases para operaciones desconocidas.
     */
    protected UnsupportedOperationException unknownOperation(String operation) {
        return new UnsupportedOperationException(
                "Operación '" + operation + "' no existe en el servicio '" + serviceId + "'"
        );
    }
}
