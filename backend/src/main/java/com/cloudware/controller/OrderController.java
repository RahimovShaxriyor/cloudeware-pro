package com.cloudware.controller;

import com.cloudware.service.AuthService;
import com.cloudware.service.DataService;
import com.cloudware.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final DataService data;
    private final AuthService auth;
    private final OrderService orders;

    public OrderController(DataService data, AuthService auth, OrderService orders) {
        this.data = data;
        this.auth = auth;
        this.orders = orders;
    }

    @GetMapping
    public Object all(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("SELECT * FROM orders ORDER BY id DESC");
    }

    @GetMapping("/{id}")
    public Object one(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        return orders.full(id);
    }

    @PostMapping
    public Object create(@RequestHeader("Authorization") String header, @RequestBody Map<String, Object> body) {
        return orders.create(auth.require(header), body);
    }

    @PutMapping("/{id}")
    public Object update(@RequestHeader("Authorization") String header, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        auth.require(header);
        return orders.update(id, body);
    }

    @PatchMapping("/{id}/status")
    public Object status(@RequestHeader("Authorization") String header, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        auth.require(header);
        data.jdbc().update("UPDATE orders SET status=?, updated_at=NOW() WHERE id=?", DataService.text(body, "status", "NEW"), id);
        return orders.full(id);
    }

    @DeleteMapping("/{id}")
    public Object delete(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        data.jdbc().update("DELETE FROM orders WHERE id=?", id);
        return data.ok("Order deleted");
    }

    @GetMapping("/status/{status}")
    public Object byStatus(@RequestHeader("Authorization") String header, @PathVariable String status) {
        auth.require(header);
        return data.rows("SELECT * FROM orders WHERE status=? ORDER BY id DESC", status);
    }

    @GetMapping("/customer/{customerId}")
    public Object byCustomer(@RequestHeader("Authorization") String header, @PathVariable Long customerId) {
        auth.require(header);
        return data.rows("SELECT * FROM orders WHERE customer_id=? ORDER BY id DESC", customerId);
    }

    @PostMapping("/{id}/items")
    public Object addItem(@RequestHeader("Authorization") String header, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        auth.require(header);
        return orders.addItem(id, body);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public Object removeItem(@RequestHeader("Authorization") String header, @PathVariable Long id, @PathVariable Long itemId) {
        auth.require(header);
        return orders.removeItem(id, itemId);
    }

    @PostMapping("/{id}/confirm")
    public Object confirm(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        return orders.confirm(auth.require(header), id);
    }

    @PostMapping("/{id}/cancel")
    public Object cancel(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        return orders.cancel(auth.require(header), id);
    }

    @PostMapping("/{id}/ship")
    public Object ship(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        return orders.ship(auth.require(header), id);
    }

    @PostMapping("/{id}/deliver")
    public Object deliver(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        return orders.deliver(auth.require(header), id);
    }
}
