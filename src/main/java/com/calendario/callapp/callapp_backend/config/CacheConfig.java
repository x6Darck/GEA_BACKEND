package com.calendario.callapp.callapp_backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("userDetails");
        // TTL de 45 segundos: un usuario baneado deja de autenticarse en máx 45s
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(45, TimeUnit.SECONDS)
                .maximumSize(500));
        return manager;
    }
}
