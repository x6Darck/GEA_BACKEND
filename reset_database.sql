-- ============================================================
--  GEA SYSTEM — SCRIPT DE RESET OPERACIONAL (PRO)
--  Versión: 2.0 | Fecha: 2026-05-21
--
--  Qué hace:
--    ✅ Conserva: usuarios, catálogos (roles, oficinas,
--                lugares físicos, tipos de evento)
--    ❌ Borra:    solicitudes, publicaciones, notificaciones,
--                archivos y auditoría (Envers completo)
--
--  Por qué se actualizó:
--    MySQL no permite TRUNCATE en tablas referenciadas por llaves
--    foráneas (incluso con FOREIGN_KEY_CHECKS = 0). Se cambió a
--    DELETE FROM + ALTER TABLE AUTO_INCREMENT para garantizar éxito.
-- ============================================================

USE callapp_db;  -- ← Cambia esto si tu BD tiene otro nombre

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Tablas de relación ManyToMany e Hijas
DELETE FROM solicitud_evento_lugares;
DELETE FROM solicitud_anuncio_lugares;
DELETE FROM solicitud_evento_participantes;
DELETE FROM publicaciones_evento;
DELETE FROM publicaciones_anuncio;

-- 2. Auditoría Envers (Tablas Hijas con FK a audit_revision_info o revinfo)
-- Se limpian todas para evitar inconsistencias de revisión
DELETE FROM solicitud_evento_lugares_aud;
DELETE FROM solicitud_anuncio_lugares_aud;
DELETE FROM solicitud_evento_participantes_aud;
DELETE FROM publicaciones_evento_aud;
DELETE FROM publicaciones_anuncio_aud;
DELETE FROM solicitudes_evento_aud;
DELETE FROM solicitudes_anuncio_aud;
DELETE FROM oficinas_aud;
DELETE FROM roles_aud;
DELETE FROM lugares_fisicos_aud;
DELETE FROM usuarios_aud;
DELETE FROM tipos_evento_aud;

-- 3. Tablas de Revisión Envers (Padres)
DELETE FROM audit_revision_info;
DELETE FROM revinfo;

-- 4. Datos Transaccionales Principales
DELETE FROM solicitudes_evento;
DELETE FROM solicitudes_anuncio;

-- 5. Notificaciones y Reportes
DELETE FROM notificaciones_enviadas;
DELETE FROM reportes_generados;

-- 6. Archivos adjuntos (conserva los assets del sistema)
SET SQL_SAFE_UPDATES = 0;
DELETE FROM archivos_adjuntos
WHERE nombre_original NOT IN ('avatar-sistema.png', 'evento-prueba.png');
SET SQL_SAFE_UPDATES = 1;

-- 7. Resetear contadores AUTO_INCREMENT (Equivalente al efecto de TRUNCATE)
ALTER TABLE solicitudes_evento AUTO_INCREMENT = 1;
ALTER TABLE solicitudes_anuncio AUTO_INCREMENT = 1;
ALTER TABLE publicaciones_evento AUTO_INCREMENT = 1;
ALTER TABLE publicaciones_anuncio AUTO_INCREMENT = 1;
ALTER TABLE notificaciones_enviadas AUTO_INCREMENT = 1;
ALTER TABLE reportes_generados AUTO_INCREMENT = 1;
ALTER TABLE archivos_adjuntos AUTO_INCREMENT = 1;
ALTER TABLE audit_revision_info AUTO_INCREMENT = 1;
ALTER TABLE revinfo AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'Reset completado. Usuarios y catálogos conservados. Solicitudes, publicaciones y auditorías borradas.' AS resultado;
