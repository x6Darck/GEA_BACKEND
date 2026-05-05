package com.calendario.callapp.callapp_backend.dto.request;

import lombok.Data;

@Data
public class AuthRequest {

    @jakarta.validation.constraints.NotBlank(message = "El correo es obligatorio")
    @jakarta.validation.constraints.Email(message = "Formato de correo inválido")
    private String correo;

    @jakarta.validation.constraints.NotBlank(message = "La contraseña es obligatoria")
    private String password;
}