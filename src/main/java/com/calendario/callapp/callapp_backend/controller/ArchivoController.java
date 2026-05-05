package com.calendario.callapp.callapp_backend.controller;

import com.calendario.callapp.callapp_backend.dto.response.ArchivoResponse;
import com.calendario.callapp.callapp_backend.service.impl.ArchivoServiceImpl;
import com.calendario.callapp.callapp_backend.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class ArchivoController {

    private final ArchivoServiceImpl archivoService;

    @PostMapping("/comunicaciones/archivos/upload")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN', 'OFICINA', 'USUARIO_AUTENTICADO_APP')")
    public ResponseEntity<ApiResponse<ArchivoResponse>> subir(@RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(ApiResponse.success(archivoService.guardar(archivo), "Archivo subido correctamente"));
    }

    @GetMapping("/archivos/public/{tokenAcceso}")
    public ResponseEntity<Resource> descargar(@PathVariable String tokenAcceso) {
        Resource recurso = archivoService.cargarPublicoComoRecurso(tokenAcceso);
        String contentType = "application/octet-stream";
        try {
            // Intentar detectar el tipo de contenido
            contentType = java.nio.file.Files.probeContentType(java.nio.file.Paths.get(recurso.getURI()));
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
        } catch (IOException e) {
            // fallback
        }

        return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .body(recurso);
    }
}
