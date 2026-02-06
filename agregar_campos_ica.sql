-- Script SQL para agregar campos ICA de forma segura
-- Verifica si las columnas existen antes de agregarlas

-- 1. Agregar campos ICA en BusinessSettings (solo si no existen)
SET @dbname = DATABASE();
SET @tablename = 'business_settings';
SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @dbname
        AND TABLE_NAME = @tablename
        AND COLUMN_NAME = 'ica_rate'
    ) > 0,
    'SELECT "La columna ica_rate ya existe en business_settings" AS mensaje;',
    'ALTER TABLE `business_settings` ADD COLUMN `ica_rate` DOUBLE NOT NULL DEFAULT 1.0 AFTER `rete_threshold`;'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @dbname
        AND TABLE_NAME = @tablename
        AND COLUMN_NAME = 'ica_threshold'
    ) > 0,
    'SELECT "La columna ica_threshold ya existe en business_settings" AS mensaje;',
    'ALTER TABLE `business_settings` ADD COLUMN `ica_threshold` BIGINT(20) NOT NULL DEFAULT 1000000 AFTER `ica_rate`;'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 2. Agregar campos ICA en Orden (solo si no existen)
SET @tablename = 'ordenes';

SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @dbname
        AND TABLE_NAME = @tablename
        AND COLUMN_NAME = 'tiene_retencion_ica'
    ) > 0,
    'SELECT "La columna tiene_retencion_ica ya existe en ordenes" AS mensaje;',
    'ALTER TABLE `ordenes` ADD COLUMN `tiene_retencion_ica` BIT(1) NOT NULL DEFAULT 0 AFTER `retencion_fuente`;'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @dbname
        AND TABLE_NAME = @tablename
        AND COLUMN_NAME = 'porcentaje_ica'
    ) > 0,
    'SELECT "La columna porcentaje_ica ya existe en ordenes" AS mensaje;',
    'ALTER TABLE `ordenes` ADD COLUMN `porcentaje_ica` DOUBLE NULL AFTER `tiene_retencion_ica`;'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @dbname
        AND TABLE_NAME = @tablename
        AND COLUMN_NAME = 'retencion_ica'
    ) > 0,
    'SELECT "La columna retencion_ica ya existe en ordenes" AS mensaje;',
    'ALTER TABLE `ordenes` ADD COLUMN `retencion_ica` DOUBLE NOT NULL DEFAULT 0.0 AFTER `porcentaje_ica`;'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 3. Agregar campo ICA en Factura (solo si no existe)
SET @tablename = 'facturas';

SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @dbname
        AND TABLE_NAME = @tablename
        AND COLUMN_NAME = 'retencion_ica'
    ) > 0,
    'SELECT "La columna retencion_ica ya existe en facturas" AS mensaje;',
    'ALTER TABLE `facturas` ADD COLUMN `retencion_ica` DOUBLE NOT NULL DEFAULT 0.0 AFTER `retencion_fuente`;'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Mensaje final
SELECT 'Script completado. Verifica los mensajes anteriores para ver qué columnas se agregaron.' AS resultado;

