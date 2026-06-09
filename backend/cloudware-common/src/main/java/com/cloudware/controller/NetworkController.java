package com.cloudware.controller;

import com.cloudware.annotation.Microservice;
import com.cloudware.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Microservice("platform")
@RestController
@RequestMapping("/api/network")
public class NetworkController {
    @Value("${app.instance-id:platform}")
    private String instanceId;

    @Value("${cloudware.service:platform}")
    private String serviceName;

    private final AuthService auth;

    public NetworkController(AuthService auth) {
        this.auth = auth;
    }

    @GetMapping("/instance")
    public Map<String, Object> instance() throws Exception {
        return Map.of(
            "service", serviceName,
            "instanceId", instanceId,
            "hostname", InetAddress.getLocalHost().getHostName(),
            "status", "UP",
            "time", Instant.now().toString()
        );
    }

    @GetMapping("/overview")
    public Map<String, Object> overview(@RequestHeader("Authorization") String header) {
        auth.require(header, "reports.read");
        return Map.of(
            "architecture", "microservices",
            "gateway", "nginx",
            "services", List.of(
                Map.of("name", "identity-service", "path", "/api/auth, /api/users, /api/roles", "port", 8081),
                Map.of("name", "catalog-service", "path", "/api/products", "port", 8082),
                Map.of("name", "crm-service", "path", "/api/customers", "port", 8083),
                Map.of("name", "wms-service", "path", "/api/warehouses, /api/inventory", "port", 8084),
                Map.of("name", "order-service", "path", "/api/orders", "port", 8085),
                Map.of("name", "finance-service", "path", "/api/payments", "port", 8086),
                Map.of("name", "platform-service", "path", "/api/dashboard, /api/reports, /api/settings, /api/activity, /api/notifications", "port", 8087)
            ),
            "database", "PostgreSQL 16 (shared)",
            "loadBalancing", "Nginx path-based API gateway",
            "currentInstance", instanceId,
            "time", Instant.now().toString()
        );
    }
}
