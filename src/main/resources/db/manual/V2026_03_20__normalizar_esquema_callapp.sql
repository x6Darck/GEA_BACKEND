-- CallApp - Migracion manual de normalizacion
-- Compatible con MySQL 8 sin usar ADD COLUMN IF NOT EXISTS ni CREATE INDEX IF NOT EXISTS
-- Ejecutar sobre la BD eventos_institucionales

USE eventos_institucionales;

SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_column_if_missing $$
CREATE PROCEDURE add_column_if_missing(
    IN p_table_name VARCHAR(128),
    IN p_column_name VARCHAR(128),
    IN p_column_definition TEXT
)
BEGIN
    DECLARE v_count INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND column_name = p_column_name;

    IF v_count = 0 THEN
        SET @sql = CONCAT(
            'ALTER TABLE ', p_table_name,
            ' ADD COLUMN ', p_column_name, ' ', p_column_definition
        );
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DROP PROCEDURE IF EXISTS add_index_if_missing $$
CREATE PROCEDURE add_index_if_missing(
    IN p_table_name VARCHAR(128),
    IN p_index_name VARCHAR(128),
    IN p_index_definition TEXT
)
BEGIN
    DECLARE v_count INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_count
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND index_name = p_index_name;

    IF v_count = 0 THEN
        SET @sql = CONCAT(
            'CREATE INDEX ', p_index_name,
            ' ON ', p_table_name, ' ', p_index_definition
        );
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DROP PROCEDURE IF EXISTS add_fk_if_missing $$
CREATE PROCEDURE add_fk_if_missing(
    IN p_table_name VARCHAR(128),
    IN p_fk_name VARCHAR(128),
    IN p_fk_sql TEXT
)
BEGIN
    DECLARE v_count INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_count
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND constraint_name = p_fk_name
      AND constraint_type = 'FOREIGN KEY';

    IF v_count = 0 THEN
        SET @sql = p_fk_sql;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DROP PROCEDURE IF EXISTS modify_column_if_needed $$
CREATE PROCEDURE modify_column_if_needed(
    IN p_table_name VARCHAR(128),
    IN p_column_name VARCHAR(128),
    IN p_target_definition TEXT
)
BEGIN
    DECLARE v_count INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND column_name = p_column_name;

    IF v_count > 0 THEN
        SET @sql = CONCAT(
            'ALTER TABLE ', p_table_name,
            ' MODIFY COLUMN ', p_column_name, ' ', p_target_definition
        );
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

CREATE TABLE IF NOT EXISTS tipos_evento (
    id_tipo_evento BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE,
    descripcion VARCHAR(300) NULL,
    color_hex VARCHAR(7) NOT NULL,
    activo BIT NOT NULL DEFAULT b'1'
);

INSERT INTO tipos_evento (nombre, descripcion, color_hex, activo)
VALUES
    ('ACADEMICO', 'Eventos de caracter academico', '#2563EB', b'1'),
    ('CULTURAL', 'Eventos culturales y artisticos', '#7C3AED', b'1'),
    ('DEPORTIVO', 'Eventos deportivos y recreativos', '#16A34A', b'1'),
    ('INSTITUCIONAL', 'Eventos institucionales y corporativos', '#DC2626', b'1'),
    ('INVESTIGACION', 'Eventos de investigacion y ciencia', '#0F766E', b'1'),
    ('BIENESTAR', 'Eventos de bienestar universitario', '#EA580C', b'1')
ON DUPLICATE KEY UPDATE
    descripcion = VALUES(descripcion),
    color_hex = VALUES(color_hex),
    activo = VALUES(activo);

CALL add_column_if_missing('oficinas', 'descripcion', 'VARCHAR(500) NULL');
CALL add_column_if_missing('oficinas', 'activa', 'BIT NOT NULL DEFAULT b''1''');
CALL add_column_if_missing('oficinas', 'fecha_creacion', 'TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP');
CALL add_column_if_missing('oficinas', 'programa_academico', 'VARCHAR(120) NULL');
CALL add_column_if_missing('oficinas', 'nombre', 'VARCHAR(120) NULL');

