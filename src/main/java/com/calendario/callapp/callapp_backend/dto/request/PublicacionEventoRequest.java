package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PublicacionEventoRequest {

    @NotBlank
    private String tituloVisible;

    private String descripcionVisible;

    private String piezaGraficaUrl;

    private LocalDateTime fechaPublicacion;
}
