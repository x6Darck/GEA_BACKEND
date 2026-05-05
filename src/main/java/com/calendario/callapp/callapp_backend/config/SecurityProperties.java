package com.calendario.callapp.callapp_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "app.security")
@Data
public class SecurityProperties {

    private Cors cors = new Cors();
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class Cors {
        private String allowedOrigins;
    }

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private int requestsPerMinute = 10;
    }
}
