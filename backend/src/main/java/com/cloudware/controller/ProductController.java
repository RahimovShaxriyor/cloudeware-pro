package com.cloudware.controller;

import com.cloudware.service.ActivityService;
import com.cloudware.service.AuthService;
import com.cloudware.service.DataService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final DataService data;
    private final AuthService auth;
    private final ActivityService activity;

    public ProductController(DataService data, AuthService auth, ActivityService activity) {
        this.data = data;
        this.auth = auth;
        this.activity = activity;
    }

    @GetMapping
    public Object all(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("SELECT * FROM products ORDER BY id DESC");
    }

    @GetMapping("/{id}")
    public Object one(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        return data.row("SELECT * FROM products WHERE id=?", id);
    }

    @GetMapping("/search")
    public Object search(@RequestHeader("Authorization") String header, @RequestParam(defaultValue = "") String query) {
        auth.require(header);
        String q = "%" + query.toLowerCase() + "%";
        return data.rows("""
            SELECT * FROM products
            WHERE LOWER(name) LIKE ? OR LOWER(sku) LIKE ? OR LOWER(category) LIKE ? OR LOWER(brand) LIKE ?
            ORDER BY id DESC
            """, q, q, q, q);
    }

    @PostMapping
    public Object create(@RequestHeader("Authorization") String header, @RequestBody Map<String, Object> body) {
        Map<String, Object> user = auth.require(header);
        validate(body);
        Long id = data.jdbc().queryForObject("""
            INSERT INTO products(sku,name,description,category,brand,size_range,color,season,wholesale_price,retail_price,minimum_stock,active,updated_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,NOW()) RETURNING id
            """, Long.class,
            DataService.text(body, "sku", null), DataService.text(body, "name", null), DataService.text(body, "description", ""),
            DataService.text(body, "category", "General"), DataService.text(body, "brand", "CloudWear"), DataService.text(body, "sizeRange", "S-XL"),
            DataService.text(body, "color", "Mixed"), DataService.text(body, "season", "All season"),
            DataService.decimal(body, "wholesalePrice", BigDecimal.ZERO), DataService.decimal(body, "retailPrice", BigDecimal.ZERO),
            DataService.integer(body, "minimumStock", 0), DataService.bool(body, "active", true));
        activity.log(user, "Products", "created", "Product created: " + DataService.text(body, "name", ""));
        return data.row("SELECT * FROM products WHERE id=?", id);
    }

    @PutMapping("/{id}")
    public Object update(@RequestHeader("Authorization") String header, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> user = auth.require(header);
        validate(body);
        data.jdbc().update("""
            UPDATE products SET sku=?, name=?, description=?, category=?, brand=?, size_range=?, color=?, season=?,
                wholesale_price=?, retail_price=?, minimum_stock=?, active=?, updated_at=NOW()
            WHERE id=?
            """,
            DataService.text(body, "sku", null), DataService.text(body, "name", null), DataService.text(body, "description", ""),
            DataService.text(body, "category", "General"), DataService.text(body, "brand", "CloudWear"), DataService.text(body, "sizeRange", "S-XL"),
            DataService.text(body, "color", "Mixed"), DataService.text(body, "season", "All season"),
            DataService.decimal(body, "wholesalePrice", BigDecimal.ZERO), DataService.decimal(body, "retailPrice", BigDecimal.ZERO),
            DataService.integer(body, "minimumStock", 0), DataService.bool(body, "active", true), id);
        activity.log(user, "Products", "updated", "Product updated: " + id);
        return data.row("SELECT * FROM products WHERE id=?", id);
    }

    @DeleteMapping("/{id}")
    public Object delete(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        Map<String, Object> user = auth.require(header);
        if (data.longValue("SELECT COUNT(*) FROM order_items WHERE product_id=?", id) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product is used in orders and cannot be deleted");
        }
        data.jdbc().update("DELETE FROM inventory WHERE product_id=?", id);
        data.jdbc().update("DELETE FROM products WHERE id=?", id);
        activity.log(user, "Products", "deleted", "Product deleted: " + id);
        return data.ok("Product deleted");
    }

    @GetMapping("/categories")
    public Object categories(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("SELECT * FROM product_categories ORDER BY name");
    }

    @PostMapping("/categories")
    public Object createCategory(@RequestHeader("Authorization") String header, @RequestBody Map<String, Object> body) {
        Map<String, Object> user = auth.require(header);
        Long id = data.jdbc().queryForObject("INSERT INTO product_categories(name,description,active,updated_at) VALUES (?,?,?,NOW()) RETURNING id", Long.class,
            DataService.text(body, "name", null), DataService.text(body, "description", ""), DataService.bool(body, "active", true));
        activity.log(user, "Products", "category_created", "Category created: " + DataService.text(body, "name", ""));
        return data.row("SELECT * FROM product_categories WHERE id=?", id);
    }

    @PutMapping("/categories/{id}")
    public Object updateCategory(@RequestHeader("Authorization") String header, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        auth.require(header);
        data.jdbc().update("UPDATE product_categories SET name=?, description=?, active=?, updated_at=NOW() WHERE id=?",
            DataService.text(body, "name", null), DataService.text(body, "description", ""), DataService.bool(body, "active", true), id);
        return data.row("SELECT * FROM product_categories WHERE id=?", id);
    }

    @DeleteMapping("/categories/{id}")
    public Object deleteCategory(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        data.jdbc().update("DELETE FROM product_categories WHERE id=?", id);
        return data.ok("Category deleted");
    }

    private void validate(Map<String, Object> body) {
        if (DataService.text(body, "sku", "").isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU is required");
        if (DataService.text(body, "name", "").isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        if (DataService.decimal(body, "wholesalePrice", BigDecimal.ZERO).compareTo(BigDecimal.ZERO) < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wholesale price must be positive");
        if (DataService.decimal(body, "retailPrice", BigDecimal.ZERO).compareTo(BigDecimal.ZERO) < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Retail price must be positive");
        if (DataService.integer(body, "minimumStock", 0) < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum stock must be positive");
    }
}
