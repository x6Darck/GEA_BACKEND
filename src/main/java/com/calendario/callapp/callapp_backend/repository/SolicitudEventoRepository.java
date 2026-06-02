package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import com.calendario.callapp.callapp_backend.entity.SolicitudEvento;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SolicitudEventoRepository extends JpaRepository<SolicitudEvento, Long> {

    @Query("SELECT s FROM SolicitudEvento s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.tipoEventoCatalogo LEFT JOIN FETCH s.usuarioSolicitante u WHERE s.id = :id")
    java.util.Optional<SolicitudEvento> getByIdOptimized(@Param("id") Long id);

    @Query("SELECT s FROM SolicitudEvento s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.tipoEventoCatalogo LEFT JOIN FETCH s.usuarioSolicitante u WHERE s.oficina.id = :oficinaId ORDER BY s.fechaCreacion DESC")
    List<SolicitudEvento> getAllByOficinaIdOptimized(@Param("oficinaId") Long oficinaId);

    @Query("SELECT s FROM SolicitudEvento s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.tipoEventoCatalogo LEFT JOIN FETCH s.usuarioSolicitante u ORDER BY s.fechaCreacion DESC")
    List<SolicitudEvento> getAllUniqueWithAssociations(Pageable pageable);

    long countByEstado(EstadoSolicitud estado);

    long countByOficinaIdAndEstado(Long oficinaId, EstadoSolicitud estado);

    long countByOficinaId(Long oficinaId);

    @Query("SELECT s.estado, COUNT(s) FROM SolicitudEvento s GROUP BY s.estado")
    List<Object[]> countByEstadoGrouped();

    @Query("SELECT s.estado, COUNT(s) FROM SolicitudEvento s WHERE s.oficina.id = :oficinaId GROUP BY s.estado")
    List<Object[]> countByEstadoGroupedByOficina(@Param("oficinaId") Long oficinaId);

    // Métodos para reportes
    @Query("SELECT s FROM SolicitudEvento s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.tipoEventoCatalogo LEFT JOIN FETCH s.usuarioRevisor " +
           "WHERE s.oficina.id = :oficinaId AND s.fechaCreacion BETWEEN :desde AND :hasta AND s.tipoEventoCatalogo.id = :tipoEventoId")
    List<SolicitudEvento> findByOficinaIdAndFechaCreacionBetweenAndTipoEventoCatalogoId(
            @Param("oficinaId") Long oficinaId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("tipoEventoId") Long tipoEventoId);

    @Query("SELECT s FROM SolicitudEvento s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.tipoEventoCatalogo LEFT JOIN FETCH s.usuarioRevisor " +
           "WHERE s.oficina.id = :oficinaId AND s.fechaCreacion BETWEEN :desde AND :hasta")
    List<SolicitudEvento> findByOficinaIdAndFechaCreacionBetween(
            @Param("oficinaId") Long oficinaId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT s FROM SolicitudEvento s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.tipoEventoCatalogo LEFT JOIN FETCH s.usuarioRevisor " +
           "WHERE s.fechaCreacion BETWEEN :desde AND :hasta AND s.tipoEventoCatalogo.id = :tipoEventoId")
    List<SolicitudEvento> findByFechaCreacionBetweenAndTipoEventoCatalogoId(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("tipoEventoId") Long tipoEventoId);

    @Query("SELECT s FROM SolicitudEvento s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.tipoEventoCatalogo LEFT JOIN FETCH s.usuarioRevisor " +
           "WHERE s.fechaCreacion BETWEEN :desde AND :hasta")
    List<SolicitudEvento> findByFechaCreacionBetween(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT s FROM SolicitudEvento s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.tipoEventoCatalogo LEFT JOIN FETCH s.usuarioRevisor " +
           "WHERE s.fechaCreacion BETWEEN :desde AND :hasta")
    List<SolicitudEvento> findByFechaCreacionBetweenOrderByFechaCreacionDesc(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    List<SolicitudEvento> findByOficinaIdAndFechaCreacionBetweenOrderByFechaCreacionDesc(
            @Param("oficinaId") Long oficinaId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
    @Query("SELECT COALESCE(s.tipoEventoCatalogo.nombre, 'Sin Tipo'), COUNT(s), COALESCE(s.tipoEventoCatalogo.colorHex, '#64748b') FROM SolicitudEvento s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta GROUP BY s.tipoEventoCatalogo.nombre, s.tipoEventoCatalogo.colorHex")
    List<Object[]> countEventosByTipoFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COALESCE(s.oficina.nombre, 'General'), COUNT(s) FROM SolicitudEvento s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta GROUP BY s.oficina.nombre")
    List<Object[]> countEventosByOficinaFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT s.estado, COUNT(s) FROM SolicitudEvento s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta GROUP BY s.estado")
    List<Object[]> countEventosByEstadoFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT MONTH(s.fechaCreacion), COUNT(s) FROM SolicitudEvento s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta GROUP BY MONTH(s.fechaCreacion)")
    List<Object[]> countEventosByMesFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(s) FROM SolicitudEvento s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta")
    long countFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(s) FROM SolicitudEvento s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.estado IN (:estados) AND s.fechaCreacion BETWEEN :desde AND :hasta")
    long countByEstadosFiltered(@Param("oficinaId") Long oficinaId, @Param("estados") List<EstadoSolicitud> estados, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT s FROM SolicitudEvento s JOIN s.lugaresFisicos l WHERE s.fechaEvento IN :fechas " +
           "AND l.id IN :lugarIds " +
           "AND s.estado IN (com.calendario.callapp.callapp_backend.entity.EstadoSolicitud.APROBADA, com.calendario.callapp.callapp_backend.entity.EstadoSolicitud.PUBLICADA) " +
           "AND ((s.horaInicio < :horaFin AND s.horaFin > :horaInicio)) " +
           "AND (:id IS NULL OR s.id <> :id)")
    List<SolicitudEvento> findConflictsBulk(@Param("fechas") List<java.time.LocalDate> fechas, 
                                            @Param("lugarIds") List<Long> lugarIds, 
                                            @Param("horaInicio") java.time.LocalTime horaInicio, 
                                            @Param("horaFin") java.time.LocalTime horaFin,
                                            @Param("id") Long id);

    List<SolicitudEvento> findAllByIdGrupoRecurrencia(String idGrupoRecurrencia);
}
