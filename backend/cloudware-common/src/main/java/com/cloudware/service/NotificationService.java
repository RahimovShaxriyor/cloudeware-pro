package com.cloudware.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final JdbcTemplate jdbc;

    public NotificationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(String type, String title, String message) {
        jdbc.update(
            "INSERT INTO notifications (type,title,message,is_read,created_at) VALUES (?,?,?,FALSE,NOW())",
            type, title, message
        );
    }
}
