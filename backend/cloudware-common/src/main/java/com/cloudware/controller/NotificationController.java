package com.cloudware.controller;

import com.cloudware.annotation.Microservice;
import com.cloudware.service.AuthService;
import com.cloudware.service.DataService;
import org.springframework.web.bind.annotation.*;

@Microservice("platform")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final DataService data;
    private final AuthService auth;

    public NotificationController(DataService data, AuthService auth) {
        this.data = data;
        this.auth = auth;
    }

    @GetMapping
    public Object all(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("SELECT * FROM notifications ORDER BY id DESC LIMIT 50");
    }

    @PatchMapping("/{id}/read")
    public Object read(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        data.jdbc().update("UPDATE notifications SET is_read=TRUE WHERE id=?", id);
        return data.ok("Notification marked as read");
    }

    @PatchMapping("/read-all")
    public Object readAll(@RequestHeader("Authorization") String header) {
        auth.require(header);
        data.jdbc().update("UPDATE notifications SET is_read=TRUE");
        return data.ok("All notifications marked as read");
    }

    @DeleteMapping("/{id}")
    public Object delete(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        data.jdbc().update("DELETE FROM notifications WHERE id=?", id);
        return data.ok("Notification deleted");
    }
}
