package com.cloudware.controller;

import com.cloudware.service.AuthService;
import com.cloudware.service.DataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DataService data;
    private final AuthService auth;

    @Value("${app.instance-id:local-backend}")
    private String instanceId;

    public DashboardController(DataService data, AuthService auth) {
        this.data = data;
        this.auth = auth;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return Map.of(
            "totalOrders", data.longValue("SELECT COUNT(*) FROM orders"),
            "activeOrders", data.longValue("SELECT COUNT(*) FROM orders WHERE status NOT IN ('DELIVERED','CANCELLED','RETURNED')"),
            "customers", data.longValue("SELECT COUNT(*) FROM customers"),
            "products", data.longValue("SELECT COUNT(*) FROM products"),
            "warehouses", data.longValue("SELECT COUNT(*) FROM warehouses"),
            "lowStock", data.longValue("SELECT COUNT(*) FROM inventory i JOIN products p ON p.id=i.product_id WHERE (i.quantity-i.reserved_quantity) <= p.minimum_stock"),
            "revenue", data.doubleValue("SELECT COALESCE(SUM(total_amount),0) FROM orders WHERE status IN ('DELIVERED','SHIPPED')"),
            "pendingPayments", data.longValue("SELECT COUNT(*) FROM payments WHERE status IN ('PENDING','PARTIAL')"),
            "instanceId", instanceId
        );
    }

    @GetMapping("/sales-chart")
    public Object salesChart(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("""
            SELECT TO_CHAR(created_at::date, 'YYYY-MM-DD') AS date, COALESCE(SUM(total_amount),0) AS revenue, COUNT(*) AS orders
            FROM orders
            GROUP BY created_at::date
            ORDER BY created_at::date DESC
            LIMIT 14
            """);
    }

    @GetMapping("/low-stock")
    public Object lowStock(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("""
            SELECT i.id, p.name AS product_name, p.sku, p.minimum_stock, w.name AS warehouse_name,
                   i.quantity, i.reserved_quantity, (i.quantity-i.reserved_quantity) AS available_quantity
            FROM inventory i
            JOIN products p ON p.id=i.product_id
            JOIN warehouses w ON w.id=i.warehouse_id
            WHERE (i.quantity-i.reserved_quantity) <= p.minimum_stock
            ORDER BY available_quantity ASC
            LIMIT 10
            """);
    }

    @GetMapping("/recent-orders")
    public Object recentOrders(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("SELECT * FROM orders ORDER BY id DESC LIMIT 10");
    }

    @GetMapping("/top-products")
    public Object topProducts(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("""
            SELECT p.id, p.name, p.sku, COALESCE(SUM(oi.quantity),0) AS sold_quantity, COALESCE(SUM(oi.total_price),0) AS revenue
            FROM products p
            LEFT JOIN order_items oi ON oi.product_id=p.id
            GROUP BY p.id, p.name, p.sku
            ORDER BY sold_quantity DESC, revenue DESC
            LIMIT 10
            """);
    }

    @GetMapping("/latest-activity")
    public Object latestActivity(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("SELECT * FROM activity_log ORDER BY id DESC LIMIT 12");
    }
}
