-- CallApp - Migracion manual de limpieza fina final
-- Objetivo:
-- 1. Endurecer columnas obligatorias segun el backend actual
-- 2. Limpiar nombres tecnicos pendientes
-- 3. Retirar tablas legacy que ya no participan en el flujo nuevo

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

DROP PROCEDURE IF EXISTS drop_table_if_exists $$
CREATE PROCEDURE drop_table_if_exists(IN p_table_name VARCHAR(128))
BEGIN
    DECLARE v_count INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_count
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name;

    IF v_count > 0 THEN
        SET @sql = CONCAT('DROP TABLE ', p_table_name);
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

-- 1. Endurecer rol de usuario
UPDATE usuarios
SET rol = CASE id_rol
    WHEN 1 THEN 'SUPER_ADMIN'
    WHEN 2 THEN 'COMUNICACIONES'
    WHEN 3 THEN 'OFICINA'
    WHEN 4 THEN 'USUARIO_APP'
    WHEN 5 THEN 'USUARIO_AUTENTICADO_APP'
    ELSE 'USUARIO_APP'
END
WHERE rol IS NULL OR rol = '';

CALL modify_column_if_needed('usuarios', 'rol', 'VARCHAR(20) NOT NULL');

-- 2. Renombrar la FK autogenerada de publicaciones_anuncio a un nombre limpio
CALL drop_fk_if_exists('publicaciones_anuncio', 'FKblt2jsiqoa4ddgnkr6q5xiae');
CALL add_fk_if_missing(
    'publicaciones_anuncio',
    'fk_publicaciones_anuncio_solicitud',
    'ALTER TABLE publicaciones_anuncio ADD CONSTRAINT fk_publicaciones_anuncio_solicitud FOREIGN KEY (id_solicitud_anuncio) REFERENCES solicitudes_anuncio(id_solicitud_anuncio)'
);

-- 3. Retirar tablas legacy que ya no usa el backend actual
CALL drop_table_if_exists('eventos');
CALL drop_table_if_exists('notificaciones');

DROP PROCEDURE IF EXISTS modify_column_if_needed;
DROP PROCEDURE IF EXISTS drop_table_if_exists;
DROP PROCEDURE IF EXISTS drop_fk_if_exists;
DROP PROCEDURE IF EXISTS add_fk_if_missing;

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

-- Resultado esperado:
-- 1. usuarios.rol queda obligatorio.
-- 2. publicaciones_anuncio usa una FK con nombre estable.
-- 3. eventos y notificaciones legacy desaparecen del esquema productivo.
