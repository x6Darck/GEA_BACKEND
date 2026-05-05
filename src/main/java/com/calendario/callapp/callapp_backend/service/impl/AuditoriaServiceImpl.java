package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.dto.response.AuditoriaRevisionResponse;
import com.calendario.callapp.callapp_backend.entity.SolicitudAnuncio;
import com.calendario.callapp.callapp_backend.entity.SolicitudEvento;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class AuditoriaServiceImpl {

    private final EntityManager entityManager;

    public List<AuditoriaRevisionResponse> historialUsuarios(Long id) {
        return historial(id, Usuario.class, usuario -> "Usuario " + usuario.getCorreo());
    }

    public List<AuditoriaRevisionResponse> historialSolicitudesEvento(Long id) {
        return historial(id, SolicitudEvento.class, evento -> "Solicitud evento " + evento.getNombreEvento() + " [" + evento.getEstado() + "]");
    }

    public List<AuditoriaRevisionResponse> historialSolicitudesAnuncio(Long id) {
        return historial(id, SolicitudAnuncio.class, anuncio -> "Solicitud anuncio " + anuncio.getTitulo() + " [" + anuncio.getEstado() + "]");
    }

    private <T> List<AuditoriaRevisionResponse> historial(Long id, Class<T> entityClass, Function<T, String> resumenFn) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        if (!reader.isEntityClassAudited(entityClass)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La entidad no tiene auditoria habilitada");
        }

        @SuppressWarnings("unchecked")
        List<Object[]> revisiones = reader.createQuery()
                .forRevisionsOfEntity(entityClass, false, true)
                .add(org.hibernate.envers.query.AuditEntity.id().eq(id))
                .getResultList();

        if (revisiones.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontraron revisiones para el id solicitado");
        }

        return revisiones.stream()
                .map(item -> {
                    T entidad = entityClass.cast(item[0]);
                    DefaultRevisionEntity revisionEntity = (DefaultRevisionEntity) item[1];
                    RevisionType revisionType = (RevisionType) item[2];
                    Number revisionNumber = revisionEntity.getId();
                    LocalDateTime fecha = Instant.ofEpochMilli(revisionEntity.getTimestamp())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();

                    return AuditoriaRevisionResponse.builder()
                            .revision(revisionNumber)
                            .fechaRevision(fecha)
                            .tipoCambio(revisionType.name())
                            .resumen(resumenFn.apply(entidad))
                            .build();
                })
                .sorted(Comparator.comparing(AuditoriaRevisionResponse::getFechaRevision).reversed())
                .toList();
    }
}
