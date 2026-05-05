package com.calendario.callapp.callapp_backend.service.impl;

import org.springframework.transaction.annotation.Transactional;

import com.calendario.callapp.callapp_backend.dto.request.TipoEventoRequest;
import com.calendario.callapp.callapp_backend.dto.response.TipoEventoResponse;
import com.calendario.callapp.callapp_backend.entity.TipoEventoCatalogo;
import com.calendario.callapp.callapp_backend.repository.TipoEventoCatalogoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoEventoServiceImpl {

    private final TipoEventoCatalogoRepository tipoEventoRepository;

    @Transactional(readOnly = true)
    public List<TipoEventoResponse> listarActivos() {
        return tipoEventoRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TipoEventoResponse crear(TipoEventoRequest request) {
        TipoEventoCatalogo tipoEvento = new TipoEventoCatalogo();
        tipoEvento.setNombre(request.getNombre());
        tipoEvento.setDescripcion(request.getDescripcion());
        tipoEvento.setColorHex(request.getColorHex());
        tipoEvento.setActivo(request.getActivo() == null || request.getActivo());
        return toResponse(tipoEventoRepository.save(tipoEvento));
    }

    private TipoEventoResponse toResponse(TipoEventoCatalogo tipoEvento) {
        return TipoEventoResponse.builder()
                .id(tipoEvento.getId())
                .nombre(tipoEvento.getNombre())
                .descripcion(tipoEvento.getDescripcion())
                .colorHex(tipoEvento.getColorHex())
                .activo(tipoEvento.getActivo())
                .build();
    }
}
