package com.cloudware.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ActivityService {
    private final JdbcTemplate jdbc;

    public ActivityService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void log(String module, String action, String description) {
        log(null, null, module, action, description);
    }

    public void log(Map<String, Object> user, String module, String action, String description) {
        Long userId = null;
        String userName = "System";
        if (user != null) {
            Object id = user.get("id");
            if (id instanceof Number n) userId = n.longValue();
            Object name = user.get("fullName");
            if (name != null) userName = String.valueOf(name);
        }
        log(userId, userName, module, action, description);
    }

    private void log(Long userId, String userName, String module, String action, String description) {
        jdbc.update(
            "INSERT INTO activity_log (user_id,user_name,module,action,description,created_at) VALUES (?,?,?,?,?,NOW())",
            userId, userName, module, action, description
        );
    }
}
