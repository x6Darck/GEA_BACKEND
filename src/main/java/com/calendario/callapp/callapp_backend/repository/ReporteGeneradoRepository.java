package com.calendario.callapp.callapp_backend.repository;


import com.calendario.callapp.callapp_backend.entity.ReporteGenerado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReporteGeneradoRepository extends JpaRepository<ReporteGenerado, Long> {

    @Override
    @org.springframework.lang.NonNull
    @Query("SELECT r FROM ReporteGenerado r LEFT JOIN FETCH r.usuarioGenerador u LEFT JOIN FETCH u.oficina")
    List<ReporteGenerado> findAll();

    @Override
    @org.springframework.lang.NonNull
    @Query("SELECT r FROM ReporteGenerado r LEFT JOIN FETCH r.usuarioGenerador u LEFT JOIN FETCH u.oficina WHERE r.id = :id")
    java.util.Optional<ReporteGenerado> findById(@org.springframework.lang.NonNull @org.springframework.data.repository.query.Param("id") Long id);

    @Query("SELECT r FROM ReporteGenerado r LEFT JOIN FETCH r.usuarioGenerador u LEFT JOIN FETCH u.oficina WHERE u.id = :usuarioId ORDER BY r.fechaCreacion DESC")
    List<ReporteGenerado> findByUsuarioGeneradorIdOrderByFechaCreacionDesc(@org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);
    
    @Query("SELECT r FROM ReporteGenerado r LEFT JOIN FETCH r.usuarioGenerador u LEFT JOIN FETCH u.oficina WHERE u.oficina.id = :oficinaId ORDER BY r.fechaCreacion DESC")
    List<ReporteGenerado> findByUsuarioGeneradorOficinaIdOrderByFechaCreacionDesc(@org.springframework.data.repository.query.Param("oficinaId") Long oficinaId);

    @Query("SELECT r FROM ReporteGenerado r LEFT JOIN FETCH r.usuarioGenerador u LEFT JOIN FETCH u.oficina WHERE r.fechaCreacion BETWEEN :desde AND :hasta ORDER BY r.fechaCreacion DESC")
    List<ReporteGenerado> findByFechaCreacionBetween(
            @org.springframework.data.repository.query.Param("desde") java.time.LocalDateTime desde,
            @org.springframework.data.repository.query.Param("hasta") java.time.LocalDateTime hasta);
}
