package com.calendario.callapp.callapp_backend.service;

import com.calendario.callapp.callapp_backend.dto.request.DispositivoTokenRequest;
import org.springframework.security.core.Authentication;

public interface DispositivoUsuarioService {
    void registrarToken(DispositivoTokenRequest request, Authentication authentication);
    void removerToken(String token, Authentication authentication);
}
