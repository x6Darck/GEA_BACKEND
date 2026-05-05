package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.LugarFisico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LugarFisicoRepository extends JpaRepository<LugarFisico, Long> {
    List<LugarFisico> findByActivoTrueOrderByNombreAsc();
    Optional<LugarFisico> findByNombreIgnoreCase(String nombre);
}
