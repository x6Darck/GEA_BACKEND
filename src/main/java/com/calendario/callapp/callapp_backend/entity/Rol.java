package com.calendario.callapp.callapp_backend.entity;

public enum Rol {
    SUPER_ADMIN,
    COMUNICACIONES,
    OFICINA,
    USUARIO_APP,
    USUARIO_AUTENTICADO_APP,
    ADMIN,
    USUARIO;

    public static Rol fromNombre(String nombre) {
        if (nombre == null) return Rol.USUARIO;
        
        return switch (nombre) {
            case "SuperAdmin" -> SUPER_ADMIN;
            case "Comunicaciones" -> COMUNICACIONES;
            case "Oficina" -> OFICINA;
            case "Usuario Autenticado" -> USUARIO_AUTENTICADO_APP;
            case "Admin" -> ADMIN;
            case "Usuario" -> USUARIO;
            default -> {
                try {
                    yield Rol.valueOf(nombre.toUpperCase());
                } catch (Exception e) {
                    yield Rol.USUARIO;
                }
            }
        };
    }

    public String getSecurityRole() {
        return switch (this) {
            case ADMIN -> "SUPER_ADMIN";
            case USUARIO -> "USUARIO_APP";
            default -> this.name();
        };
    }

    public boolean esAdministradorGlobal() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    public boolean esComunicaciones() {
        return this == COMUNICACIONES;
    }

    public boolean esOficina() {
        return this == OFICINA || this == USUARIO_AUTENTICADO_APP || this == COMUNICACIONES;
    }
}
