package com.cloudware.controller;

import com.cloudware.annotation.Microservice;
import com.cloudware.service.AuthService;
import com.cloudware.service.DataService;
import org.springframework.web.bind.annotation.*;

@Microservice("platform")
@RestController
@RequestMapping("/api/activity")
public class ActivityController {
    private final DataService data;
    private final AuthService auth;

    public ActivityController(DataService data, AuthService auth) {
        this.data = data;
        this.auth = auth;
    }

    @GetMapping
    public Object all(@RequestHeader("Authorization") String header) {
        auth.require(header);
        return data.rows("SELECT * FROM activity_log ORDER BY id DESC LIMIT 200");
    }

    @GetMapping("/{id}")
    public Object one(@RequestHeader("Authorization") String header, @PathVariable Long id) {
        auth.require(header);
        return data.row("SELECT * FROM activity_log WHERE id=?", id);
    }

    @GetMapping("/user/{userId}")
    public Object byUser(@RequestHeader("Authorization") String header, @PathVariable Long userId) {
        auth.require(header);
        return data.rows("SELECT * FROM activity_log WHERE user_id=? ORDER BY id DESC", userId);
    }

    @GetMapping("/module/{module}")
    public Object byModule(@RequestHeader("Authorization") String header, @PathVariable String module) {
        auth.require(header);
        return data.rows("SELECT * FROM activity_log WHERE LOWER(module)=LOWER(?) ORDER BY id DESC", module);
    }
}
