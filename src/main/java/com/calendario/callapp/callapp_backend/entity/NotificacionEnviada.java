package com.calendario.callapp.callapp_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones_enviadas")
@Data
public class NotificacionEnviada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion_enviada")
    private Long id;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(length = 1000)
    private String destinatarios;

    @Column(nullable = false, length = 200)
    private String asunto;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @Column(nullable = false)
    private Boolean exito;

    @Column(name = "detalle_error", length = 1000)
    private String detalleError;
}
