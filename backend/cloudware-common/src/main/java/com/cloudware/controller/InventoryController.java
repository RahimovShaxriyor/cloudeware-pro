package com.cloudware.controller;

import com.cloudware.annotation.Microservice;
import com.cloudware.service.ActivityService;
import com.cloudware.service.AuthService;
import com.cloudware.service.DataService;
import com.cloudware.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Microservice("wms")
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final DataService data;
    private final AuthService auth;
    private final ActivityService activity;
    private final NotificationService notifications;

    public InventoryController(DataService data, AuthService auth, ActivityService activity, NotificationService notifications) {
        this.data = data;
        this.auth = auth;
        this.activity = activity;
        this.notifications = notifications;
    }

    @GetMapping
    public Object all(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return inventoryQuery("", new Object[]{});
    }

    @GetMapping("/{id}")
    public Object one(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        return data.row(baseSql() + " WHERE i.id=?", id);
    }

    @GetMapping("/product/{productId}")
    public Object byProduct(@RequestHeader("Authorization") String header, @PathVariable Long productId) {
        auth.require(header);
        return inventoryQuery(" WHERE i.product_id=?", new Object[]{productId});
    }

    @GetMapping("/warehouse/{warehouseId}")
    public Object byWarehouse(@RequestHeader("Authorization") String header, @PathVariable Long warehouseId) {
        auth.require(header);
        return inventoryQuery(" WHERE i.warehouse_id=?", new Object[]{warehouseId});
    }

    @PostMapping("/adjust")
    public Object adjust(@RequestHeader("Authorization") String header, @RequestBody Map<String, Object> body) {
        Map<String, Object> user = auth.require(header);
        Long productId = DataService.id(body, "productId");
        Long warehouseId = DataService.id(body, "warehouseId");
        int quantity = DataService.integer(body, "quantity", 0);
        String reason = DataService.text(body, "reason", "Manual stock adjustment");
        if (productId == null || warehouseId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product and warehouse are required");
        data.jdbc().update("""
            INSERT INTO inventory(product_id,warehouse_id,quantity,reserved_quantity,updated_at)
            VALUES (?,?,?,0,NOW())
            ON CONFLICT(product_id,warehouse_id) DO UPDATE SET quantity=inventory.quantity + EXCLUDED.quantity, updated_at=NOW()
            """, productId, warehouseId, quantity);
        data.jdbc().update("""
            INSERT INTO inventory_movements(product_id,warehouse_id,type,quantity,reason,created_by,created_at)
            VALUES (?,?,?,?,?,?,NOW())
            """, productId, warehouseId, "ADJUSTMENT", quantity, reason, user.get("fullName"));
        activity.log(user, "Inventory", "adjusted", "Stock adjusted by " + quantity + " units");
        notifications.create("STOCK_ADJUSTMENT", "Stock adjusted", reason);
        return inventoryQuery(" WHERE i.product_id=? AND i.warehouse_id=?", new Object[]{productId, warehouseId}).get(0);
    }

    @PostMapping("/transfer")
    public Object transfer(@RequestHeader("Authorization") String header, @RequestBody Map<String, Object> body) {
        Map<String, Object> user = auth.require(header);
        Long productId = DataService.id(body, "productId");
        Long from = DataService.id(body, "fromWarehouseId");
        Long to = DataService.id(body, "toWarehouseId");
        int quantity = DataService.integer(body, "quantity", 0);
        String reason = DataService.text(body, "reason", "Warehouse transfer");
        if (productId == null || from == null || to == null || from.equals(to)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid product, from warehouse and to warehouse are required");
        if (quantity <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be positive");
        Integer available = data.jdbc().queryForObject("SELECT quantity-reserved_quantity FROM inventory WHERE product_id=? AND warehouse_id=?", Integer.class, productId, from);
        if (available == null || available < quantity) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough available stock for transfer");
        data.jdbc().update("UPDATE inventory SET quantity=quantity-?, updated_at=NOW() WHERE product_id=? AND warehouse_id=?", quantity, productId, from);
        data.jdbc().update("""
            INSERT INTO inventory(product_id,warehouse_id,quantity,reserved_quantity,updated_at)
            VALUES (?,?,?,0,NOW())
            ON CONFLICT(product_id,warehouse_id) DO UPDATE SET quantity=inventory.quantity + EXCLUDED.quantity, updated_at=NOW()
            """, productId, to, quantity);
        data.jdbc().update("""
            INSERT INTO inventory_movements(product_id,from_warehouse_id,to_warehouse_id,type,quantity,reason,created_by,created_at)
            VALUES (?,?,?,?,?,?,?,NOW())
            """, productId, from, to, "TRANSFER", quantity, reason, user.get("fullName"));
        activity.log(user, "Inventory", "transferred", "Transferred " + quantity + " units between warehouses");
        notifications.create("WAREHOUSE_TRANSFER", "Warehouse transfer completed", reason);
        return data.ok("Stock transferred");
    }

    @GetMapping("/low-stock")
    public Object lowStock(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return inventoryQuery(" WHERE (i.quantity-i.reserved_quantity) <= p.minimum_stock", new Object[]{});
    }

    @GetMapping("/movements")
    public Object movements(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("""
            SELECT m.*, p.name AS product_name, w.name AS warehouse_name, fw.name AS from_warehouse_name, tw.name AS to_warehouse_name
            FROM inventory_movements m
            JOIN products p ON p.id=m.product_id
            LEFT JOIN warehouses w ON w.id=m.warehouse_id
            LEFT JOIN warehouses fw ON fw.id=m.from_warehouse_id
            LEFT JOIN warehouses tw ON tw.id=m.to_warehouse_id
            ORDER BY m.id DESC LIMIT 100
            """);
    }

    private java.util.List<Map<String, Object>> inventoryQuery(String where, Object[] args) {
        return data.rows(baseSql() + where + " ORDER BY p.name, w.name", args);
    }

    private String baseSql() {
        return """
            SELECT i.id, i.product_id, p.name AS product_name, p.sku, p.category, p.minimum_stock,
                   i.warehouse_id, w.name AS warehouse_name, w.city AS warehouse_city,
                   i.quantity, i.reserved_quantity, (i.quantity-i.reserved_quantity) AS available_quantity,
                   CASE WHEN (i.quantity-i.reserved_quantity) <= p.minimum_stock THEN TRUE ELSE FALSE END AS low_stock
            FROM inventory i
            JOIN products p ON p.id=i.product_id
            JOIN warehouses w ON w.id=i.warehouse_id
            """;
    }
}
