package com.cloudware.controller;

import com.cloudware.service.AuthService;
import com.cloudware.service.DataService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final DataService data;
    private final AuthService auth;

    public ReportController(DataService data, AuthService auth) {
        this.data = data;
        this.auth = auth;
    }

    @GetMapping("/sales")
    public Object sales(@RequestHeader("Authorization") String header, @RequestParam Map<String, String> params) {
        auth.require(header);
        return data.rows("""
            SELECT o.order_number, o.status, o.customer_name, o.delivery_city, o.subtotal, o.tax, o.total_amount, o.created_at
            FROM orders o
            WHERE (? = '' OR o.created_at::date >= NULLIF(?, '')::date)
              AND (? = '' OR o.created_at::date <= NULLIF(?, '')::date)
              AND (? = '' OR o.status = ?)
              AND (? = '' OR o.customer_id = NULLIF(?, '')::bigint)
            ORDER BY o.created_at DESC
            """, val(params, "dateFrom"), val(params, "dateFrom"), val(params, "dateTo"), val(params, "dateTo"), val(params, "status"), val(params, "status"), val(params, "customerId"), val(params, "customerId"));
    }

    @GetMapping("/revenue")
    public Object revenue(@RequestHeader("Authorization") String header, @RequestParam Map<String, String> params) {
        auth.require(header);
        return data.rows("""
            SELECT TO_CHAR(created_at::date, 'YYYY-MM-DD') AS date, COUNT(*) AS orders, COALESCE(SUM(total_amount),0) AS revenue
            FROM orders
            WHERE status IN ('SHIPPED','DELIVERED')
              AND (? = '' OR created_at::date >= NULLIF(?, '')::date)
              AND (? = '' OR created_at::date <= NULLIF(?, '')::date)
            GROUP BY created_at::date
            ORDER BY created_at::date DESC
            """, val(params, "dateFrom"), val(params, "dateFrom"), val(params, "dateTo"), val(params, "dateTo"));
    }

    @GetMapping("/inventory")
    public Object inventory(@RequestHeader("Authorization") String header, @RequestParam Map<String, String> params) {
        auth.require(header);
        return data.rows("""
            SELECT p.sku, p.name AS product_name, p.category, w.name AS warehouse_name, i.quantity, i.reserved_quantity, (i.quantity-i.reserved_quantity) AS available_quantity,
                   CASE WHEN (i.quantity-i.reserved_quantity) <= p.minimum_stock THEN TRUE ELSE FALSE END AS low_stock
            FROM inventory i JOIN products p ON p.id=i.product_id JOIN warehouses w ON w.id=i.warehouse_id
            WHERE (? = '' OR i.warehouse_id = NULLIF(?, '')::bigint)
              AND (? = '' OR p.category = ?)
            ORDER BY p.name, w.name
            """, val(params, "warehouseId"), val(params, "warehouseId"), val(params, "category"), val(params, "category"));
    }

    @GetMapping("/customers")
    public Object customers(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("""
            SELECT c.id, c.company_name, c.city, c.segment, c.credit_limit, c.current_debt,
                   COUNT(o.id) AS order_count, COALESCE(SUM(o.total_amount),0) AS order_total
            FROM customers c LEFT JOIN orders o ON o.customer_id=c.id
            GROUP BY c.id ORDER BY order_total DESC
            """);
    }

    @GetMapping("/orders")
    public Object orders(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("SELECT status, COUNT(*) AS count, COALESCE(SUM(total_amount),0) AS amount FROM orders GROUP BY status ORDER BY status");
    }

    @GetMapping("/profit")
    public Object profit(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("""
            SELECT p.sku, p.name AS product_name, COALESCE(SUM(oi.quantity),0) AS sold_quantity,
                   COALESCE(SUM(oi.total_price),0) AS revenue,
                   COALESCE(SUM(oi.quantity * p.wholesale_price),0) AS cost,
                   COALESCE(SUM(oi.total_price - (oi.quantity * p.wholesale_price)),0) AS profit
            FROM products p LEFT JOIN order_items oi ON oi.product_id=p.id
            GROUP BY p.id ORDER BY profit DESC
            """);
    }

    @GetMapping("/export/sales")
    public Object exportSales(@RequestHeader("Authorization") String header, @RequestParam Map<String, String> params) {
        return sales(header, params);
    }

    @GetMapping("/export/inventory")
    public Object exportInventory(@RequestHeader("Authorization") String header, @RequestParam Map<String, String> params) {
        return inventory(header, params);
    }

    private String val(Map<String, String> params, String key) {
        return params.getOrDefault(key, "");
    }
}
