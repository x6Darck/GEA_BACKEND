package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SolicitudAnuncioRequest {

    @NotBlank
    private String titulo;

    private String descripcion;

    private String categoria;

    private java.util.List<Long> idsLugaresFisicos;

    private String correoContacto;

    private String responsableAnuncio;

    private LocalDate fechaInicioPublicacion;

    private LocalDate fechaFinPublicacion;

    private LocalTime horaInicio;

    private LocalTime horaFin;

    private String piezaGraficaUrl;

    private Boolean requierePiezaGrafica;
}
