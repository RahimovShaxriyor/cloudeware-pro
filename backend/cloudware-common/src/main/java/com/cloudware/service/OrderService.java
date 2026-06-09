package com.cloudware.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
    private final DataService data;
    private final JdbcTemplate jdbc;
    private final ActivityService activity;
    private final NotificationService notifications;

    public OrderService(DataService data, ActivityService activity, NotificationService notifications) {
        this.data = data;
        this.jdbc = data.jdbc();
        this.activity = activity;
        this.notifications = notifications;
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> user, Map<String, Object> body) {
        Long customerId = DataService.id(body, "customerId");
        if (customerId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer is required");
        String customerName = data.row("SELECT company_name FROM customers WHERE id=?", customerId).get("companyName").toString();
        String number = DataService.text(body, "orderNumber", "");
        if (number.isBlank()) number = "CW-" + System.currentTimeMillis();
        Long id = jdbc.queryForObject("""
            INSERT INTO orders(order_number,customer_id,customer_name,status,priority,delivery_city,delivery_address,discount,delivery_fee,notes,updated_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,NOW()) RETURNING id
            """, Long.class,
            number, customerId, customerName, DataService.text(body, "status", "DRAFT"), DataService.text(body, "priority", "NORMAL"),
            DataService.text(body, "deliveryCity", "Tashkent"), DataService.text(body, "deliveryAddress", ""),
            DataService.decimal(body, "discount", BigDecimal.ZERO), DataService.decimal(body, "deliveryFee", BigDecimal.ZERO), DataService.text(body, "notes", ""));
        Object items = body.get("items");
        if (items instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> raw) addItem(id, cast(raw));
            }
        }
        recalc(id);
        activity.log(user, "Orders", "created", "Order created: " + number);
        notifications.create("NEW_ORDER", "New order", "Order " + number + " was created");
        return full(id);
    }

    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        jdbc.update("""
            UPDATE orders SET customer_id=?, customer_name=(SELECT company_name FROM customers WHERE id=?), status=?, priority=?, delivery_city=?, delivery_address=?, discount=?, delivery_fee=?, notes=?, updated_at=NOW()
            WHERE id=?
            """,
            DataService.id(body, "customerId"), DataService.id(body, "customerId"), DataService.text(body, "status", "DRAFT"), DataService.text(body, "priority", "NORMAL"),
            DataService.text(body, "deliveryCity", "Tashkent"), DataService.text(body, "deliveryAddress", ""), DataService.decimal(body, "discount", BigDecimal.ZERO),
            DataService.decimal(body, "deliveryFee", BigDecimal.ZERO), DataService.text(body, "notes", ""), id);
        recalc(id);
        return full(id);
    }

    @Transactional
    public Map<String, Object> addItem(Long orderId, Map<String, Object> body) {
        Long productId = DataService.id(body, "productId");
        int quantity = DataService.integer(body, "quantity", 1);
        if (productId == null || quantity <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product and positive quantity are required");
        Map<String, Object> product = data.row("SELECT * FROM products WHERE id=?", productId);
        BigDecimal unit = DataService.decimal(body, "unitPrice", new BigDecimal(product.get("wholesalePrice").toString()));
        BigDecimal total = unit.multiply(BigDecimal.valueOf(quantity));
        jdbc.update("""
            INSERT INTO order_items(order_id,product_id,product_name,sku,quantity,unit_price,total_price)
            VALUES (?,?,?,?,?,?,?)
            ON CONFLICT(order_id, product_id) DO UPDATE SET quantity=order_items.quantity+EXCLUDED.quantity, total_price=(order_items.quantity+EXCLUDED.quantity)*EXCLUDED.unit_price, unit_price=EXCLUDED.unit_price
            """, orderId, productId, product.get("name"), product.get("sku"), quantity, unit, total);
        recalc(orderId);
        return full(orderId);
    }

    @Transactional
    public Map<String, Object> removeItem(Long orderId, Long itemId) {
        jdbc.update("DELETE FROM order_items WHERE id=? AND order_id=?", itemId, orderId);
        recalc(orderId);
        return full(orderId);
    }

    @Transactional
    public Map<String, Object> confirm(Map<String, Object> user, Long orderId) {
        List<Map<String, Object>> items = data.rows("SELECT * FROM order_items WHERE order_id=?", orderId);
        if (items.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot confirm order without items");
        for (Map<String, Object> item : items) {
            Long productId = ((Number) item.get("productId")).longValue();
            int required = ((Number) item.get("quantity")).intValue();
            int available = data.longValue("SELECT COALESCE(SUM(quantity-reserved_quantity),0) FROM inventory WHERE product_id=?", productId).intValue();
            if (available < required) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough stock for " + item.get("productName"));
        }
        for (Map<String, Object> item : items) reserve(((Number) item.get("productId")).longValue(), ((Number) item.get("quantity")).intValue());
        jdbc.update("UPDATE orders SET status='CONFIRMED', updated_at=NOW() WHERE id=?", orderId);
        activity.log(user, "Orders", "confirmed", "Order confirmed: " + orderId);
        return full(orderId);
    }

    @Transactional
    public Map<String, Object> cancel(Map<String, Object> user, Long orderId) {
        release(orderId);
        jdbc.update("UPDATE orders SET status='CANCELLED', updated_at=NOW() WHERE id=?", orderId);
        activity.log(user, "Orders", "cancelled", "Order cancelled: " + orderId);
        return full(orderId);
    }

    @Transactional
    public Map<String, Object> ship(Map<String, Object> user, Long orderId) {
        jdbc.update("UPDATE orders SET status='SHIPPED', updated_at=NOW() WHERE id=?", orderId);
        activity.log(user, "Orders", "shipped", "Order shipped: " + orderId);
        return full(orderId);
    }

    @Transactional
    public Map<String, Object> deliver(Map<String, Object> user, Long orderId) {
        for (Map<String, Object> item : data.rows("SELECT * FROM order_items WHERE order_id=?", orderId)) {
            Long productId = ((Number) item.get("productId")).longValue();
            int remaining = ((Number) item.get("quantity")).intValue();
            for (Map<String, Object> row : data.rows("SELECT * FROM inventory WHERE product_id=? AND reserved_quantity>0 ORDER BY reserved_quantity DESC", productId)) {
                long invId = ((Number) row.get("id")).longValue();
                int reserved = ((Number) row.get("reservedQuantity")).intValue();
                int take = Math.min(remaining, reserved);
                if (take <= 0) continue;
                jdbc.update("UPDATE inventory SET quantity=quantity-?, reserved_quantity=reserved_quantity-?, updated_at=NOW() WHERE id=?", take, take, invId);
                remaining -= take;
                if (remaining == 0) break;
            }
        }
        jdbc.update("UPDATE orders SET status='DELIVERED', updated_at=NOW() WHERE id=?", orderId);
        activity.log(user, "Orders", "delivered", "Order delivered: " + orderId);
        notifications.create("ORDER_DELIVERED", "Order delivered", "Order " + orderId + " was delivered");
        return full(orderId);
    }

    public Map<String, Object> full(Long id) {
        Map<String, Object> order = data.row("SELECT * FROM orders WHERE id=?", id);
        order.put("items", data.rows("SELECT * FROM order_items WHERE order_id=? ORDER BY id", id));
        return order;
    }

    public void recalc(Long orderId) {
        BigDecimal subtotal = jdbc.queryForObject("SELECT COALESCE(SUM(total_price),0) FROM order_items WHERE order_id=?", BigDecimal.class, orderId);
        if (subtotal == null) subtotal = BigDecimal.ZERO;
        BigDecimal discount = jdbc.queryForObject("SELECT COALESCE(discount,0) FROM orders WHERE id=?", BigDecimal.class, orderId);
        BigDecimal delivery = jdbc.queryForObject("SELECT COALESCE(delivery_fee,0) FROM orders WHERE id=?", BigDecimal.class, orderId);
        BigDecimal taxPercent = BigDecimal.ZERO;
        try {
            String value = jdbc.queryForObject("SELECT setting_value FROM app_settings WHERE setting_key='taxPercent'", String.class);
            taxPercent = new BigDecimal(value == null ? "0" : value);
        } catch (Exception ignored) {}
        BigDecimal tax = subtotal.multiply(taxPercent).divide(BigDecimal.valueOf(100));
        BigDecimal total = subtotal.subtract(discount == null ? BigDecimal.ZERO : discount).add(tax).add(delivery == null ? BigDecimal.ZERO : delivery);
        jdbc.update("UPDATE orders SET subtotal=?, tax=?, total_amount=?, updated_at=NOW() WHERE id=?", subtotal, tax, total, orderId);
    }

    private void reserve(Long productId, int quantity) {
        int remaining = quantity;
        for (Map<String, Object> row : data.rows("SELECT id, quantity, reserved_quantity, (quantity-reserved_quantity) AS available_quantity FROM inventory WHERE product_id=? AND (quantity-reserved_quantity)>0 ORDER BY available_quantity DESC", productId)) {
            long invId = ((Number) row.get("id")).longValue();
            int available = ((Number) row.get("availableQuantity")).intValue();
            int take = Math.min(remaining, available);
            jdbc.update("UPDATE inventory SET reserved_quantity=reserved_quantity+?, updated_at=NOW() WHERE id=?", take, invId);
            remaining -= take;
            if (remaining == 0) break;
        }
    }

    private void release(Long orderId) {
        for (Map<String, Object> item : data.rows("SELECT * FROM order_items WHERE order_id=?", orderId)) {
            Long productId = ((Number) item.get("productId")).longValue();
            int remaining = ((Number) item.get("quantity")).intValue();
            for (Map<String, Object> row : data.rows("SELECT * FROM inventory WHERE product_id=? AND reserved_quantity>0 ORDER BY reserved_quantity DESC", productId)) {
                long invId = ((Number) row.get("id")).longValue();
                int reserved = ((Number) row.get("reservedQuantity")).intValue();
                int take = Math.min(remaining, reserved);
                jdbc.update("UPDATE inventory SET reserved_quantity=reserved_quantity-?, updated_at=NOW() WHERE id=?", take, invId);
                remaining -= take;
                if (remaining == 0) break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Map<?, ?> raw) {
        return (Map<String, Object>) raw;
    }
}
