package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.SolicitudEventoParticipante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudEventoParticipanteRepository extends JpaRepository<SolicitudEventoParticipante, Long> {

    List<SolicitudEventoParticipante> findBySolicitudEventoId(Long solicitudEventoId);

    void deleteBySolicitudEventoId(Long solicitudEventoId);
}
