package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long tokenExpiration;

    @PostConstruct
    public void validarConfiguracion() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("jwt.secret no puede estar vacío. Configura la variable JWT_SECRET.");
        }
        if (secretKey.length() < 32) {
            throw new IllegalStateException("jwt.secret debe tener al menos 32 caracteres por seguridad.");
        }
        if (secretKey.startsWith("dev_only_") && !isDevEnvironment()) {
            log.warn("ADVERTENCIA DE SEGURIDAD: Se está usando el JWT secret de desarrollo. Define JWT_SECRET en producción.");
        }
        log.info("Configuración JWT validada correctamente.");
    }

    private boolean isDevEnvironment() {
        String profile = System.getProperty("spring.profiles.active", "");
        return profile.contains("dev") || profile.isEmpty();
    }

    public String generarToken(Usuario usuario) {
        return Jwts.builder()
                .setSubject(usuario.getCorreo())
                .claim("id", usuario.getId())
                .claim("rol", usuario.getRol().getSecurityRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + tokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRol(String token) {
        return extractClaim(token, claims -> claims.get("rol", String.class));
    }

    public boolean isTokenValid(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration != null && expiration.after(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claimsResolver.apply(claims);
        } catch (Exception e) {
            return null;
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
