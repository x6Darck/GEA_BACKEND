package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.getByCorreoOptimized(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getPassword())
                .disabled(!"ACTIVO".equalsIgnoreCase(usuario.getEstado()))
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getSecurityRole())))
                .build();
    }
}
