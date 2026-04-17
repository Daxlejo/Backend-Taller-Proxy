package com.proxy.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada principal de la aplicación Spring Boot.
 *
 * @SpringBootApplication combina 3 anotaciones:
 *   - @Configuration: esta clase puede definir beans
 *   - @EnableAutoConfiguration: Spring auto-configura todo (servidor, JSON, etc.)
 *   - @ComponentScan: escanea todos los @Component, @Service, @Controller en el paquete
 */
@SpringBootApplication
public class ProxyMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProxyMonitorApplication.class, args);
    }
}
