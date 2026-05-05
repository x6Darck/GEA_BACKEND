-- CallApp - Tercera migracion manual de retiro final del legado
-- Ejecutar despues de:
-- 1. V2026_03_20__normalizar_esquema_callapp.sql
-- 2. V2026_03_20__depurar_esquema_legacy_callapp.sql
--
-- Objetivo:
-- 1. Dejar una sola columna oficial por concepto
-- 2. Retirar tablas legacy ya respaldadas
-- 3. Alinear definitivamente la BD con el backend actual

USE eventos_institucionales;

SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET SQL_SAFE_UPDATES = 0;
SET FOREIGN_KEY_CHECKS = 0;

DELIMITER $$

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

DELIMITER ;

-- Asegurar consolidacion final antes de eliminar duplicados
UPDATE oficinas
SET nombre = COALESCE(NULLIF(nombre, ''), nombre_oficina)
WHERE nombre IS NULL OR nombre = '';

UPDATE solicitudes_evento
SET descripcion_evento = COALESCE(NULLIF(descripcion_evento, ''), descripcion)
WHERE descripcion_evento IS NULL OR descripcion_evento = '';

-- Retirar columnas duplicadas ya obsoletas
CALL drop_column_if_exists('oficinas', 'nombre_oficina');
CALL drop_column_if_exists('solicitudes_evento', 'descripcion');

-- Retirar tablas legacy ya respaldadas y vaciadas
CALL drop_table_if_exists('anuncios');
CALL drop_table_if_exists('invitados_evento');
CALL drop_table_if_exists('entidades_conjunto');

DROP PROCEDURE IF EXISTS drop_column_if_exists;
DROP PROCEDURE IF EXISTS drop_table_if_exists;

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;
SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;

-- Resultado esperado:
-- 1. oficinas.nombre queda como columna oficial.
-- 2. solicitudes_evento.descripcion_evento queda como columna oficial.
-- 3. anuncios, invitados_evento y entidades_conjunto desaparecen del esquema productivo.
-- 4. Los respaldos backup_* permanecen disponibles.