UPDATE oficinas
SET nombre = COALESCE(NULLIF(nombre, ''), nombre_oficina)
WHERE nombre IS NULL OR nombre = '';

UPDATE oficinas
SET descripcion = COALESCE(
    descripcion,
    CONCAT('La oficina ', COALESCE(nombre, nombre_oficina), ' realiza funciones institucionales.')
)
WHERE descripcion IS NULL;

CALL add_column_if_missing('usuarios', 'rol', 'VARCHAR(50) NULL');
CALL add_column_if_missing('usuarios', 'auth_provider', 'ENUM(''LOCAL'',''MICROSOFT'') NOT NULL DEFAULT ''LOCAL''');
CALL add_column_if_missing('usuarios', 'microsoft_oid', 'VARCHAR(255) NULL');

UPDATE usuarios
SET rol = CASE id_rol
    WHEN 1 THEN 'SUPER_ADMIN'
    WHEN 2 THEN 'COMUNICACIONES'
    WHEN 3 THEN 'OFICINA'
    WHEN 4 THEN 'USUARIO_APP'
    WHEN 5 THEN 'USUARIO_AUTENTICADO_APP'
    ELSE COALESCE(rol, 'USUARIO_APP')
END
WHERE rol IS NULL OR rol = '' OR rol IN ('ADMIN', 'USUARIO');

CALL add_column_if_missing('solicitudes_evento', 'descripcion_evento', 'VARCHAR(2000) NULL');
CALL add_column_if_missing('solicitudes_evento', 'estado', 'ENUM(''PENDIENTE'',''APROBADA'',''RECHAZADA'',''PUBLICADA'') NOT NULL DEFAULT ''PENDIENTE''');
CALL add_column_if_missing('solicitudes_evento', 'motivo_rechazo', 'VARCHAR(1000) NULL');
CALL add_column_if_missing('solicitudes_evento', 'tipo_evento', 'VARCHAR(120) NULL');
CALL add_column_if_missing('solicitudes_evento', 'fecha_creacion', 'TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP');
CALL add_column_if_missing('solicitudes_evento', 'id_usuario_solicitante', 'INT NULL');
CALL add_column_if_missing('solicitudes_evento', 'id_usuario_revisor', 'INT NULL');
CALL add_column_if_missing('solicitudes_evento', 'id_tipo_evento', 'BIGINT NULL');

CALL modify_column_if_needed('solicitudes_evento', 'id_usuario_solicitante', 'INT NULL');
CALL modify_column_if_needed('solicitudes_evento', 'id_usuario_revisor', 'INT NULL');
CALL modify_column_if_needed('solicitud_evento_participantes', 'id_solicitud_evento', 'INT NOT NULL');
CALL modify_column_if_needed('solicitudes_anuncio', 'id_usuario_solicitante', 'INT NOT NULL');
CALL modify_column_if_needed('solicitudes_anuncio', 'id_usuario_revisor', 'INT NULL');
CALL modify_column_if_needed('publicaciones_evento', 'id_solicitud_evento', 'INT NOT NULL');
CALL modify_column_if_needed('publicaciones_evento', 'id_usuario_publicador', 'INT NOT NULL');
CALL modify_column_if_needed('publicaciones_anuncio', 'id_usuario_publicador', 'INT NOT NULL');

UPDATE solicitudes_evento
SET descripcion_evento = COALESCE(descripcion_evento, descripcion)
WHERE descripcion_evento IS NULL;

UPDATE solicitudes_evento
SET estado = CASE
    WHEN LOWER(COALESCE(estado_publicacion, '')) = 'publicado' THEN 'PUBLICADA'
    WHEN LOWER(COALESCE(estado_solicitud, '')) = 'aprobada' THEN 'APROBADA'
    WHEN LOWER(COALESCE(estado_solicitud, '')) = 'rechazada' THEN 'RECHAZADA'
    ELSE COALESCE(estado, 'PENDIENTE')
