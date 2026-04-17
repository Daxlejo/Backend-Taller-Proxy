package com.proxy.monitor.controller;

import com.proxy.monitor.proxy.MicroserviceProxy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller para invocar los 3 microservicios a través del LoggingProxy.
 *
 * Clean Architecture: Esta clase solo recibe la petición HTTP, delega al proxy
 * y devuelve la respuesta. NO contiene lógica de negocio.
 *
 * @RestController = @Controller + @ResponseBody (todo se serializa a JSON)
 * @RequestMapping define la ruta base: /api/services
 */
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final MicroserviceProxy<Map<String, Object>> inventoryProxy;
    private final MicroserviceProxy<Map<String, Object>> ordersProxy;
    private final MicroserviceProxy<Map<String, Object>> paymentsProxy;

    // @Qualifier indica qué bean de Spring usar (definidos en AppConfig)
    public ServiceController(
            @Qualifier("inventoryProxy") MicroserviceProxy<Map<String, Object>> inventoryProxy,
            @Qualifier("ordersProxy")   MicroserviceProxy<Map<String, Object>> ordersProxy,
            @Qualifier("paymentsProxy") MicroserviceProxy<Map<String, Object>> paymentsProxy) {
        this.inventoryProxy = inventoryProxy;
        this.ordersProxy    = ordersProxy;
        this.paymentsProxy  = paymentsProxy;
    }

    /**
     * POST /api/services/inventory/{operation}
     * Ejemplo: POST /api/services/inventory/checkStock  body: ["laptop"]
     */
    @PostMapping("/inventory/{operation}")
    public ResponseEntity<?> callInventory(@PathVariable String operation,
                                           @RequestBody(required = false) List<Object> params) {
        return executeProxy(inventoryProxy, operation, params);
    }

    /**
     * POST /api/services/orders/{operation}
     * Ejemplo: POST /api/services/orders/createOrder  body: ["laptop", 2]
     */
    @PostMapping("/orders/{operation}")
    public ResponseEntity<?> callOrders(@PathVariable String operation,
                                        @RequestBody(required = false) List<Object> params) {
        return executeProxy(ordersProxy, operation, params);
    }

    /**
     * POST /api/services/payments/{operation}
     * Ejemplo: POST /api/services/payments/processPayment  body: [150000, "NEQUI"]
     */
    @PostMapping("/payments/{operation}")
    public ResponseEntity<?> callPayments(@PathVariable String operation,
                                          @RequestBody(required = false) List<Object> params) {
        return executeProxy(paymentsProxy, operation, params);
    }

    /**
     * Método privado reutilizable: ejecuta el proxy y maneja errores.
     * Evita duplicar el try-catch en los 3 métodos (principio DRY: Don't Repeat Yourself).
     */
    private ResponseEntity<?> executeProxy(MicroserviceProxy<Map<String, Object>> proxy,
                                           String operation, List<Object> params) {
        try {
            Object[] paramsArray = (params != null) ? params.toArray() : new Object[0];
            Map<String, Object> result = proxy.execute(operation, paramsArray);
            return ResponseEntity.ok(result);

        } catch (UnsupportedOperationException e) {
            // Operación no existe en el servicio → 400 Bad Request
            return ResponseEntity.badRequest().body(Map.of(
                    "error",   "Operación no soportada",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            // Cualquier otro error del servicio → 500 Internal Server Error
            // El LoggingProxy ya guardó el log de error antes de relanzar la excepción
            return ResponseEntity.internalServerError().body(Map.of(
                    "error",   "Error en el servicio",
                    "message", e.getMessage()
            ));
        }
    }
}
