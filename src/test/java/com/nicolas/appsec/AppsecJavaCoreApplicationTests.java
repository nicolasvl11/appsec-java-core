package com.nicolas.appsec;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Disabled in Sprint 1 to avoid Testcontainers/Docker dependency. Enable in Sprint 2.")
@SpringBootTest
class AppsecJavaCoreApplicationTests {

    @Test
    void contextLoads() {
    }
}
