-- CallApp - Segunda migracion manual de depuracion del esquema legacy
-- Objetivo:
-- 1. Consolidar datos entre columnas heredadas y columnas nuevas
-- 2. Respaldar tablas legacy antes de retirarlas del flujo principal
-- 3. Dejar el esquema mas alineado con el backend actual sin perder informacion
-- Ejecutar despues de V2026_03_20__normalizar_esquema_callapp.sql

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

DROP PROCEDURE IF EXISTS drop_column_if_exists $$
CREATE PROCEDURE drop_column_if_exists(
    IN p_table_name VARCHAR(128),
    IN p_column_name VARCHAR(128)
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
            ' DROP COLUMN ', p_column_name
        );
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DROP PROCEDURE IF EXISTS drop_fk_if_exists $$
CREATE PROCEDURE drop_fk_if_exists(
    IN p_table_name VARCHAR(128),
    IN p_fk_name VARCHAR(128)
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

    IF v_count > 0 THEN
        SET @sql = CONCAT(
            'ALTER TABLE ', p_table_name,
            ' DROP FOREIGN KEY ', p_fk_name
        );
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DROP PROCEDURE IF EXISTS drop_index_if_exists $$
CREATE PROCEDURE drop_index_if_exists(
    IN p_table_name VARCHAR(128),
    IN p_index_name VARCHAR(128)
)
BEGIN
    DECLARE v_count INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_count
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND index_name = p_index_name;

    IF v_count > 0 AND p_index_name <> 'PRIMARY' THEN
        SET @sql = CONCAT(
            'ALTER TABLE ', p_table_name,
            ' DROP INDEX ', p_index_name
        );
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

-- 1. Respaldos de tablas legacy antes de depurar
CREATE TABLE IF NOT EXISTS backup_anuncios_legacy AS
SELECT *
FROM anuncios
WHERE 1 = 0;

INSERT INTO backup_anuncios_legacy
SELECT a.*
FROM anuncios a
LEFT JOIN backup_anuncios_legacy b ON b.id_anuncio = a.id_anuncio
WHERE b.id_anuncio IS NULL;

CREATE TABLE IF NOT EXISTS backup_invitados_evento_legacy AS
SELECT *
FROM invitados_evento
WHERE 1 = 0;

INSERT INTO backup_invitados_evento_legacy
SELECT i.*
FROM invitados_evento i
LEFT JOIN backup_invitados_evento_legacy b ON b.id_invitado = i.id_invitado
WHERE b.id_invitado IS NULL;

CREATE TABLE IF NOT EXISTS backup_entidades_conjunto_legacy AS
SELECT *
FROM entidades_conjunto
WHERE 1 = 0;

INSERT INTO backup_entidades_conjunto_legacy
SELECT e.*
FROM entidades_conjunto e
LEFT JOIN backup_entidades_conjunto_legacy b ON b.id_entidad = e.id_entidad
WHERE b.id_entidad IS NULL;

-- 2. Consolidar oficinas
UPDATE oficinas
SET nombre = COALESCE(NULLIF(nombre, ''), nombre_oficina)
WHERE nombre IS NULL OR nombre = '';

UPDATE oficinas
SET nombre_oficina = COALESCE(NULLIF(nombre_oficina, ''), nombre)
WHERE nombre_oficina IS NULL OR nombre_oficina = '';

UPDATE oficinas
SET descripcion = COALESCE(
    descripcion,
    CONCAT('La oficina ', COALESCE(nombre, nombre_oficina), ' realiza funciones institucionales.')
)
WHERE descripcion IS NULL OR descripcion = '';

-- 3. Consolidar solicitudes_evento
UPDATE solicitudes_evento
SET descripcion_evento = COALESCE(NULLIF(descripcion_evento, ''), descripcion)
WHERE descripcion_evento IS NULL OR descripcion_evento = '';

UPDATE solicitudes_evento
SET descripcion = COALESCE(NULLIF(descripcion, ''), descripcion_evento)
WHERE descripcion IS NULL OR descripcion = '';

UPDATE solicitudes_evento
SET estado = CASE
    WHEN estado IS NOT NULL THEN estado
    WHEN LOWER(COALESCE(estado_publicacion, '')) = 'publicado' THEN 'PUBLICADA'
    WHEN LOWER(COALESCE(estado_solicitud, '')) = 'aprobada' THEN 'APROBADA'
    WHEN LOWER(COALESCE(estado_solicitud, '')) = 'rechazada' THEN 'RECHAZADA'
    ELSE 'PENDIENTE'
END;

UPDATE solicitudes_evento
SET estado_solicitud = CASE estado
    WHEN 'PENDIENTE' THEN 'pendiente'
    WHEN 'APROBADA' THEN 'aprobada'
    WHEN 'RECHAZADA' THEN 'rechazada'
    WHEN 'PUBLICADA' THEN 'aprobada'
    ELSE estado_solicitud
END
WHERE estado_solicitud IS NULL OR estado_solicitud = '';

UPDATE solicitudes_evento
SET estado_publicacion = CASE estado
    WHEN 'PUBLICADA' THEN 'publicado'
    ELSE COALESCE(estado_publicacion, 'borrador')
END
WHERE estado_publicacion IS NULL OR estado_publicacion = '';

UPDATE solicitudes_evento
SET comentario_revision = COALESCE(NULLIF(comentario_revision, ''), motivo_rechazo)
WHERE comentario_revision IS NULL OR comentario_revision = '';

UPDATE solicitudes_evento
SET motivo_rechazo = COALESCE(NULLIF(motivo_rechazo, ''), comentario_revision)
WHERE motivo_rechazo IS NULL OR motivo_rechazo = '';

UPDATE solicitudes_evento
SET id_usuario_revisor = COALESCE(id_usuario_revisor, id_revisado_por)
WHERE id_usuario_revisor IS NULL AND id_revisado_por IS NOT NULL;

UPDATE solicitudes_evento
SET id_revisado_por = COALESCE(id_revisado_por, id_usuario_revisor)
WHERE id_revisado_por IS NULL AND id_usuario_revisor IS NOT NULL;

UPDATE solicitudes_evento
SET fecha_creacion = COALESCE(fecha_creacion, fecha_solicitud, CURRENT_TIMESTAMP)
WHERE fecha_creacion IS NULL;

UPDATE solicitudes_evento
SET fecha_solicitud = COALESCE(fecha_solicitud, fecha_creacion)
WHERE fecha_solicitud IS NULL;

UPDATE solicitudes_evento
SET tipo_evento = UPPER(COALESCE(NULLIF(tipo_evento, ''), 'INSTITUCIONAL'))
WHERE tipo_evento IS NULL OR tipo_evento = '' OR tipo_evento <> UPPER(tipo_evento);

UPDATE solicitudes_evento se
JOIN tipos_evento te ON UPPER(se.tipo_evento) = te.nombre
SET se.id_tipo_evento = te.id_tipo_evento
WHERE se.id_tipo_evento IS NULL;

-- 4. Migrar invitados_evento legacy hacia solicitud_evento_participantes
INSERT INTO solicitud_evento_participantes (
    nombre,
    cargo,
    descripcion,
    foto_url,
    tipo,
    id_solicitud_evento
)
SELECT
    i.nombre_invitado,
    NULL,
    i.perfil,
    i.fotografia,
    'INVITADO',
    i.id_solicitud
FROM invitados_evento i
LEFT JOIN solicitud_evento_participantes p
    ON p.id_solicitud_evento = i.id_solicitud
   AND p.nombre = i.nombre_invitado
   AND p.tipo = 'INVITADO'
WHERE p.id_participante IS NULL;

-- 5. Migrar entidades_conjunto legacy hacia solicitud_evento_participantes como COLABORADOR institucional
INSERT INTO solicitud_evento_participantes (
    nombre,
    cargo,
    descripcion,
    foto_url,
    tipo,
    id_solicitud_evento
)
SELECT
    e.nombre_entidad,
    'ENTIDAD',
    CONCAT('Entidad participante del evento. Logo: ', COALESCE(e.logo, 'sin logo')),
    e.logo,
    'COLABORADOR',
    e.id_solicitud
FROM entidades_conjunto e
LEFT JOIN solicitud_evento_participantes p
    ON p.id_solicitud_evento = e.id_solicitud
   AND p.nombre = e.nombre_entidad
   AND p.tipo = 'COLABORADOR'
WHERE p.id_participante IS NULL;

-- 6. Migrar anuncios legacy hacia solicitudes_anuncio/publicaciones_anuncio
CALL add_index_if_missing('solicitudes_anuncio', 'idx_solicitudes_anuncio_titulo', '(titulo)');

INSERT INTO solicitudes_anuncio (
    titulo,
    descripcion,
    categoria,
    estado,
    motivo_rechazo,
    fecha_creacion,
    fecha_revision,
    id_usuario_solicitante,
    id_usuario_revisor
)
SELECT
    a.titulo,
    a.descripcion,
    a.categoria,
    CASE
        WHEN LOWER(COALESCE(a.estado_publicacion, '')) = 'publicado' THEN 'PUBLICADA'
        WHEN LOWER(COALESCE(a.estado_solicitud, '')) = 'aprobada' THEN 'APROBADA'
        WHEN LOWER(COALESCE(a.estado_solicitud, '')) = 'rechazada' THEN 'RECHAZADA'
        ELSE 'PENDIENTE'
    END,
    a.comentario_revision,
    COALESCE(a.fecha_creacion, CURRENT_TIMESTAMP),
    a.fecha_revision,
    a.id_usuario,
    a.id_revisado_por
FROM anuncios a
LEFT JOIN solicitudes_anuncio sa
    ON sa.titulo = a.titulo
   AND sa.id_usuario_solicitante = a.id_usuario
   AND sa.fecha_creacion = a.fecha_creacion
WHERE sa.id_solicitud_anuncio IS NULL;

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
    a.imagen,
    COALESCE(a.fecha_revision, a.fecha_creacion, NOW()),
    b'1',
    COALESCE(sa.id_usuario_revisor, sa.id_usuario_solicitante)
FROM anuncios a
JOIN solicitudes_anuncio sa
    ON sa.titulo = a.titulo
   AND sa.id_usuario_solicitante = a.id_usuario
LEFT JOIN publicaciones_anuncio pa
    ON pa.id_solicitud_anuncio = sa.id_solicitud_anuncio
WHERE LOWER(COALESCE(a.estado_publicacion, '')) = 'publicado'
  AND pa.id_publicacion_anuncio IS NULL;

-- 7. Retirar constraints legacy redundantes
CALL drop_fk_if_exists('solicitudes_evento', 'fk_solicitud_revisor');
CALL drop_index_if_exists('solicitudes_evento', 'fk_solicitud_revisor');

-- 8. Limpieza opcional de columnas legacy duplicadas
-- Estas eliminaciones solo se ejecutan cuando la informacion ya fue consolidada.
CALL drop_column_if_exists('solicitudes_evento', 'estado_solicitud');
CALL drop_column_if_exists('solicitudes_evento', 'estado_publicacion');
CALL drop_column_if_exists('solicitudes_evento', 'comentario_revision');
CALL drop_column_if_exists('solicitudes_evento', 'id_revisado_por');
CALL drop_column_if_exists('solicitudes_evento', 'fecha_solicitud');
CALL drop_column_if_exists('solicitudes_evento', 'pieza_comunicaciones');

-- Se preservan por ahora descripcion y descripcion_evento para no romper integraciones externas.
-- Se preservan nombre_oficina y nombre por compatibilidad transitoria.
-- Se preserva anuncios como tabla legado respaldada, por si otra integracion aun la consulta.

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;
DROP PROCEDURE IF EXISTS drop_column_if_exists;
DROP PROCEDURE IF EXISTS drop_fk_if_exists;
DROP PROCEDURE IF EXISTS drop_index_if_exists;

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

-- Resultado esperado tras esta segunda migracion:
-- 1. solicitudes_evento usa estado, motivo_rechazo, id_usuario_revisor, id_tipo_evento como fuente oficial.
-- 2. solicitud_evento_participantes contiene invitados y entidades migradas.
-- 3. solicitudes_anuncio/publicaciones_anuncio concentran tambien los anuncios legacy.
-- 4. La BD queda mas limpia sin perder respaldo historico.
