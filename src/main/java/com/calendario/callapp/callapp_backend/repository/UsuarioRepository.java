package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("SELECT u FROM Usuario u JOIN FETCH u.rolEntity LEFT JOIN FETCH u.oficina WHERE u.correo = :correo")
    Optional<Usuario> getByCorreoOptimized(@Param("correo") String correo);

    boolean existsByCorreo(String correo);

    boolean existsByTelefono(String telefono);

    boolean existsByMicrosoftOid(String microsoftOid);

    long countByOficinaId(Long oficinaId);

    @Query("SELECT u FROM Usuario u " +
           "JOIN FETCH u.rolEntity " +
           "LEFT JOIN FETCH u.oficina " +
           "WHERE (:rolName IS NULL OR u.rolEntity.nombre = :rolName) AND " +
           "(:estado IS NULL OR u.estado = :estado) AND " +
           "(:q IS NULL OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Usuario> searchUsuarios(@Param("q") String q, @Param("rolName") String rolName, @Param("estado") String estado);
}
