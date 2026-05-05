package com.calendario.callapp.callapp_backend.dto.response;

import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import com.calendario.callapp.callapp_backend.entity.FrecuenciaRecurrencia;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class SolicitudEventoResponse {

    private Long id;
    private String nombreEvento;
    private String descripcionEvento;
    private LocalDate fechaEvento;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String lugar;
    private java.util.List<String> lugares;
    private java.util.List<Long> idsLugaresFisicos;
    private String linkConexion;
    private String responsableEvento;
    private Long tipoEventoId;
    private String tipoEvento;
    private String tipoEventoColorHex;
    private EstadoSolicitud estado;
    private String motivoRechazo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private String usuarioCreacion;
    private String usuarioActualizacion;
    private Long oficinaId;
    private String oficinaNombre;
    private Long usuarioSolicitanteId;
    private String usuarioSolicitanteCorreo;
    private String piezaGraficaUrl;
    private Boolean requiereTransmision;
    private Boolean requiereCubrimiento;
    private String observaciones;
    private Boolean esImportante;
    private Boolean requierePiezaGrafica;
    private FrecuenciaRecurrencia frecuenciaRecurrencia;
    private LocalDate fechaFinRecurrencia;
    private String idGrupoRecurrencia;
    private Boolean esPrincipal;
    private Boolean visible;
    private List<SolicitudEventoParticipanteResponse> participantes;
}
