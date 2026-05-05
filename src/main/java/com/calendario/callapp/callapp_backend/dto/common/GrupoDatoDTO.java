package com.calendario.callapp.callapp_backend.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GrupoDatoDTO {
    private String etiqueta;
    private Long valor;
    private String color;
    
    public GrupoDatoDTO(String etiqueta, Long valor) {
        this.etiqueta = etiqueta;
        this.valor = valor;
    }
}
