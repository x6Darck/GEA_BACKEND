package com.calendario.callapp.callapp_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;



@Configuration
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class AppSecurityProperties {

    /**
     * Lista de orígenes permitidos para CORS (separados por coma en properties).
     */
    private String corsAllowedOrigins;

    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class RateLimit {
        /**
         * Si el limitador de peticiones está habilitado.
         */
        private boolean enabled = true;

        /**
         * Máximo de peticiones permitidas por minuto para endpoints de auth.
         */
        private int requestsPerMinute = 10;
    }
}
