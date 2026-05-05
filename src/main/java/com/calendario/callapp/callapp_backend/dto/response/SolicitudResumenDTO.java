package com.calendario.callapp.callapp_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudResumenDTO {
    private Long id;
    private String tipo; // EVENTO o ANUNCIO
    private String titulo;
    private String oficina;
    private String fechaRegistro;
    private String estado;
}
