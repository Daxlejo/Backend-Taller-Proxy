package com.proxy.monitor.config;

import com.proxy.monitor.proxy.LoggingProxy;
import com.proxy.monitor.proxy.MicroserviceProxy;
import com.proxy.monitor.repository.LogRepository;
import com.proxy.monitor.service.InventoryService;
import com.proxy.monitor.service.OrderService;
import com.proxy.monitor.service.PaymentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class AppConfig {

    private final LogRepository logRepository;

    // Spring inyecta el LogRepository automáticamente por constructor
    public AppConfig(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Bean(name = "inventoryProxy")
    public MicroserviceProxy<Map<String, Object>> inventoryProxy(InventoryService inventoryService) {
        return new LoggingProxy<>(inventoryService, logRepository);
    }

    @Bean(name = "ordersProxy")
    public MicroserviceProxy<Map<String, Object>> ordersProxy(OrderService orderService) {
        return new LoggingProxy<>(orderService, logRepository);
    }

    @Bean(name = "paymentsProxy")
    public MicroserviceProxy<Map<String, Object>> paymentsProxy(PaymentService paymentService) {
        return new LoggingProxy<>(paymentService, logRepository);
    }
}
