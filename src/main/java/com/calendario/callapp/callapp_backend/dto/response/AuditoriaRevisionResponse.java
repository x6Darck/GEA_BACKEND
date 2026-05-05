package com.calendario.callapp.callapp_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditoriaRevisionResponse {

    private Number revision;
    private LocalDateTime fechaRevision;
    private String tipoCambio;
    private String resumen;
}
