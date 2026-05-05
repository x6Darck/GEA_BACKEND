package com.calendario.callapp.callapp_backend.mapper;

import com.calendario.callapp.callapp_backend.dto.response.LugarFisicoResponse;
import com.calendario.callapp.callapp_backend.entity.LugarFisico;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface LugarFisicoMapper {
    LugarFisicoResponse toResponse(LugarFisico entity);
    List<LugarFisicoResponse> toResponseList(List<LugarFisico> entities);
}
