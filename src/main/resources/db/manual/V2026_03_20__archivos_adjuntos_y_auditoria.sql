-- CallApp - Migracion manual para metadatos de archivos
-- Objetivo:
-- 1. Registrar archivos adjuntos con token publico seguro
-- 2. Preparar soporte para exposicion controlada

USE eventos_institucionales;

CREATE TABLE IF NOT EXISTS archivos_adjuntos (
    id_archivo_adjunto BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre_original VARCHAR(255) NOT NULL,
    nombre_almacenado VARCHAR(255) NOT NULL UNIQUE,
    token_acceso VARCHAR(120) NOT NULL UNIQUE,
    content_type VARCHAR(120) NOT NULL,
    tamano BIGINT NOT NULL,
    publico BIT NOT NULL DEFAULT b'1',
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DELIMITER $$

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

DELIMITER ;

CALL add_index_if_missing('archivos_adjuntos', 'idx_archivos_adjuntos_token_acceso', '(token_acceso)');

DROP PROCEDURE IF EXISTS add_index_if_missing;

-- Nota:
-- Las tablas de auditoria Envers (REVINFO y *_AUD) pueden ser creadas automaticamente
-- por Hibernate al iniciar la aplicacion con ddl-auto=update.
