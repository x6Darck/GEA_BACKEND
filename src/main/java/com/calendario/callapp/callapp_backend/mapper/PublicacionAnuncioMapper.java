package com.calendario.callapp.callapp_backend.mapper;


import com.calendario.callapp.callapp_backend.dto.response.PublicacionAnuncioResponse;
import com.calendario.callapp.callapp_backend.entity.PublicacionAnuncio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface PublicacionAnuncioMapper {

    @Mapping(target = "solicitudAnuncioId", source = "solicitudAnuncio.id")
    @Mapping(target = "categoria", source = "solicitudAnuncio.categoria")
    @Mapping(target = "lugar", expression = "java(entity.getSolicitudAnuncio().getLugaresFisicos().stream().map(com.calendario.callapp.callapp_backend.entity.LugarFisico::getNombre).collect(java.util.stream.Collectors.joining(\", \")))")
    @Mapping(target = "lugares", expression = "java(entity.getSolicitudAnuncio().getLugaresFisicos().stream().map(com.calendario.callapp.callapp_backend.entity.LugarFisico::getNombre).toList())")
    @Mapping(target = "idsLugaresFisicos", expression = "java(entity.getSolicitudAnuncio().getLugaresFisicos().stream().map(com.calendario.callapp.callapp_backend.entity.LugarFisico::getId).toList())")
    @Mapping(target = "correoContacto", source = "solicitudAnuncio.correoContacto")
    @Mapping(target = "responsableAnuncio", source = "solicitudAnuncio.responsableAnuncio")
    @Mapping(target = "fechaInicioPublicacion", source = "solicitudAnuncio.fechaInicioPublicacion")
    @Mapping(target = "fechaFinPublicacion", source = "solicitudAnuncio.fechaFinPublicacion")
    @Mapping(target = "horaInicio", source = "solicitudAnuncio.horaInicio")
    @Mapping(target = "horaFin", source = "solicitudAnuncio.horaFin")
    @Mapping(target = "oficinaNombre", expression = "java(entity.getSolicitudAnuncio().getOficina() != null ? entity.getSolicitudAnuncio().getOficina().getNombre() : (entity.getSolicitudAnuncio().getUsuarioSolicitante().getOficina() != null ? entity.getSolicitudAnuncio().getUsuarioSolicitante().getOficina().getNombre() : \"N/A\"))")
    @Mapping(target = "requierePiezaGrafica", source = "solicitudAnuncio.requierePiezaGrafica")
    PublicacionAnuncioResponse toResponse(PublicacionAnuncio entity);

    List<PublicacionAnuncioResponse> toResponseList(List<PublicacionAnuncio> entities);
}
