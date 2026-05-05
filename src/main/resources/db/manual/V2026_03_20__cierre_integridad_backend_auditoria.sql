-- CallApp - Migracion manual de cierre de integridad y auditoria
-- Objetivo:
-- 1. Alinear restricciones de BD con el backend actual
-- 2. Crear infraestructura base de auditoria Envers manualmente

USE eventos_institucionales;

SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

DELIMITER $$

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

DELIMITER ;

-- 1. Cierre de integridad en tablas productivas
UPDATE solicitudes_evento
SET id_usuario_solicitante = id_usuario_revisor
WHERE id_usuario_solicitante IS NULL
  AND id_usuario_revisor IS NOT NULL;

UPDATE solicitudes_evento
SET fecha_creacion = CURRENT_TIMESTAMP
WHERE fecha_creacion IS NULL;

CALL modify_column_if_needed('solicitudes_evento', 'id_usuario_solicitante', 'INT NOT NULL');
CALL modify_column_if_needed('solicitudes_evento', 'fecha_creacion', 'TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP');

-- 2. Infraestructura base de auditoria Envers
CREATE TABLE IF NOT EXISTS REVINFO (
    REV INT AUTO_INCREMENT PRIMARY KEY,
    REVTSTMP BIGINT
);

CREATE TABLE IF NOT EXISTS usuarios_AUD (
    id_usuario INT NOT NULL,
    REV INT NOT NULL,
    REVTYPE TINYINT NULL,
    nombre VARCHAR(255) NULL,
    correo VARCHAR(255) NULL,
    telefono VARCHAR(30) NULL,
    password VARCHAR(255) NULL,
    id_rol INT NULL,
    id_oficina INT NULL,
    estado VARCHAR(255) NULL,
    auth_provider ENUM('LOCAL','MICROSOFT') NULL,
    microsoft_oid VARCHAR(255) NULL,
    fecha_creacion TIMESTAMP NULL,
    rol VARCHAR(20) NULL,
    PRIMARY KEY (id_usuario, REV)
);

CREATE TABLE IF NOT EXISTS oficinas_AUD (
    id_oficina INT NOT NULL,
    REV INT NOT NULL,
    REVTYPE TINYINT NULL,
    nombre VARCHAR(120) NULL,
    programa_academico VARCHAR(120) NULL,
    fecha_creacion DATETIME NULL,
    descripcion VARCHAR(500) NULL,
    activa BIT NULL,
    PRIMARY KEY (id_oficina, REV)
);

CREATE TABLE IF NOT EXISTS tipos_evento_AUD (
    id_tipo_evento BIGINT NOT NULL,
    REV INT NOT NULL,
    REVTYPE TINYINT NULL,
    nombre VARCHAR(80) NULL,
    descripcion VARCHAR(300) NULL,
    color_hex VARCHAR(7) NULL,
    activo BIT NULL,
    PRIMARY KEY (id_tipo_evento, REV)
);

CREATE TABLE IF NOT EXISTS solicitudes_evento_AUD (
    id_solicitud INT NOT NULL,
    REV INT NOT NULL,
    REVTYPE TINYINT NULL,
    nombre_evento VARCHAR(160) NULL,
    descripcion_evento VARCHAR(2000) NULL,
    fecha_evento DATE NULL,
    hora_inicio TIME NULL,
    hora_fin TIME NULL,
    lugar VARCHAR(200) NULL,
    responsable_evento VARCHAR(120) NULL,
    tipo_evento VARCHAR(120) NULL,
    estado ENUM('APROBADA','PENDIENTE','PUBLICADA','RECHAZADA') NULL,
    motivo_rechazo VARCHAR(1000) NULL,
    fecha_creacion TIMESTAMP NULL,
    fecha_revision TIMESTAMP NULL,
    id_oficina INT NULL,
    id_usuario_solicitante INT NULL,
    id_usuario_revisor INT NULL,
    id_tipo_evento BIGINT NULL,
    PRIMARY KEY (id_solicitud, REV)
);

CREATE TABLE IF NOT EXISTS solicitud_evento_participantes_AUD (
    id_participante BIGINT NOT NULL,
    REV INT NOT NULL,
    REVTYPE TINYINT NULL,
    nombre VARCHAR(120) NULL,
    cargo VARCHAR(120) NULL,
    descripcion VARCHAR(1000) NULL,
    foto_url VARCHAR(500) NULL,
    tipo ENUM('INVITADO','COLABORADOR') NULL,
    id_solicitud_evento INT NULL,
    PRIMARY KEY (id_participante, REV)
);

CREATE TABLE IF NOT EXISTS publicaciones_evento_AUD (
    id_publicacion_evento BIGINT NOT NULL,
    REV INT NOT NULL,
    REVTYPE TINYINT NULL,
    id_solicitud_evento INT NULL,
    titulo_visible VARCHAR(160) NULL,
    descripcion_visible VARCHAR(2000) NULL,
    pieza_grafica_url VARCHAR(500) NULL,
    fecha_publicacion DATETIME(6) NULL,
    visible BIT NULL,
    id_usuario_publicador INT NULL,
    PRIMARY KEY (id_publicacion_evento, REV)
);

