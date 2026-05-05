package com.calendario.callapp.callapp_backend.dto.response;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class AuthResponse {

    private String token;

    private String nombre;

    private String correo;

    private String rol;
    
    private Long idOficina;
    
    private String oficinaNombre;
    
    private String fotoUrl;

    @Builder.Default
    private String tipo = "Bearer";
}