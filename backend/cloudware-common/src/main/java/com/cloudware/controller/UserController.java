package com.cloudware.controller;

import com.cloudware.annotation.Microservice;
import com.cloudware.service.ActivityService;
import com.cloudware.service.AuthService;
import com.cloudware.service.DataService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Microservice("identity")
@RestController
@RequestMapping("/api")
public class UserController {
    private final DataService data;
    private final AuthService auth;
    private final ActivityService activity;

    public UserController(DataService data, AuthService auth, ActivityService activity) {
        this.data = data;
        this.auth = auth;
        this.activity = activity;
    }

    @GetMapping("/users")
    public Object users(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("SELECT id, full_name, email, role, department, phone, active, created_at, updated_at FROM app_users ORDER BY id DESC");
    }

    @GetMapping("/users/{id}")
    public Object user(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        Map<String, Object> u = data.row("SELECT id, full_name, email, role, department, phone, active, created_at, updated_at FROM app_users WHERE id=?", id);
        u.put("permissions", auth.permissionsFor(String.valueOf(u.get("role"))));
        return u;
    }

    @PostMapping("/users")
    public Object createUser(@RequestHeader("Authorization") String header, @RequestBody Map<String, Object> body) {
        Map<String, Object> current = auth.require(header);
        Long id = data.jdbc().queryForObject("""
            INSERT INTO app_users(full_name,email,password,role,department,phone,active,updated_at)
            VALUES (?,?,?,?,?,?,?,NOW()) RETURNING id
            """, Long.class,
            DataService.text(body, "fullName", null), DataService.text(body, "email", null), DataService.text(body, "password", "password123"),
            DataService.text(body, "role", "VIEWER"), DataService.text(body, "department", "General"), DataService.text(body, "phone", ""), DataService.bool(body, "active", true));
        data.jdbc().update("""
            INSERT INTO user_roles(user_id, role_id)
            SELECT ?, id FROM roles WHERE name=? ON CONFLICT DO NOTHING
            """, id, DataService.text(body, "role", "VIEWER"));
        activity.log(current, "Users", "created", "User created: " + DataService.text(body, "email", ""));
        return user(header, id);
    }

    @PutMapping("/users/{id}")
    public Object updateUser(@RequestHeader("Authorization") String header, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        auth.require(header);
        data.jdbc().update("""
            UPDATE app_users SET full_name=?, email=?, role=?, department=?, phone=?, active=?, updated_at=NOW() WHERE id=?
            """, DataService.text(body, "fullName", null), DataService.text(body, "email", null), DataService.text(body, "role", "VIEWER"),
            DataService.text(body, "department", "General"), DataService.text(body, "phone", ""), DataService.bool(body, "active", true), id);
        data.jdbc().update("DELETE FROM user_roles WHERE user_id=?", id);
        data.jdbc().update("INSERT INTO user_roles(user_id, role_id) SELECT ?, id FROM roles WHERE name=? ON CONFLICT DO NOTHING", id, DataService.text(body, "role", "VIEWER"));
        return user(header, id);
    }

    @DeleteMapping("/users/{id}")
    public Object deleteUser(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        data.jdbc().update("DELETE FROM app_users WHERE id=?", id);
        return data.ok("User deleted");
    }

    @PatchMapping("/users/{id}/status")
    public Object status(@RequestHeader("Authorization") String header, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        auth.require(header);
        data.jdbc().update("UPDATE app_users SET active=?, updated_at=NOW() WHERE id=?", DataService.bool(body, "active", true), id);
        return user(header, id);
    }

    @GetMapping("/roles")
    public Object roles(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("SELECT * FROM roles ORDER BY name");
    }

    @PostMapping("/roles")
    public Object createRole(@RequestHeader("Authorization") String header, @RequestBody Map<String, Object> body) {
        auth.require(header);
        Long id = data.jdbc().queryForObject("INSERT INTO roles(name,description) VALUES (?,?) RETURNING id", Long.class,
            DataService.text(body, "name", null), DataService.text(body, "description", ""));
        return data.row("SELECT * FROM roles WHERE id=?", id);
    }

    @PutMapping("/roles/{id}")
    public Object updateRole(@RequestHeader("Authorization") String header, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        auth.require(header);
        data.jdbc().update("UPDATE roles SET name=?, description=? WHERE id=?", DataService.text(body, "name", null), DataService.text(body, "description", ""), id);
        return data.row("SELECT * FROM roles WHERE id=?", id);
    }

    @DeleteMapping("/roles/{id}")
    public Object deleteRole(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        data.jdbc().update("DELETE FROM roles WHERE id=?", id);
        return data.ok("Role deleted");
    }
}
