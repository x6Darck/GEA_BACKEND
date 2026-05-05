package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.ArchivoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArchivoAdjuntoRepository extends JpaRepository<ArchivoAdjunto, Long> {

    Optional<ArchivoAdjunto> findByTokenAccesoAndPublicoTrue(String tokenAcceso);
    Optional<ArchivoAdjunto> findByNombreOriginal(String nombreOriginal);
}
