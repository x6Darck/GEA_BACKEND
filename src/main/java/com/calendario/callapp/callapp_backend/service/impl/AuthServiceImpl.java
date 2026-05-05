package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.dto.request.AuthRequest;
import com.calendario.callapp.callapp_backend.dto.response.AuthResponse;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.entity.Oficina;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import com.calendario.callapp.callapp_backend.security.JwtService;
import com.calendario.callapp.callapp_backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {

        // 🔍 Buscar usuario por correo
        Usuario usuario = usuarioRepository.getByCorreoOptimized(request.getCorreo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));


        // 🔐 Validar estado
        if (!"ACTIVO".equalsIgnoreCase(usuario.getEstado())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tu cuenta está inactiva. Contacta al administrador.");
        }

        // 🔐 Validar contraseña
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        }

        // 🎟️ Generar token JWT
        String token = jwtService.generarToken(usuario);
        
        Oficina oficina = usuario.getOficina();
        Long idOficina = oficina != null ? oficina.getId() : null;
        String oficinaNombre = oficina != null ? oficina.getNombre() : null;

        // 📦 Retornar respuesta con metadata inyectada
        return AuthResponse.builder()
                .token(token)
                .nombre(usuario.getNombre())
                .correo(usuario.getCorreo())
                .rol(usuario.getRol().name())
                .idOficina(idOficina)
                .oficinaNombre(oficinaNombre)
                .fotoUrl(usuario.getFotoUrl())
                .build();
    }
}