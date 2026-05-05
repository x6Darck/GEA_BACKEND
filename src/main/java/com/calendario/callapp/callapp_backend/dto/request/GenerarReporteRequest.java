package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GenerarReporteRequest {

    @NotBlank
    private String nombre;

    private String descripcion;

    @NotBlank
    private String formato;

    @NotBlank
    private String alcance;

    @NotNull
    private LocalDate desde;

    @NotNull
    private LocalDate hasta;

    private Long idOficina;

    private Long idTipoEvento;
}
