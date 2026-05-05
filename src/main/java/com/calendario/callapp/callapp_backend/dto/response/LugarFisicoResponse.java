package com.calendario.callapp.callapp_backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LugarFisicoResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private Integer capacidad;
    private Boolean activo;
}
