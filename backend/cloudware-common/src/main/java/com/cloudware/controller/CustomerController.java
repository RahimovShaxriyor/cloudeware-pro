package com.cloudware.controller;

import com.cloudware.annotation.Microservice;
import com.cloudware.service.ActivityService;
import com.cloudware.service.AuthService;
import com.cloudware.service.DataService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

@Microservice("crm")
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final DataService data;
    private final AuthService auth;
    private final ActivityService activity;

    public CustomerController(DataService data, AuthService auth, ActivityService activity) {
        this.data = data;
        this.auth = auth;
        this.activity = activity;
    }

    @GetMapping
    public Object all(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("SELECT * FROM customers ORDER BY id DESC");
    }

    @GetMapping("/{id}")
    public Object one(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        return data.row("SELECT * FROM customers WHERE id=?", id);
    }

    @GetMapping("/search")
    public Object search(@RequestHeader("Authorization") String header, @RequestParam(defaultValue = "") String query) {
        auth.require(header);
        String q = "%" + query.toLowerCase() + "%";
        return data.rows("""
            SELECT * FROM customers
            WHERE LOWER(company_name) LIKE ? OR LOWER(contact_person) LIKE ? OR LOWER(email) LIKE ? OR LOWER(city) LIKE ?
            ORDER BY id DESC
            """, q, q, q, q);
    }

    @PostMapping
    public Object create(@RequestHeader("Authorization") String header, @RequestBody Map<String, Object> body) {
        Map<String, Object> user = auth.require(header);
        validate(body);
        Long id = data.jdbc().queryForObject("""
            INSERT INTO customers(company_name,contact_person,email,phone,city,address,segment,credit_limit,current_debt,active,updated_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,NOW()) RETURNING id
            """, Long.class,
            DataService.text(body, "companyName", null), DataService.text(body, "contactPerson", ""), DataService.text(body, "email", ""),
            DataService.text(body, "phone", ""), DataService.text(body, "city", "Tashkent"), DataService.text(body, "address", ""),
            DataService.text(body, "segment", "Wholesale Partner"), DataService.decimal(body, "creditLimit", BigDecimal.ZERO),
            DataService.decimal(body, "currentDebt", BigDecimal.ZERO), DataService.bool(body, "active", true));
        activity.log(user, "Customers", "created", "Customer created: " + DataService.text(body, "companyName", ""));
        return data.row("SELECT * FROM customers WHERE id=?", id);
    }

    @PutMapping("/{id}")
    public Object update(@RequestHeader("Authorization") String header, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> user = auth.require(header);
        validate(body);
        data.jdbc().update("""
            UPDATE customers SET company_name=?, contact_person=?, email=?, phone=?, city=?, address=?, segment=?, credit_limit=?, current_debt=?, active=?, updated_at=NOW()
            WHERE id=?
            """,
            DataService.text(body, "companyName", null), DataService.text(body, "contactPerson", ""), DataService.text(body, "email", ""),
            DataService.text(body, "phone", ""), DataService.text(body, "city", "Tashkent"), DataService.text(body, "address", ""),
            DataService.text(body, "segment", "Wholesale Partner"), DataService.decimal(body, "creditLimit", BigDecimal.ZERO),
            DataService.decimal(body, "currentDebt", BigDecimal.ZERO), DataService.bool(body, "active", true), id);
        activity.log(user, "Customers", "updated", "Customer updated: " + id);
        return data.row("SELECT * FROM customers WHERE id=?", id);
    }

    @DeleteMapping("/{id}")
    public Object delete(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        if (data.longValue("SELECT COUNT(*) FROM orders WHERE customer_id=?", id) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer has orders and cannot be deleted");
        }
        data.jdbc().update("DELETE FROM customers WHERE id=?", id);
        return data.ok("Customer deleted");
    }

    @GetMapping("/{id}/orders")
    public Object orders(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        return data.rows("SELECT * FROM orders WHERE customer_id=? ORDER BY id DESC", id);
    }

    @GetMapping("/{id}/balance")
    public Object balance(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        return data.row("""
            SELECT c.id, c.company_name, c.credit_limit, c.current_debt,
                   COALESCE(SUM(o.total_amount),0) AS total_orders,
                   COALESCE((SELECT SUM(p.amount) FROM payments p WHERE p.customer_id=c.id AND p.status IN ('PAID','PARTIAL')),0) AS total_paid
            FROM customers c
            LEFT JOIN orders o ON o.customer_id=c.id
            WHERE c.id=?
            GROUP BY c.id
            """, id);
    }

    private void validate(Map<String, Object> body) {
        if (DataService.text(body, "companyName", "").isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company name is required");
        String email = DataService.text(body, "email", "");
        if (!email.isBlank() && !email.contains("@")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is invalid");
    }
}
