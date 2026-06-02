package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.PublicacionEvento;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PublicacionEventoRepository extends JpaRepository<PublicacionEvento, Long> {

    Optional<PublicacionEvento> findBySolicitudEventoId(Long solicitudEventoId);

    List<PublicacionEvento> findBySolicitudEventoIdIn(List<Long> solicitudEventoIds);


    @Query("""
            SELECT p
            FROM PublicacionEvento p
            JOIN FETCH p.solicitudEvento s
            JOIN FETCH s.oficina
            JOIN FETCH s.tipoEventoCatalogo
            WHERE p.visible = true
              AND s.fechaEvento >= :hoy
            ORDER BY s.esImportante DESC, s.fechaEvento ASC, s.horaInicio ASC
            """)
    List<PublicacionEvento> findProximos(@Param("hoy") LocalDate hoy, Pageable pageable);

    @Query("""
            SELECT p
            FROM PublicacionEvento p
            JOIN FETCH p.solicitudEvento s
            JOIN FETCH s.oficina
            JOIN FETCH s.tipoEventoCatalogo
            WHERE p.visible = true
              AND s.fechaEvento >= :inicio
              AND s.fechaEvento < :fin
            ORDER BY s.esImportante DESC, s.fechaEvento ASC, s.horaInicio ASC
            """)
    List<PublicacionEvento> findPublicadasEnRango(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT p FROM PublicacionEvento p JOIN FETCH p.solicitudEvento s JOIN FETCH s.oficina JOIN FETCH s.tipoEventoCatalogo WHERE p.visible = true ORDER BY s.esImportante DESC, p.fechaPublicacion DESC")
    List<PublicacionEvento> getAllVisibleOptimized(Pageable pageable);

    @Query("SELECT p FROM PublicacionEvento p JOIN FETCH p.solicitudEvento s JOIN FETCH s.oficina JOIN FETCH s.tipoEventoCatalogo WHERE p.id = :id AND p.visible = true")
    Optional<PublicacionEvento> getByIdAndVisibleUnique(@Param("id") Long id);
}
