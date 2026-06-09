package com.cloudware.controller;

import com.cloudware.annotation.Microservice;
import com.cloudware.service.ActivityService;
import com.cloudware.service.AuthService;
import com.cloudware.service.DataService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Microservice("platform")
@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final DataService data;
    private final AuthService auth;
    private final ActivityService activity;

    public SettingsController(DataService data, AuthService auth, ActivityService activity) {
        this.data = data;
        this.auth = auth;
        this.activity = activity;
    }

    @GetMapping
    public Object all(@RequestHeader("Authorization") String header) {
        auth.require(header);
        Map<String, Object> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : data.rows("SELECT * FROM app_settings ORDER BY setting_group, setting_key")) {
            String group = String.valueOf(row.get("settingGroup"));
            String key = String.valueOf(row.get("settingKey"));
            String value = String.valueOf(row.get("settingValue"));
            String type = String.valueOf(row.get("valueType"));
            grouped.computeIfAbsent(group, k -> new LinkedHashMap<String, Object>());
            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) grouped.get(group);
            map.put(key, parse(value, type));
        }
        return grouped;
    }

    @PutMapping
    public Object updateAll(@RequestHeader("Authorization") String header, @RequestBody Map<String, Object> body) {
        Map<String, Object> user = auth.require(header);
        body.forEach((group, value) -> {
            if (value instanceof Map<?, ?> raw) saveGroup(group, cast(raw));
        });
        activity.log(user, "Settings", "updated", "All settings updated");
        return all(header);
    }

    @GetMapping("/{group}")
    public Object getGroup(@RequestHeader("Authorization") String header, @PathVariable String group) {
        auth.require(header);
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map<String, Object> row : data.rows("SELECT * FROM app_settings WHERE setting_group=? ORDER BY setting_key", group)) {
            result.put(String.valueOf(row.get("settingKey")), parse(String.valueOf(row.get("settingValue")), String.valueOf(row.get("valueType"))));
        }
        return result;
    }

    @PutMapping("/{group}")
    public Object putGroup(@RequestHeader("Authorization") String header, @PathVariable String group, @RequestBody Map<String, Object> body) {
        Map<String, Object> user = auth.require(header);
        saveGroup(group, body);
        activity.log(user, "Settings", "changed", "Settings group changed: " + group);
        return getGroup(header, group);
    }

    private void saveGroup(String group, Map<String, Object> body) {
        body.forEach((key, value) -> data.jdbc().update("""
            INSERT INTO app_settings(setting_group,setting_key,setting_value,value_type,updated_at)
            VALUES (?,?,?,?,NOW())
            ON CONFLICT(setting_key) DO UPDATE SET setting_group=EXCLUDED.setting_group, setting_value=EXCLUDED.setting_value, value_type=EXCLUDED.value_type, updated_at=NOW()
            """, group, key, String.valueOf(value), typeOf(value)));
    }

    private Object parse(String value, String type) {
        try {
            return switch (type) {
                case "boolean" -> Boolean.parseBoolean(value);
                case "number" -> value.contains(".") ? Double.parseDouble(value) : Long.parseLong(value);
                default -> value;
            };
        } catch (Exception e) {
            return value;
        }
    }

    private String typeOf(Object value) {
        if (value instanceof Boolean) return "boolean";
        if (value instanceof Number) return "number";
        return "string";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Map<?, ?> raw) {
        return (Map<String, Object>) raw;
    }
}
