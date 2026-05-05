package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import com.calendario.callapp.callapp_backend.entity.FrecuenciaRecurrencia;

@Data
public class SolicitudEventoRequest {

    @NotBlank
    private String nombreEvento;

    private String descripcionEvento;

    @NotNull
    @FutureOrPresent
    private LocalDate fechaEvento;

    @NotNull
    private LocalTime horaInicio;

    @NotNull
    private LocalTime horaFin;

    private java.util.List<Long> idsLugaresFisicos;

    private String linkConexion;

    private String responsableEvento;

    @NotBlank
    private String tipoEvento;

    private Boolean requierePiezaGrafica;

    private FrecuenciaRecurrencia frecuenciaRecurrencia;

    private LocalDate fechaFinRecurrencia;

    private Boolean requiereTransmision;

    private Boolean requiereCubrimiento;

    private String observaciones;

    private Boolean esImportante;

    private Long idOficina;

    @Valid
    private List<SolicitudEventoParticipanteRequest> participantes;
}
