package com.calendario.callapp.callapp_backend.controller;

import com.calendario.callapp.callapp_backend.dto.request.DispositivoTokenRequest;
import com.calendario.callapp.callapp_backend.service.impl.DispositivoUsuarioServiceImpl;
import com.calendario.callapp.callapp_backend.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuario/dispositivos")
@RequiredArgsConstructor
public class DispositivoUsuarioController {

    private final DispositivoUsuarioServiceImpl dispositivoUsuarioService;

    @PostMapping("/registrar")
    public ResponseEntity<ApiResponse<Void>> registrarDispositivo(
            @Valid @RequestBody DispositivoTokenRequest request,
            Authentication authentication) {
        dispositivoUsuarioService.registrarToken(request, authentication);
        return ResponseEntity.ok(ApiResponse.success(null, "Dispositivo registrado exitosamente"));
    }

    @PostMapping("/desregistrar")
    public ResponseEntity<ApiResponse<Void>> removerDispositivo(
            @Valid @RequestBody DispositivoTokenRequest request,
            Authentication authentication) {
        dispositivoUsuarioService.removerToken(request.getToken(), authentication);
        return ResponseEntity.ok(ApiResponse.success(null, "Dispositivo removido exitosamente"));
    }
}
