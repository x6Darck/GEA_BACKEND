package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OficinaRequest {

    @NotBlank
    private String nombre;

    private String programaAcademico;

    private String descripcion;

    private Boolean activa;
}