CREATE TABLE IF NOT EXISTS solicitudes_anuncio_AUD (
    id_solicitud_anuncio BIGINT NOT NULL,
    REV INT NOT NULL,
    REVTYPE TINYINT NULL,
    titulo VARCHAR(160) NULL,
    descripcion VARCHAR(2000) NULL,
    categoria VARCHAR(100) NULL,
    lugar VARCHAR(200) NULL,
    correo_contacto VARCHAR(150) NULL,
    responsable_anuncio VARCHAR(150) NULL,
    fecha_inicio_publicacion DATE NULL,
    fecha_fin_publicacion DATE NULL,
    hora_inicio TIME(6) NULL,
    hora_fin TIME(6) NULL,
    estado ENUM('APROBADA','PENDIENTE','PUBLICADA','RECHAZADA') NULL,
    motivo_rechazo VARCHAR(1000) NULL,
    fecha_creacion DATETIME(6) NULL,
    fecha_revision DATETIME(6) NULL,
    id_usuario_solicitante INT NULL,
    id_usuario_revisor INT NULL,
    PRIMARY KEY (id_solicitud_anuncio, REV)
);

CREATE TABLE IF NOT EXISTS publicaciones_anuncio_AUD (
    id_publicacion_anuncio BIGINT NOT NULL,
    REV INT NOT NULL,
    REVTYPE TINYINT NULL,
    id_solicitud_anuncio BIGINT NULL,
    titulo_visible VARCHAR(160) NULL,
    descripcion_visible VARCHAR(2000) NULL,
    pieza_grafica_url VARCHAR(500) NULL,
    fecha_publicacion DATETIME(6) NULL,
    visible BIT NULL,
    id_usuario_publicador INT NULL,
    PRIMARY KEY (id_publicacion_anuncio, REV)
);

CALL add_index_if_missing('usuarios_AUD', 'idx_usuarios_aud_rev', '(REV)');
CALL add_index_if_missing('oficinas_AUD', 'idx_oficinas_aud_rev', '(REV)');
CALL add_index_if_missing('tipos_evento_AUD', 'idx_tipos_evento_aud_rev', '(REV)');
CALL add_index_if_missing('solicitudes_evento_AUD', 'idx_solicitudes_evento_aud_rev', '(REV)');
CALL add_index_if_missing('solicitud_evento_participantes_AUD', 'idx_participantes_aud_rev', '(REV)');
CALL add_index_if_missing('publicaciones_evento_AUD', 'idx_publicaciones_evento_aud_rev', '(REV)');
CALL add_index_if_missing('solicitudes_anuncio_AUD', 'idx_solicitudes_anuncio_aud_rev', '(REV)');
CALL add_index_if_missing('publicaciones_anuncio_AUD', 'idx_publicaciones_anuncio_aud_rev', '(REV)');

CALL add_fk_if_missing('usuarios_AUD', 'fk_usuarios_aud_revinfo',
    'ALTER TABLE usuarios_AUD ADD CONSTRAINT fk_usuarios_aud_revinfo FOREIGN KEY (REV) REFERENCES REVINFO(REV)');
CALL add_fk_if_missing('oficinas_AUD', 'fk_oficinas_aud_revinfo',
    'ALTER TABLE oficinas_AUD ADD CONSTRAINT fk_oficinas_aud_revinfo FOREIGN KEY (REV) REFERENCES REVINFO(REV)');
CALL add_fk_if_missing('tipos_evento_AUD', 'fk_tipos_evento_aud_revinfo',
    'ALTER TABLE tipos_evento_AUD ADD CONSTRAINT fk_tipos_evento_aud_revinfo FOREIGN KEY (REV) REFERENCES REVINFO(REV)');
CALL add_fk_if_missing('solicitudes_evento_AUD', 'fk_solicitudes_evento_aud_revinfo',
    'ALTER TABLE solicitudes_evento_AUD ADD CONSTRAINT fk_solicitudes_evento_aud_revinfo FOREIGN KEY (REV) REFERENCES REVINFO(REV)');
CALL add_fk_if_missing('solicitud_evento_participantes_AUD', 'fk_participantes_aud_revinfo',
    'ALTER TABLE solicitud_evento_participantes_AUD ADD CONSTRAINT fk_participantes_aud_revinfo FOREIGN KEY (REV) REFERENCES REVINFO(REV)');
CALL add_fk_if_missing('publicaciones_evento_AUD', 'fk_publicaciones_evento_aud_revinfo',
    'ALTER TABLE publicaciones_evento_AUD ADD CONSTRAINT fk_publicaciones_evento_aud_revinfo FOREIGN KEY (REV) REFERENCES REVINFO(REV)');
CALL add_fk_if_missing('solicitudes_anuncio_AUD', 'fk_solicitudes_anuncio_aud_revinfo',
    'ALTER TABLE solicitudes_anuncio_AUD ADD CONSTRAINT fk_solicitudes_anuncio_aud_revinfo FOREIGN KEY (REV) REFERENCES REVINFO(REV)');
CALL add_fk_if_missing('publicaciones_anuncio_AUD', 'fk_publicaciones_anuncio_aud_revinfo',
    'ALTER TABLE publicaciones_anuncio_AUD ADD CONSTRAINT fk_publicaciones_anuncio_aud_revinfo FOREIGN KEY (REV) REFERENCES REVINFO(REV)');

DROP PROCEDURE IF EXISTS modify_column_if_needed;
DROP PROCEDURE IF EXISTS add_index_if_missing;
DROP PROCEDURE IF EXISTS add_fk_if_missing;

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

-- Resultado esperado:
-- 1. solicitudes_evento.id_usuario_solicitante queda obligatorio.
-- 2. solicitudes_evento.fecha_creacion queda no nula.
-- 3. REVINFO y tablas *_AUD quedan listas para auditoria.
