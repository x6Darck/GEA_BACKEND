package com.calendario.callapp.callapp_backend.controller;

import com.calendario.callapp.callapp_backend.dto.request.TipoEventoRequest;
import com.calendario.callapp.callapp_backend.dto.response.TipoEventoResponse;
import com.calendario.callapp.callapp_backend.service.impl.TipoEventoServiceImpl;
import com.calendario.callapp.callapp_backend.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuario/tipos-evento")
@RequiredArgsConstructor
public class TipoEventoController {

    private final TipoEventoServiceImpl tipoEventoService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TipoEventoResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.success(tipoEventoService.listarActivos()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMUNICACIONES')")
    public ResponseEntity<ApiResponse<TipoEventoResponse>> crear(@Valid @RequestBody TipoEventoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tipoEventoService.crear(request), "Tipo de evento creado exitosamente"));
    }
}
