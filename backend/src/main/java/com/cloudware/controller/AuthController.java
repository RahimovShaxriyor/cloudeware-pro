package com.cloudware.controller;

import com.cloudware.service.AuthService;
import com.cloudware.service.DataService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final DataService data;

    public AuthController(AuthService auth, DataService data) {
        this.auth = auth;
        this.data = data;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        return auth.login(body);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String header) {
        auth.logout(header);
        return data.ok("Logged out");
    }

    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader("Authorization") String header) {
        return auth.require(header);
    }

    @PutMapping("/profile")
    public Map<String, Object> profile(@RequestHeader("Authorization") String header, @RequestBody Map<String, Object> body) {
        return auth.updateProfile(auth.require(header), body);
    }

    @PutMapping("/change-password")
    public Map<String, Object> changePassword(@RequestHeader("Authorization") String header, @RequestBody Map<String, Object> body) {
        return auth.changePassword(auth.require(header), body);
    }
}