END
WHERE estado IS NULL OR estado = 'PENDIENTE';

UPDATE solicitudes_evento
SET motivo_rechazo = comentario_revision
WHERE motivo_rechazo IS NULL
  AND comentario_revision IS NOT NULL
  AND LOWER(COALESCE(estado_solicitud, '')) = 'rechazada';

UPDATE solicitudes_evento
SET id_usuario_revisor = COALESCE(id_usuario_revisor, id_revisado_por)
WHERE id_usuario_revisor IS NULL
  AND id_revisado_por IS NOT NULL;

UPDATE solicitudes_evento
SET fecha_creacion = COALESCE(fecha_creacion, fecha_solicitud, CURRENT_TIMESTAMP)
WHERE fecha_creacion IS NULL;

UPDATE solicitudes_evento
SET tipo_evento = COALESCE(NULLIF(tipo_evento, ''), 'INSTITUCIONAL')
WHERE tipo_evento IS NULL OR tipo_evento = '';

UPDATE solicitudes_evento se
JOIN tipos_evento te ON UPPER(se.tipo_evento) = te.nombre
SET se.id_tipo_evento = te.id_tipo_evento
WHERE se.id_tipo_evento IS NULL;

CALL add_column_if_missing('solicitudes_anuncio', 'motivo_rechazo', 'VARCHAR(1000) NULL');
CALL add_column_if_missing('solicitudes_anuncio', 'fecha_creacion', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP');

INSERT INTO publicaciones_anuncio (
    id_solicitud_anuncio,
    titulo_visible,
    descripcion_visible,
    pieza_grafica_url,
    fecha_publicacion,
    visible,
    id_usuario_publicador
)
SELECT
    sa.id_solicitud_anuncio,
    sa.titulo,
    sa.descripcion,
    NULL,
    NOW(),
    b'1',
    COALESCE(sa.id_usuario_revisor, sa.id_usuario_solicitante)
FROM solicitudes_anuncio sa
LEFT JOIN publicaciones_anuncio pa
    ON pa.id_solicitud_anuncio = sa.id_solicitud_anuncio
WHERE sa.estado = 'PUBLICADA'
  AND pa.id_publicacion_anuncio IS NULL;

CALL add_index_if_missing('solicitudes_evento', 'idx_solicitudes_evento_estado', '(estado)');
CALL add_index_if_missing('solicitudes_evento', 'idx_solicitudes_evento_fecha', '(fecha_creacion)');
CALL add_index_if_missing('solicitudes_anuncio', 'idx_solicitudes_anuncio_estado', '(estado)');
CALL add_index_if_missing('solicitudes_anuncio', 'idx_solicitudes_anuncio_fecha', '(fecha_creacion)');

CALL add_index_if_missing('solicitudes_evento', 'idx_solicitudes_evento_usuario_solicitante', '(id_usuario_solicitante)');
CALL add_index_if_missing('solicitudes_evento', 'idx_solicitudes_evento_usuario_revisor', '(id_usuario_revisor)');
CALL add_index_if_missing('solicitudes_evento', 'idx_solicitudes_evento_tipo_evento', '(id_tipo_evento)');
CALL add_index_if_missing('solicitud_evento_participantes', 'idx_participantes_solicitud_evento', '(id_solicitud_evento)');
CALL add_index_if_missing('solicitudes_anuncio', 'idx_solicitudes_anuncio_usuario_solicitante', '(id_usuario_solicitante)');
CALL add_index_if_missing('solicitudes_anuncio', 'idx_solicitudes_anuncio_usuario_revisor', '(id_usuario_revisor)');
CALL add_index_if_missing('publicaciones_evento', 'idx_publicaciones_evento_solicitud', '(id_solicitud_evento)');
CALL add_index_if_missing('publicaciones_evento', 'idx_publicaciones_evento_publicador', '(id_usuario_publicador)');
CALL add_index_if_missing('publicaciones_anuncio', 'idx_publicaciones_anuncio_publicador', '(id_usuario_publicador)');

CALL add_fk_if_missing(
    'solicitudes_evento',
    'fk_solicitudes_evento_usuario_solicitante',
    'ALTER TABLE solicitudes_evento ADD CONSTRAINT fk_solicitudes_evento_usuario_solicitante FOREIGN KEY (id_usuario_solicitante) REFERENCES usuarios(id_usuario)'
);
CALL add_fk_if_missing(
    'solicitudes_evento',
    'fk_solicitudes_evento_usuario_revisor',
    'ALTER TABLE solicitudes_evento ADD CONSTRAINT fk_solicitudes_evento_usuario_revisor FOREIGN KEY (id_usuario_revisor) REFERENCES usuarios(id_usuario)'
);
CALL add_fk_if_missing(
    'solicitudes_evento',
    'fk_solicitudes_evento_tipo_evento',
    'ALTER TABLE solicitudes_evento ADD CONSTRAINT fk_solicitudes_evento_tipo_evento FOREIGN KEY (id_tipo_evento) REFERENCES tipos_evento(id_tipo_evento)'
);
CALL add_fk_if_missing(
    'solicitud_evento_participantes',
    'fk_participantes_solicitud_evento',
    'ALTER TABLE solicitud_evento_participantes ADD CONSTRAINT fk_participantes_solicitud_evento FOREIGN KEY (id_solicitud_evento) REFERENCES solicitudes_evento(id_solicitud)'
);
CALL add_fk_if_missing(
    'solicitudes_anuncio',
    'fk_solicitudes_anuncio_usuario_solicitante',
    'ALTER TABLE solicitudes_anuncio ADD CONSTRAINT fk_solicitudes_anuncio_usuario_solicitante FOREIGN KEY (id_usuario_solicitante) REFERENCES usuarios(id_usuario)'
);
CALL add_fk_if_missing(
    'solicitudes_anuncio',
    'fk_solicitudes_anuncio_usuario_revisor',
    'ALTER TABLE solicitudes_anuncio ADD CONSTRAINT fk_solicitudes_anuncio_usuario_revisor FOREIGN KEY (id_usuario_revisor) REFERENCES usuarios(id_usuario)'
);
CALL add_fk_if_missing(
    'publicaciones_evento',
    'fk_publicaciones_evento_solicitud',
    'ALTER TABLE publicaciones_evento ADD CONSTRAINT fk_publicaciones_evento_solicitud FOREIGN KEY (id_solicitud_evento) REFERENCES solicitudes_evento(id_solicitud)'
);
CALL add_fk_if_missing(
    'publicaciones_evento',
    'fk_publicaciones_evento_publicador',
    'ALTER TABLE publicaciones_evento ADD CONSTRAINT fk_publicaciones_evento_publicador FOREIGN KEY (id_usuario_publicador) REFERENCES usuarios(id_usuario)'
);
CALL add_fk_if_missing(
    'publicaciones_anuncio',
    'fk_publicaciones_anuncio_publicador',
    'ALTER TABLE publicaciones_anuncio ADD CONSTRAINT fk_publicaciones_anuncio_publicador FOREIGN KEY (id_usuario_publicador) REFERENCES usuarios(id_usuario)'
);

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;
DROP PROCEDURE IF EXISTS add_fk_if_missing;
DROP PROCEDURE IF EXISTS modify_column_if_needed;

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

-- Revision recomendada posterior:
-- 1. Verificar datos de nombre vs nombre_oficina en oficinas.
-- 2. Verificar si invitados_evento y entidades_conjunto se migraran a solicitud_evento_participantes.
-- 3. Cuando la app nueva este estable, eliminar columnas heredadas duplicadas.
