package com.calendario.callapp.callapp_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardResumenResponse {

    private String alcance;
    private long totalSolicitudesEvento;
    private long eventosPendientes;
    private long eventosAprobados;
    private long eventosPublicados;
    private long totalSolicitudesAnuncio;
    private long anunciosPendientes;
    private long anunciosAprobados;
    private long anunciosPublicados;
    private long totalUsuarios;
    private long totalOficinas;
    private List<PublicacionEventoResponse> proximosEventos;
}
