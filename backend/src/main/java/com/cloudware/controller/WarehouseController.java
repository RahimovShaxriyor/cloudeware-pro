package com.cloudware.controller;

import com.cloudware.service.ActivityService;
import com.cloudware.service.AuthService;
import com.cloudware.service.DataService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {
    private final DataService data;
    private final AuthService auth;
    private final ActivityService activity;

    public WarehouseController(DataService data, AuthService auth, ActivityService activity) {
        this.data = data;
        this.auth = auth;
        this.activity = activity;
    }

    @GetMapping
    public Object all(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("SELECT * FROM warehouses ORDER BY id DESC");
    }

    @GetMapping("/{id}")
    public Object one(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        return data.row("SELECT * FROM warehouses WHERE id=?", id);
    }

    @PostMapping
    public Object create(@RequestHeader("Authorization") String header, @RequestBody Map<String, Object> body) {
        Map<String, Object> user = auth.require(header);
        Long id = data.jdbc().queryForObject("""
            INSERT INTO warehouses(name,code,city,address,capacity_units,active,updated_at) VALUES (?,?,?,?,?,?,NOW()) RETURNING id
            """, Long.class,
            DataService.text(body, "name", null), DataService.text(body, "code", null), DataService.text(body, "city", "Tashkent"),
            DataService.text(body, "address", ""), DataService.integer(body, "capacityUnits", 0), DataService.bool(body, "active", true));
        activity.log(user, "Warehouses", "created", "Warehouse created: " + DataService.text(body, "name", ""));
        return data.row("SELECT * FROM warehouses WHERE id=?", id);
    }

    @PutMapping("/{id}")
    public Object update(@RequestHeader("Authorization") String header, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        auth.require(header);
        data.jdbc().update("""
            UPDATE warehouses SET name=?, code=?, city=?, address=?, capacity_units=?, active=?, updated_at=NOW() WHERE id=?
            """, DataService.text(body, "name", null), DataService.text(body, "code", null), DataService.text(body, "city", "Tashkent"),
            DataService.text(body, "address", ""), DataService.integer(body, "capacityUnits", 0), DataService.bool(body, "active", true), id);
        return data.row("SELECT * FROM warehouses WHERE id=?", id);
    }

    @DeleteMapping("/{id}")
    public Object delete(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        data.jdbc().update("DELETE FROM inventory WHERE warehouse_id=?", id);
        data.jdbc().update("DELETE FROM warehouses WHERE id=?", id);
        return data.ok("Warehouse deleted");
    }

    @GetMapping("/{id}/inventory")
    public Object inventory(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        return data.rows("""
            SELECT i.id, i.product_id, p.name AS product_name, p.sku, i.warehouse_id, w.name AS warehouse_name,
                   i.quantity, i.reserved_quantity, (i.quantity-i.reserved_quantity) AS available_quantity, p.minimum_stock
            FROM inventory i JOIN products p ON p.id=i.product_id JOIN warehouses w ON w.id=i.warehouse_id
            WHERE i.warehouse_id=? ORDER BY p.name
            """, id);
    }
}
