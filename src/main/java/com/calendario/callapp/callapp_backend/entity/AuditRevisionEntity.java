package com.calendario.callapp.callapp_backend.entity;

import com.calendario.callapp.callapp_backend.util.AuditRevisionListener;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

/**
 * Entidad de revisión personalizada.
 * Se utiliza un nombre de tabla único 'audit_revision_info' para evitar 
 * conflictos con tablas antiguas ('revinfo') y garantizar portabilidad.
 */
@Entity
@Table(name = "audit_revision_info")
@AttributeOverrides({
    @AttributeOverride(name = "id", column = @Column(name = "rev")),
    @AttributeOverride(name = "timestamp", column = @Column(name = "revtstmp"))
})
@Data
@EqualsAndHashCode(callSuper = true)
@RevisionEntity(AuditRevisionListener.class)
public class AuditRevisionEntity extends DefaultRevisionEntity {

    @Column(name = "nombre_usuario")
    private String nombreUsuario;

    @Column(name = "ip_address")
    private String ipAddress;
}
