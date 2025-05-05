package com.couponmoa.backend.couponmoastore.common.util;

import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;

@Component
public class YamlDebugChecker {
    private final Environment env;

    public YamlDebugChecker(Environment env) {
        this.env = env;
    }

    @PostConstruct
    public void logConfigDetails() {
        System.out.println("🟡 [DEBUG] Active Profiles: " + String.join(", ", env.getActiveProfiles()));
        System.out.println("🟡 [DEBUG] spring.datasource.url: " + env.getProperty("spring.datasource.url"));
        System.out.println("🟡 [DEBUG] spring.datasource.username: " + env.getProperty("spring.datasource.username"));
        System.out.println("🟡 [DEBUG] REDIS_HOST (env): " + System.getenv("REDIS_HOST"));
        System.out.println("🟡 [DEBUG] spring.redis.host: " + env.getProperty("spring.data.redis.host"));
    }
}
