package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TipoEventoRequest {

    @NotBlank
    private String nombre;

    private String descripcion;

    @NotBlank
    private String colorHex;

    private Boolean activo;
}
