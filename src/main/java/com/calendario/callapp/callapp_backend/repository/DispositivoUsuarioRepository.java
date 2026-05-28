package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.DispositivoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface DispositivoUsuarioRepository extends JpaRepository<DispositivoUsuario, Long> {
    Optional<DispositivoUsuario> findByToken(String token);
    List<DispositivoUsuario> findByUsuarioId(Long usuarioId);
    void deleteByToken(String token);
}
