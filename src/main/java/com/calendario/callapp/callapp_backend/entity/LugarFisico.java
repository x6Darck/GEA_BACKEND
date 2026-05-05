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
@Table(name = "lugares_fisicos")
@Data
@Audited
@lombok.EqualsAndHashCode(callSuper = false)
public class LugarFisico extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lugar_fisico")
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column
    private Integer capacidad;

    @Column(nullable = false)
    private Boolean activo = true;
}
