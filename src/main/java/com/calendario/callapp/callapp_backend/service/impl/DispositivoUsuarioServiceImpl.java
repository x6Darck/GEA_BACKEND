package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.dto.request.DispositivoTokenRequest;
import com.calendario.callapp.callapp_backend.entity.DispositivoUsuario;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.DispositivoUsuarioRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import com.calendario.callapp.callapp_backend.service.DispositivoUsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispositivoUsuarioServiceImpl implements DispositivoUsuarioService {

    private final DispositivoUsuarioRepository dispositivoUsuarioRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public void registrarToken(DispositivoTokenRequest request, Authentication authentication) {
        String token = request.getToken();
        Usuario usuario = usuarioRepository.getByCorreoOptimized(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        log.info("Registrando dispositivo para el usuario {}: {}", usuario.getCorreo(), token);

        Optional<DispositivoUsuario> tokenExistente = dispositivoUsuarioRepository.findByToken(token);

        if (tokenExistente.isPresent()) {
            DispositivoUsuario dispositivo = tokenExistente.get();
            dispositivo.setUsuario(usuario);
            dispositivo.setFechaRegistro(LocalDateTime.now());
            dispositivoUsuarioRepository.save(dispositivo);
            log.info("Token de dispositivo existente actualizado para el usuario: {}", usuario.getCorreo());
        } else {
            DispositivoUsuario nuevoDispositivo = new DispositivoUsuario();
            nuevoDispositivo.setToken(token);
            nuevoDispositivo.setUsuario(usuario);
            nuevoDispositivo.setFechaRegistro(LocalDateTime.now());
            dispositivoUsuarioRepository.save(nuevoDispositivo);
            log.info("Nuevo token de dispositivo registrado con éxito.");
        }
    }

    @Override
    @Transactional
    public void removerToken(String token, Authentication authentication) {
        Usuario usuario = usuarioRepository.getByCorreoOptimized(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        dispositivoUsuarioRepository.findByToken(token).ifPresent(dispositivo -> {
            if (!dispositivo.getUsuario().getId().equals(usuario.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes eliminar un dispositivo que no te pertenece");
            }
        });

        log.info("Removiendo dispositivo para usuario {}: {}", usuario.getCorreo(), token);
        dispositivoUsuarioRepository.deleteByToken(token);
    }
}
