package com.calendario.callapp.callapp_backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArchivoResponse {

    private Long id;
    private String nombreArchivo;
    private String nombreOriginal;
    private String tokenAcceso;
    private String url;
    private String contentType;
    private long tamano;
}
