package com.nicolas.appsec.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AdminController {

    @GetMapping("/api/v1/admin")
    public Map<String, Object> admin() {
        return Map.of(
                "status", "ok",
                "area", "admin"
        );
    }
}