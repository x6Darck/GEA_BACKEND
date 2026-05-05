package com.calendario.callapp.callapp_backend.util;

import com.calendario.callapp.callapp_backend.entity.AuditRevisionEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Listener que se ejecuta cada vez que Hibernate Envers crea una nueva revisión.
 * Se encarga de llenar los campos personalizados (usuario e IP).
 */
public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        AuditRevisionEntity auditEntity = (AuditRevisionEntity) revisionEntity;
        
        // 1. Obtener el usuario del contexto de seguridad
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            auditEntity.setNombreUsuario(auth.getName());
        } else {
            auditEntity.setNombreUsuario("SISTEMA/PUBLICO");
        }

        // 2. Obtener la IP del cliente
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null || ipAddress.isEmpty()) {
                ipAddress = request.getRemoteAddr();
            }
            auditEntity.setIpAddress(ipAddress);
        }
    }
}
