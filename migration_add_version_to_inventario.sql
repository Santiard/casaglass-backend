-- ============================================================================
-- MIGRACIÓN: Agregar campo version para lock optimista en tabla inventario
-- ============================================================================
-- Fecha: 2025-12-23
-- Propósito: Cambiar de lock pesimista (PESSIMISTIC_WRITE) a lock optimista (@Version)
-- 
-- IMPORTANTE: 
-- - Esta migración es segura y NO requiere downtime
-- - El campo version empieza en 0 para todos los registros existentes
-- - Hibernate incrementará automáticamente el campo en cada UPDATE
-- ============================================================================

USE casaglass_db;  -- Ajustar nombre de BD si es diferente

-- Agregar columna version con valor inicial 0
ALTER TABLE inventario 
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Verificar que se agregó correctamente
SELECT 
    TABLE_NAME,
    COLUMN_NAME, 
    DATA_TYPE, 
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM 
    INFORMATION_SCHEMA.COLUMNS 
WHERE 
    TABLE_NAME = 'inventario' 
    AND COLUMN_NAME = 'version';

-- Verificar registros existentes (todos deben tener version = 0)
SELECT 
    COUNT(*) as total_registros,
    MAX(version) as version_maxima,
    MIN(version) as version_minima
FROM 
    inventario;

-- ============================================================================
-- ROLLBACK (si necesitas deshacer la migración):
-- ============================================================================
-- ALTER TABLE inventario DROP COLUMN version;
-- ============================================================================
