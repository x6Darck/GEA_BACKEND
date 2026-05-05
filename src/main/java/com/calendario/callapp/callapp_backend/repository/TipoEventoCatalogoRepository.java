package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.TipoEventoCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoEventoCatalogoRepository extends JpaRepository<TipoEventoCatalogo, Long> {

    List<TipoEventoCatalogo> findByActivoTrueOrderByNombreAsc();

    Optional<TipoEventoCatalogo> findByNombreIgnoreCaseAndActivoTrue(String nombre);
    Optional<TipoEventoCatalogo> findByNombreIgnoreCase(String nombre);
}
