package com.calendario.callapp.callapp_backend.dto.request;

import com.calendario.callapp.callapp_backend.entity.TipoParticipante;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SolicitudEventoParticipanteRequest {

    @NotBlank
    private String nombre;

    private String cargo;

    private String descripcion;

    private String fotoUrl;

    private String telefono;

    private String correo;

    @NotNull
    private TipoParticipante tipo;
}
