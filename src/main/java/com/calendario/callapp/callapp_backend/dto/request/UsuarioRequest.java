package com.calendario.callapp.callapp_backend.dto.request;

import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.Rol;
import lombok.Data;

@Data
public class UsuarioRequest {

    private String nombre;
    private String correo;
    private String telefono;
    private String password;
    private Long idRol;
    private Rol rol;
    private Long idOficina;
    private AuthProvider authProvider;
    private String microsoftOid;
    private String fotoUrl;
    private String estado;
}
