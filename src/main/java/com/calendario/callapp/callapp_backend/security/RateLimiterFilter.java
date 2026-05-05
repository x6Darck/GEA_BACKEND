package com.calendario.callapp.callapp_backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiterFilter extends OncePerRequestFilter {

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.security.rate-limit.requests-per-minute:10}")
    private int maxRequests;

    private final Map<String, UserRequests> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        // Skip protection for public files and swagger for performance
        if (path.contains("/archivos/public/") || path.contains("/swagger") || path.contains("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        long currentTime = System.currentTimeMillis() / 60000; // Minute resolution

        UserRequests userRequests = requestCounts.compute(clientIp, (k, v) -> {
            if (v == null || v.minute != currentTime) {
                return new UserRequests(currentTime, new AtomicInteger(1));
            }
            v.count.incrementAndGet();
            return v;
        });

        // Determine limit based on path (more strict for auth)
        int currentMax = path.startsWith("/auth/") || path.contains("/api/auth/") ? maxRequests : maxRequests * 5;

        if (userRequests.count.get() > currentMax) {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false, \"message\":\"Has superado el limite de peticiones. Por favor espera un momento.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Prevent Memory Leak: Clear old IP entries every 10 minutes.
     * This ensures the map doesn't grow indefinitely in production.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 600000) 
    public void cleanupOldEntries() {
        long currentTime = System.currentTimeMillis() / 60000;
        int initialSize = requestCounts.size();
        requestCounts.entrySet().removeIf(entry -> entry.getValue().minute < currentTime);
        if (initialSize > 0) {
            System.out.println("RateLimiter cleanup: Removed entries. Current size: " + requestCounts.size());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class UserRequests {
        final long minute;
        final AtomicInteger count;

        UserRequests(long minute, AtomicInteger count) {
            this.minute = minute;
            this.count = count;
        }
    }
}
