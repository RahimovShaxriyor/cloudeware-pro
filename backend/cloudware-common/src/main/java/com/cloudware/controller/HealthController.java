package com.cloudware.controller;

import com.cloudware.annotation.Microservice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Microservice("platform")
@RestController
@RequestMapping("/api")
public class HealthController {
    @Value("${app.instance-id:platform-service}")
    private String instanceId;

    @Value("${cloudware.service:platform}")
    private String serviceName;

    @GetMapping("/health")
    public Map<String, Object> health() throws Exception {
        return Map.of(
            "status", "UP",
            "service", serviceName,
            "architecture", "microservices",
            "instanceId", instanceId,
            "hostname", InetAddress.getLocalHost().getHostName(),
            "time", Instant.now().toString()
        );
    }

    @GetMapping("/openapi")
    public Map<String, Object> openapi() {
        return Map.of(
            "title", "CloudWare Pro API",
            "version", "2.0.0",
            "basePath", "/api",
            "architecture", "microservices",
            "services", List.of("identity", "catalog", "crm", "wms", "order", "finance", "platform"),
            "modules", new String[]{"auth", "dashboard", "products", "customers", "warehouses", "inventory", "orders", "payments", "reports", "settings", "users", "roles", "activity", "notifications", "network"}
        );
    }
}
