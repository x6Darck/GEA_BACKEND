-- CallApp - Migracion manual para enriquecer usuarios y soporte de dashboard
-- Objetivo:
-- 1. Agregar telefono a usuarios para alinear con el panel de administracion
-- 2. Preparar datos sin romper compatibilidad

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

DELIMITER ;

CALL add_column_if_missing('usuarios', 'telefono', 'VARCHAR(30) NULL');

DROP PROCEDURE IF EXISTS add_column_if_missing;

-- Resultado esperado:
-- 1. usuarios.telefono queda disponible para el panel de usuarios.
