package com.calendario.callapp.callapp_backend.service;

import com.calendario.callapp.callapp_backend.dto.response.LugarFisicoResponse;
import java.util.List;

public interface LugarFisicoService {
    List<LugarFisicoResponse> listarActivos();
    LugarFisicoResponse obtenerPorId(Long id);
}
