package com.cloudware.controller;

import com.cloudware.service.ActivityService;
import com.cloudware.service.AuthService;
import com.cloudware.service.DataService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final DataService data;
    private final AuthService auth;
    private final ActivityService activity;

    public PaymentController(DataService data, AuthService auth, ActivityService activity) {
        this.data = data;
        this.auth = auth;
        this.activity = activity;
    }

    @GetMapping
    public Object all(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("""
            SELECT p.*, c.company_name AS customer_name, o.order_number
            FROM payments p
            LEFT JOIN customers c ON c.id=p.customer_id
            LEFT JOIN orders o ON o.id=p.order_id
            ORDER BY p.id DESC
            """);
    }

    @GetMapping("/{id}")
    public Object one(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        return data.row("SELECT * FROM payments WHERE id=?", id);
    }

    @PostMapping
    public Object create(@RequestHeader("Authorization") String header, @RequestBody Map<String, Object> body) {
        Map<String, Object> user = auth.require(header);
        Long id = data.jdbc().queryForObject("""
            INSERT INTO payments(order_id,customer_id,amount,method,status,payment_date,notes,updated_at)
            VALUES (?,?,?,?,?,COALESCE(CAST(? AS DATE), CURRENT_DATE),?,NOW()) RETURNING id
            """, Long.class,
            DataService.id(body, "orderId"), DataService.id(body, "customerId"), DataService.decimal(body, "amount", BigDecimal.ZERO),
            DataService.text(body, "method", "CASH"), DataService.text(body, "status", "PENDING"), DataService.text(body, "paymentDate", null), DataService.text(body, "notes", ""));
        activity.log(user, "Payments", "created", "Payment added");
        return data.row("SELECT * FROM payments WHERE id=?", id);
    }

    @PutMapping("/{id}")
    public Object update(@RequestHeader("Authorization") String header, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        auth.require(header);
        data.jdbc().update("""
            UPDATE payments SET order_id=?, customer_id=?, amount=?, method=?, status=?, payment_date=COALESCE(CAST(? AS DATE), payment_date), notes=?, updated_at=NOW() WHERE id=?
            """, DataService.id(body, "orderId"), DataService.id(body, "customerId"), DataService.decimal(body, "amount", BigDecimal.ZERO),
            DataService.text(body, "method", "CASH"), DataService.text(body, "status", "PENDING"), DataService.text(body, "paymentDate", null), DataService.text(body, "notes", ""), id);
        return data.row("SELECT * FROM payments WHERE id=?", id);
    }

    @DeleteMapping("/{id}")
    public Object delete(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        data.jdbc().update("DELETE FROM payments WHERE id=?", id);
        return data.ok("Payment deleted");
    }

    @GetMapping("/order/{orderId}")
    public Object byOrder(@RequestHeader("Authorization") String header, @PathVariable Long orderId) {
        auth.require(header);
        return data.rows("SELECT * FROM payments WHERE order_id=? ORDER BY id DESC", orderId);
    }

    @GetMapping("/customer/{customerId}")
    public Object byCustomer(@RequestHeader("Authorization") String header, @PathVariable Long customerId) {
        auth.require(header);
        return data.rows("SELECT * FROM payments WHERE customer_id=? ORDER BY id DESC", customerId);
    }
}
