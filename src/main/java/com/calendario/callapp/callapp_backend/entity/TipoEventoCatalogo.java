package com.calendario.callapp.callapp_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "tipos_evento")
@Data
@Audited
public class TipoEventoCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_evento")
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    @Column(name = "color_hex", nullable = false, length = 7)
    private String colorHex;

    @Column(nullable = false)
    private Boolean activo;
}
