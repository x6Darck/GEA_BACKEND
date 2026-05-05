package com.calendario.callapp.callapp_backend.repository;

import java.util.List;
import java.util.Optional;
import com.calendario.callapp.callapp_backend.entity.Oficina;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OficinaRepository extends JpaRepository<Oficina, Long> {
    Optional<Oficina> findByNombreIgnoreCase(String nombre);
    List<Oficina> findByActivaTrueOrderByNombreAsc();
}
