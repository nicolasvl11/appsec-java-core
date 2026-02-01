package com.nicolas.appsec.api;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/api/v1/ping")
    public Map<String, Object> ping() {
        return Map.of(
            "status", "ok",
            "service", "appsec-java-core",
            "time", Instant.now().toString()
        );
    }
}
