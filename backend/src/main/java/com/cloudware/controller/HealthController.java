package com.cloudware.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {
    @Value("${app.instance-id:local-backend}")
    private String instanceId;

    @GetMapping("/health")
    public Map<String, Object> health() throws Exception {
        return Map.of(
            "status", "UP",
            "service", "cloudware-pro",
            "instanceId", instanceId,
            "hostname", InetAddress.getLocalHost().getHostName(),
            "time", Instant.now().toString()
        );
    }

    @GetMapping("/openapi")
    public Map<String, Object> openapi() {
        return Map.of(
            "title", "CloudWare Pro API",
            "version", "1.0.0",
            "basePath", "/api",
            "modules", new String[]{"auth", "dashboard", "products", "customers", "warehouses", "inventory", "orders", "payments", "reports", "settings", "users", "roles", "activity", "notifications"}
        );
    }
}
