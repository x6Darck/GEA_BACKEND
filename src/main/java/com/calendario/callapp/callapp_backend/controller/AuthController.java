package com.calendario.callapp.callapp_backend.controller;

import com.calendario.callapp.callapp_backend.dto.request.AuthRequest;
import com.calendario.callapp.callapp_backend.dto.request.MicrosoftAuthRequest;
import com.calendario.callapp.callapp_backend.dto.response.AuthResponse;
import com.calendario.callapp.callapp_backend.service.AuthService;
import com.calendario.callapp.callapp_backend.service.impl.MicrosoftAuthServiceImpl;
import com.calendario.callapp.callapp_backend.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MicrosoftAuthServiceImpl microsoftAuthService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@jakarta.validation.Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request), "Sesión iniciada correctamente"));
    }

    @PostMapping("/microsoft/mobile")
    public ResponseEntity<ApiResponse<AuthResponse>> microsoftMobile(@Valid @RequestBody MicrosoftAuthRequest request) {
        return ResponseEntity.ok(ApiResponse.success(microsoftAuthService.autenticar(request), "Sesión de Microsoft iniciada"));
    }
}
