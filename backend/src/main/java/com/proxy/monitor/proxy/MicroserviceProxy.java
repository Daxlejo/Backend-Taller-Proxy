package com.proxy.monitor.proxy;

/**
 * INTERFACE GENÉRICA: Define el contrato que todo proxy de microservicio debe cumplir.
 *
 * ¿Qué es un Genérico <T>?
 *  - <T> es un "tipo paramétrico": al implementar esta interface puedes decir
 *    qué tipo de dato va a retornar el método execute().
 *  - Ejemplo: MicroserviceProxy<Map<String, Object>> → retorna un mapa
 *             MicroserviceProxy<String>              → retorna un String
 *
 * ¿Por qué usar interface aquí?
 *  - Define un CONTRATO: cualquier clase que implemente MicroserviceProxy<T>
 *    ESTÁ OBLIGADA a implementar execute().
 *  - Permite polimorfismo: puedo tener una variable de tipo MicroserviceProxy<T>
 *    apuntando a cualquier implementación (LoggingProxy, MockProxy, etc.)
 *
 * ¿Qué es Object... params (Varargs)?
 *  - "..." significa que acepta 0, 1, 2 o N argumentos del tipo Object.
 *  - Internamente llega como un array: Object[]
 *  - Ejemplo de uso: proxy.execute("checkStock", "laptop", 5)
 *                    proxy.execute("listItems")   ← sin parámetros también vale
 *
 * @param <T> Tipo de retorno del método execute
 */
public interface MicroserviceProxy<T> {

    /**
     * Ejecuta una operación en el microservicio.
     *
     * @param operation Nombre de la operación a ejecutar (ej: "checkStock", "createOrder")
     * @param params    Parámetros variables que necesita la operación
     * @return T        El resultado de la operación (tipo definido por quien implementa)
     * @throws UnsupportedOperationException si la operación no existe en el servicio
     */
    T execute(String operation, Object... params);

    /**
     * Retorna el identificador único del servicio.
     * Útil para el logging: saber QUÉ servicio está siendo llamado.
     *
     * @return String con el nombre del servicio (ej: "inventory", "orders", "payments")
     */
    String getServiceId();
}
