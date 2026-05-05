package com.calendario.callapp.callapp_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.envers.Audited;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "solicitudes_evento")
@Getter
@Setter
@Audited
@lombok.EqualsAndHashCode(callSuper = false, exclude = "participantes")
public class SolicitudEvento extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long id;

    @Column(name = "nombre_evento", nullable = false, length = 160)
    private String nombreEvento;

    @Column(name = "descripcion_evento", length = 2000)
    private String descripcionEvento;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDate fechaEvento;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "solicitud_evento_lugares",
        joinColumns = @JoinColumn(name = "id_solicitud_evento"),
        inverseJoinColumns = @JoinColumn(name = "id_lugar_fisico")
    )
    private List<LugarFisico> lugaresFisicos = new java.util.ArrayList<>();

    @Column(name = "link_conexion", length = 500)
    private String linkConexion;

    @Column(name = "responsable_evento", length = 120)
    private String responsableEvento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_evento")
    private TipoEventoCatalogo tipoEventoCatalogo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado;

    @Column(length = 1000)
    private String motivoRechazo;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_oficina", nullable = false)
    private Oficina oficina;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_solicitante", nullable = false)
    private Usuario usuarioSolicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_revisor")
    private Usuario usuarioRevisor;

    @Column(name = "pieza_grafica_url", length = 500)
    private String piezaGraficaUrl;

    @Column(name = "requiere_transmision", nullable = false)
    private Boolean requiereTransmision = false;

    @Column(name = "requiere_cubrimiento", nullable = false)
    private Boolean requiereCubrimiento = false;

    @Column(name = "observaciones", length = 2000)
    private String observaciones;

    @Column(name = "es_importante", nullable = false)
    private Boolean esImportante = false;

    @Column(name = "requiere_pieza_grafica", nullable = false)
    private Boolean requierePiezaGrafica = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "frecuencia_recurrencia", length = 20)
    private FrecuenciaRecurrencia frecuenciaRecurrencia = FrecuenciaRecurrencia.NINGUNA;

    @Column(name = "fecha_fin_recurrencia")
    private LocalDate fechaFinRecurrencia;

    @Column(name = "id_grupo_recurrencia", length = 50)
    private String idGrupoRecurrencia;

    @Column(name = "es_principal", nullable = false)
    private Boolean esPrincipal = false;

    @OneToMany(mappedBy = "solicitudEvento", cascade = CascadeType.ALL, orphanRemoval = true)
    @lombok.Setter(lombok.AccessLevel.NONE)
    private List<SolicitudEventoParticipante> participantes = new ArrayList<>();
}
