package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import com.calendario.callapp.callapp_backend.entity.SolicitudAnuncio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SolicitudAnuncioRepository extends JpaRepository<SolicitudAnuncio, Long> {

    @Query("SELECT s FROM SolicitudAnuncio s LEFT JOIN FETCH s.usuarioSolicitante u LEFT JOIN FETCH u.oficina WHERE s.id = :id")
    java.util.Optional<SolicitudAnuncio> getByIdOptimized(@Param("id") Long id);

    @Query("SELECT s FROM SolicitudAnuncio s LEFT JOIN FETCH s.usuarioSolicitante u LEFT JOIN FETCH u.oficina LEFT JOIN FETCH s.oficina " +
           "WHERE s.oficina.id = :oficinaId OR (s.oficina IS NULL AND u.oficina.id = :oficinaId) ORDER BY s.fechaCreacion DESC")
    List<SolicitudAnuncio> getAllByOficinaIdOptimized(@Param("oficinaId") Long oficinaId);

    @Query("SELECT s FROM SolicitudAnuncio s LEFT JOIN FETCH s.usuarioSolicitante u LEFT JOIN FETCH u.oficina ORDER BY s.fechaCreacion DESC")
    List<SolicitudAnuncio> getAllUniqueWithAssociations();

    long countByEstado(EstadoSolicitud estado);
    long countByOficinaId(Long oficinaId);
    long countByOficinaIdAndEstado(Long oficinaId, EstadoSolicitud estado);

    @Query("SELECT s.estado, COUNT(s) FROM SolicitudAnuncio s GROUP BY s.estado")
    List<Object[]> countByEstadoGrouped();

    @Query("SELECT s.estado, COUNT(s) FROM SolicitudAnuncio s WHERE s.oficina.id = :oficinaId GROUP BY s.estado")
    List<Object[]> countByEstadoGroupedByOficina(@Param("oficinaId") Long oficinaId);

    // Métodos para reportes
    @Query("SELECT s FROM SolicitudAnuncio s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.usuarioSolicitante u LEFT JOIN FETCH u.oficina LEFT JOIN FETCH s.usuarioRevisor " +
           "WHERE (s.oficina.id = :oficinaId OR (s.oficina IS NULL AND s.usuarioSolicitante.oficina.id = :oficinaId)) " +
           "AND s.fechaCreacion BETWEEN :desde AND :hasta")
    List<SolicitudAnuncio> findByUsuarioSolicitanteOficinaIdAndFechaCreacionBetween(
            @Param("oficinaId") Long oficinaId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT s FROM SolicitudAnuncio s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.usuarioSolicitante u LEFT JOIN FETCH u.oficina LEFT JOIN FETCH s.usuarioRevisor " +
           "WHERE s.fechaCreacion BETWEEN :desde AND :hasta")
    List<SolicitudAnuncio> findByFechaCreacionBetween(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT s FROM SolicitudAnuncio s LEFT JOIN FETCH s.oficina LEFT JOIN FETCH s.usuarioSolicitante u LEFT JOIN FETCH u.oficina LEFT JOIN FETCH s.usuarioRevisor " +
           "WHERE s.fechaCreacion BETWEEN :desde AND :hasta")
    List<SolicitudAnuncio> findByFechaCreacionBetweenOrderByFechaCreacionDesc(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    List<SolicitudAnuncio> findByOficinaIdAndFechaCreacionBetweenOrderByFechaCreacionDesc(
            @Param("oficinaId") Long oficinaId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(s) FROM SolicitudAnuncio s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta")
    long countFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COALESCE(s.oficina.nombre, 'General'), COUNT(s) FROM SolicitudAnuncio s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta GROUP BY s.oficina.nombre")
    List<Object[]> countAnunciosByOficinaFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT s.estado, COUNT(s) FROM SolicitudAnuncio s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta GROUP BY s.estado")
    List<Object[]> countAnunciosByEstadoFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT MONTH(s.fechaCreacion), COUNT(s) FROM SolicitudAnuncio s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta GROUP BY MONTH(s.fechaCreacion)")
    List<Object[]> countAnunciosByMesFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(s) FROM SolicitudAnuncio s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.estado IN (:estados) AND s.fechaCreacion BETWEEN :desde AND :hasta")
    long countByEstadosFiltered(@Param("oficinaId") Long oficinaId, @Param("estados") List<EstadoSolicitud> estados, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COALESCE(s.categoria, 'General'), COUNT(s), '#64748b' FROM SolicitudAnuncio s WHERE (:oficinaId IS NULL OR s.oficina.id = :oficinaId) AND s.fechaCreacion BETWEEN :desde AND :hasta GROUP BY s.categoria")
    List<Object[]> countAnunciosByCategoriaFiltered(@Param("oficinaId") Long oficinaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
