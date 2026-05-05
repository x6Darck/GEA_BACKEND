-- CallApp - Migracion manual para dashboard, anuncios enriquecidos e historial de reportes
-- Objetivo:
-- 1. Enriquecer solicitudes_anuncio con campos del modal administrativo
-- 2. Crear la tabla de reportes_generados para historial del panel de reportes

USE eventos_institucionales;

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

CALL add_column_if_missing('solicitudes_anuncio', 'lugar', 'VARCHAR(200) NULL');
CALL add_column_if_missing('solicitudes_anuncio', 'correo_contacto', 'VARCHAR(150) NULL');
CALL add_column_if_missing('solicitudes_anuncio', 'responsable_anuncio', 'VARCHAR(150) NULL');
CALL add_column_if_missing('solicitudes_anuncio', 'fecha_inicio_publicacion', 'DATE NULL');
CALL add_column_if_missing('solicitudes_anuncio', 'fecha_fin_publicacion', 'DATE NULL');
CALL add_column_if_missing('solicitudes_anuncio', 'hora_inicio', 'TIME NULL');
CALL add_column_if_missing('solicitudes_anuncio', 'hora_fin', 'TIME NULL');

CREATE TABLE IF NOT EXISTS reportes_generados (
    id_reporte_generado BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(160) NOT NULL,
    descripcion VARCHAR(500) NULL,
    formato VARCHAR(20) NOT NULL,
    fecha_desde DATE NOT NULL,
    fecha_hasta DATE NOT NULL,
    alcance VARCHAR(30) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario_generador INT NOT NULL
);

CALL modify_column_if_needed('reportes_generados', 'id_usuario_generador', 'INT NOT NULL');

CALL add_index_if_missing('reportes_generados', 'idx_reportes_generados_fecha_creacion', '(fecha_creacion)');
CALL add_index_if_missing('reportes_generados', 'idx_reportes_generados_usuario', '(id_usuario_generador)');

CALL add_fk_if_missing(
    'reportes_generados',
    'fk_reportes_generados_usuario',
    'ALTER TABLE reportes_generados ADD CONSTRAINT fk_reportes_generados_usuario FOREIGN KEY (id_usuario_generador) REFERENCES usuarios(id_usuario)'
);

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;
DROP PROCEDURE IF EXISTS add_fk_if_missing;
DROP PROCEDURE IF EXISTS modify_column_if_needed;

-- Resultado esperado:
-- 1. solicitudes_anuncio soporta lugar, correo, responsable y vigencia.
-- 2. reportes_generados queda disponible para el historial del panel de reportes.
