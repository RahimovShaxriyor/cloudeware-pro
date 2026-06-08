package com.cloudware.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class DataService {
    private final JdbcTemplate jdbc;

    public DataService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public JdbcTemplate jdbc() {
        return jdbc;
    }

    public List<Map<String, Object>> rows(String sql, Object... args) {
        return jdbc.queryForList(sql, args).stream().map(this::camelize).toList();
    }

    public Map<String, Object> row(String sql, Object... args) {
        List<Map<String, Object>> list = rows(sql, args);
        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
        }
        return list.get(0);
    }

    public Map<String, Object> maybeRow(String sql, Object... args) {
        List<Map<String, Object>> list = rows(sql, args);
        return list.isEmpty() ? null : list.get(0);
    }

    public Long longValue(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    public Double doubleValue(String sql, Object... args) {
        Double value = jdbc.queryForObject(sql, Double.class, args);
        return value == null ? 0.0 : value;
    }

    public Map<String, Object> ok(String message) {
        return Map.of("success", true, "message", message);
    }

    public Map<String, Object> camelize(Map<String, Object> source) {
        Map<String, Object> out = new LinkedHashMap<>();
        source.forEach((key, value) -> out.put(toCamel(key), normalizeValue(value)));
        return out;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().toString();
        }
        if (value instanceof OffsetDateTime dateTime) {
            return dateTime.toString();
        }
        return value;
    }

    private String toCamel(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        boolean nextUpper = false;
        for (char ch : lower.toCharArray()) {
            if (ch == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                builder.append(Character.toUpperCase(ch));
                nextUpper = false;
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    public static String text(Map<String, Object> body, String key, String fallback) {
        Object value = body.get(key);
        if (value == null) return fallback;
        String string = String.valueOf(value).trim();
        return string.isBlank() ? fallback : string;
    }

    public static Long id(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || String.valueOf(value).isBlank()) return null;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(String.valueOf(value));
    }

    public static Integer integer(Map<String, Object> body, String key, Integer fallback) {
        Object value = body.get(key);
        if (value == null || String.valueOf(value).isBlank()) return fallback;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(String.valueOf(value));
    }

    public static BigDecimal decimal(Map<String, Object> body, String key, BigDecimal fallback) {
        Object value = body.get(key);
        if (value == null || String.valueOf(value).isBlank()) return fallback;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return new BigDecimal(String.valueOf(value));
    }

    public static Boolean bool(Map<String, Object> body, String key, Boolean fallback) {
        Object value = body.get(key);
        if (value == null || String.valueOf(value).isBlank()) return fallback;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
