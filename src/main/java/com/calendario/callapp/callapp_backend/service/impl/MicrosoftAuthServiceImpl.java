package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.dto.request.MicrosoftAuthRequest;
import com.calendario.callapp.callapp_backend.dto.response.AuthResponse;
import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.RolRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import com.calendario.callapp.callapp_backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MicrosoftAuthServiceImpl {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final JwtService jwtService;

    @Value("${microsoft.tenant-id:}")
    private String tenantId = "";

    @Value("${microsoft.client-id:}")
    private String clientId = "";

    public AuthResponse autenticar(MicrosoftAuthRequest request) {
        if (tenantId == null || tenantId.isBlank() || clientId == null || clientId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_IMPLEMENTED,
                    "Configura microsoft.tenant-id y microsoft.client-id para habilitar este login"
            );
        }

        Jwt jwt;
        try {
            JwtDecoder decoder = JwtDecoders.fromIssuerLocation("https://login.microsoftonline.com/" + tenantId + "/v2.0");
            jwt = decoder.decode(request.getIdToken());
        } catch (JwtException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de Microsoft invalido");
        }

        validarAudiencia(jwt);

        String correo = obtenerClaim(jwt, "preferred_username");
        String nombre = obtenerClaim(jwt, "name");
        String oid = obtenerClaim(jwt, "oid");

        Usuario usuario = usuarioRepository.getByCorreoOptimized(correo).orElseGet(Usuario::new);
        if (usuario.getId() == null) {
            usuario.setCorreo(correo);
            usuario.setNombre(nombre != null && !nombre.isBlank() ? nombre : correo);
            usuario.setPassword(UUID.randomUUID().toString());
            usuario.setEstado("ACTIVO");
            usuario.setFechaCreacion(java.time.LocalDateTime.now());
            usuario.setRolEntity(rolRepository.findByNombre("Usuario Autenticado").orElse(null));
            usuario.setAuthProvider(AuthProvider.MICROSOFT);
        } else {
            // 🔐 Validar estado para usuarios existentes
            if (!"ACTIVO".equalsIgnoreCase(usuario.getEstado())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tu cuenta está inactiva. Contacta al administrador.");
            }
        }

        usuario.setMicrosoftOid(oid);
        usuario.setAuthProvider(AuthProvider.MICROSOFT);
        if (usuario.getRolEntity() == null) {
            usuario.setRolEntity(rolRepository.findByNombre("Usuario Autenticado").orElse(null));
        }

        Usuario guardado = usuarioRepository.save(usuario);

        return AuthResponse.builder()
                .token(jwtService.generarToken(guardado))
                .build();
    }

    private void validarAudiencia(Jwt jwt) {
        if (jwt.getAudience() == null || jwt.getAudience().stream().noneMatch(clientId::equals)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El token no pertenece a esta aplicacion");
        }
    }

    private String obtenerClaim(Jwt jwt, String claim) {
        Object value = jwt.getClaims().get(claim);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta el claim requerido: " + claim);
        }
        return value.toString();
    }
}
