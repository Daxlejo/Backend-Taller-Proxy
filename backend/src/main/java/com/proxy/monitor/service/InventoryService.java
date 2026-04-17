package com.proxy.monitor.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * SERVICIO DE INVENTARIO: Gestión de productos y stock.
 *
 * Hereda de MicroserviceBase e implementa handleOperation() con
 * operaciones de inventario simuladas con datos en memoria.
 *
 * @Service: Spring lo registra como bean (componente de servicio)
 */
@Service
public class InventoryService extends MicroserviceBase {

    // Inventario simulado en memoria: producto → cantidad
    private final Map<String, Integer> inventory = new LinkedHashMap<>(Map.of(
            "laptop",     15,
            "monitor",    8,
            "teclado",    30,
            "mouse",      25,
            "auriculares", 12,
            "webcam",     5,
            "silla",      3
    ));

    public InventoryService() {
        super("inventory"); // le decimos al padre cuál es nuestro serviceId
    }

    /**
     * Implementación del método abstracto: despacha según la operación recibida.
     * Usamos switch para mayor legibilidad que múltiples if-else.
     */
    @Override
    protected Map<String, Object> handleOperation(String operation, Object... params) {
        return switch (operation.toLowerCase()) {
            case "checkstock"  -> checkStock(params);
            case "additem"     -> addItem(params);
            case "removeitem"  -> removeItem(params);
            case "listitems"   -> listItems();
            default            -> throw unknownOperation(operation);
        };
    }

    /** Consulta el stock de un producto */
    private Map<String, Object> checkStock(Object... params) {
        String product = params.length > 0 ? params[0].toString() : randomProduct();
        int stock = inventory.getOrDefault(product, 0);
        return Map.of(
                "product",   product,
                "stock",     stock,
                "available", stock > 0,
                "status",    stock > 5 ? "SUFFICIENT" : stock > 0 ? "LOW" : "OUT_OF_STOCK"
        );
    }

    /** Agrega unidades a un producto */
    private Map<String, Object> addItem(Object... params) {
        String product  = params.length > 0 ? params[0].toString() : randomProduct();
        int quantity    = params.length > 1 ? Integer.parseInt(params[1].toString()) : random.nextInt(10) + 1;
        inventory.merge(product, quantity, Integer::sum);
        return Map.of(
                "product",     product,
                "added",       quantity,
                "newStock",    inventory.get(product),
                "message",     "Stock actualizado exitosamente"
        );
    }

    /** Reduce unidades de un producto */
    private Map<String, Object> removeItem(Object... params) {
        String product  = params.length > 0 ? params[0].toString() : randomProduct();
        int quantity    = params.length > 1 ? Integer.parseInt(params[1].toString()) : random.nextInt(3) + 1;
        int current     = inventory.getOrDefault(product, 0);
        if (current < quantity) {
            throw new IllegalStateException("Stock insuficiente para '" + product +
                    "'. Disponible: " + current + ", Solicitado: " + quantity);
        }
        inventory.put(product, current - quantity);
        return Map.of(
                "product",     product,
                "removed",     quantity,
                "remainingStock", inventory.get(product)
        );
    }

    /** Lista todos los productos con su stock */
    private Map<String, Object> listItems() {
        return Map.of(
                "items",      new LinkedHashMap<>(inventory),
                "totalProducts", inventory.size(),
                "totalUnits", inventory.values().stream().mapToInt(Integer::intValue).sum()
        );
    }

    private String randomProduct() {
        List<String> products = new ArrayList<>(inventory.keySet());
        return products.get(random.nextInt(products.size()));
    }
}
