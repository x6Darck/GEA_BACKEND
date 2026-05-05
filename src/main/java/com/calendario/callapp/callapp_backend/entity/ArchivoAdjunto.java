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
@Table(name = "archivos_adjuntos")
@Data
public class ArchivoAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_archivo_adjunto")
    private Long id;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "nombre_almacenado", nullable = false, unique = true, length = 255)
    private String nombreAlmacenado;

    @Column(name = "token_acceso", nullable = false, unique = true, length = 120)
    private String tokenAcceso;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(nullable = false)
    private Long tamano;

    @Column(nullable = false)
    private Boolean publico;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
