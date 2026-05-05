package com.calendario.callapp.callapp_backend.dto.response;

import com.calendario.callapp.callapp_backend.entity.TipoParticipante;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SolicitudEventoParticipanteResponse {

    private Long id;
    private String nombre;
    private String cargo;
    private String descripcion;
    private String fotoUrl;
    private String telefono;
    private String correo;
    private TipoParticipante tipo;
}
