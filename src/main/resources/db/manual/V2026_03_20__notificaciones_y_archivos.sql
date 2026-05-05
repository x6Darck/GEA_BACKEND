-- CallApp - Migracion manual para notificaciones y archivos
-- Objetivo:
-- 1. Dejar una tabla simple de notificaciones enviadas
-- 2. Preparar trazabilidad basica para futuras auditorias

USE eventos_institucionales;

CREATE TABLE IF NOT EXISTS notificaciones_enviadas (
    id_notificacion_enviada BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo VARCHAR(30) NOT NULL,
    destinatarios VARCHAR(1000) NULL,
    asunto VARCHAR(200) NOT NULL,
    fecha_envio DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exito BIT NOT NULL DEFAULT b'1',
    detalle_error VARCHAR(1000) NULL
);

-- Resultado esperado:
-- 1. Queda lista la trazabilidad minima de notificaciones.
