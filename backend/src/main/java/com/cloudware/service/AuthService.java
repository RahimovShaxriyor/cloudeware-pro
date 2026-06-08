package com.cloudware.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class AuthService {
    private final DataService data;
    private final JdbcTemplate jdbc;
    private final ActivityService activity;

    public AuthService(DataService data, ActivityService activity) {
        this.data = data;
        this.jdbc = data.jdbc();
        this.activity = activity;
    }

    public Map<String, Object> login(Map<String, Object> request) {
        String email = DataService.text(request, "email", "");
        String password = DataService.text(request, "password", "");
        List<Map<String, Object>> users = data.rows(
            """
            SELECT id, full_name, email, role, department, phone, active
            FROM app_users
            WHERE LOWER(email) = LOWER(?) AND password = ?
            """,
            email, password
        );
        if (users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        Map<String, Object> user = users.get(0);
        if (Boolean.FALSE.equals(user.get("active"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is deactivated");
        }
        user.put("permissions", permissionsFor(String.valueOf(user.get("role"))));
        String token = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO auth_tokens(token,user_id,created_at) VALUES (?,?,NOW())", token, ((Number) user.get("id")).longValue());
        activity.log(user, "Auth", "login", "User logged in: " + user.get("email"));
        return Map.of("token", token, "user", user);
    }

    public Map<String, Object> require(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Bearer token");
        }
        String token = authHeader.substring(7);
        List<Map<String, Object>> users = data.rows("""
            SELECT u.id, u.full_name, u.email, u.role, u.department, u.phone, u.active
            FROM auth_tokens t
            JOIN app_users u ON u.id=t.user_id
            WHERE t.token=?
            """, token);
        if (users.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        Map<String, Object> user = users.get(0);
        if (Boolean.FALSE.equals(user.get("active"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is deactivated");
        user.put("permissions", permissionsFor(String.valueOf(user.get("role"))));
        return user;
    }

    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) jdbc.update("DELETE FROM auth_tokens WHERE token=?", authHeader.substring(7));
    }

    public Map<String, Object> updateProfile(Map<String, Object> user, Map<String, Object> body) {
        jdbc.update(
            "UPDATE app_users SET full_name=?, phone=?, updated_at=NOW() WHERE id=?",
            DataService.text(body, "fullName", String.valueOf(user.get("fullName"))),
            DataService.text(body, "phone", String.valueOf(user.getOrDefault("phone", ""))),
            ((Number) user.get("id")).longValue()
        );
        Map<String, Object> updated = data.row("SELECT id, full_name, email, role, department, phone, active FROM app_users WHERE id=?", ((Number) user.get("id")).longValue());
        updated.put("permissions", permissionsFor(String.valueOf(updated.get("role"))));
        activity.log(updated, "Auth", "profile_updated", "Profile updated");
        return updated;
    }

    public Map<String, Object> changePassword(Map<String, Object> user, Map<String, Object> body) {
        String current = DataService.text(body, "currentPassword", "");
        String next = DataService.text(body, "newPassword", "");
        if (next.length() < 6) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least 6 characters");
        Long matches = data.longValue("SELECT COUNT(*) FROM app_users WHERE id=? AND password=?", ((Number) user.get("id")).longValue(), current);
        if (matches == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        jdbc.update("UPDATE app_users SET password=?, updated_at=NOW() WHERE id=?", next, ((Number) user.get("id")).longValue());
        activity.log(user, "Auth", "password_changed", "Password changed");
        return data.ok("Password changed");
    }

    public List<String> permissionsFor(String role) {
        return data.rows(
            """
            SELECT p.code FROM role_permissions rp
            JOIN roles r ON r.id = rp.role_id
            JOIN permissions p ON p.id = rp.permission_id
            WHERE r.name = ?
            ORDER BY p.code
            """, role
        ).stream().map(row -> String.valueOf(row.get("code"))).toList();
    }
}
