package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.PublicacionAnuncio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PublicacionAnuncioRepository extends JpaRepository<PublicacionAnuncio, Long> {

    Optional<PublicacionAnuncio> findBySolicitudAnuncioId(Long solicitudAnuncioId);

    List<PublicacionAnuncio> findBySolicitudAnuncioIdIn(List<Long> solicitudAnuncioIds);


    List<PublicacionAnuncio> findByVisibleTrueOrderByFechaPublicacionDesc();

    Optional<PublicacionAnuncio> findByIdAndVisibleTrue(Long id);
}
