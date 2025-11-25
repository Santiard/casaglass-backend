-- ============================================================================
-- SCRIPT SQL PARA ELIMINAR COLUMNAS DE entregas_dinero
-- ============================================================================
-- FECHA: 2025-01-XX
-- DESCRIPCIÓN: Elimina las columnas observaciones y numero_comprobante de 
--              la tabla entregas_dinero
-- ============================================================================
-- IMPORTANTE: 
-- 1. Hacer BACKUP de la base de datos antes de ejecutar
-- 2. Ejecutar este script en orden
-- 3. Verificar los resultados al final
-- ============================================================================

-- ============================================================================
-- PASO 1: ELIMINAR COLUMNA observaciones
-- ============================================================================

ALTER TABLE entregas_dinero DROP COLUMN IF EXISTS observaciones;

-- ============================================================================
-- PASO 2: ELIMINAR COLUMNA numero_comprobante
-- ============================================================================

ALTER TABLE entregas_dinero DROP COLUMN IF EXISTS numero_comprobante;

-- ============================================================================
-- VERIFICACIÓN FINAL
-- ============================================================================

-- Verificar que la columna observaciones fue eliminada
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ Columna observaciones eliminada correctamente'
        ELSE '❌ ERROR: La columna observaciones aún existe'
    END AS resultado
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'entregas_dinero' 
AND COLUMN_NAME = 'observaciones';

-- Verificar que la columna numero_comprobante fue eliminada
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ Columna numero_comprobante eliminada correctamente'
        ELSE '❌ ERROR: La columna numero_comprobante aún existe'
    END AS resultado
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'entregas_dinero' 
AND COLUMN_NAME = 'numero_comprobante';

-- Mostrar estructura actualizada de entregas_dinero
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'entregas_dinero'
ORDER BY ORDINAL_POSITION;

-- ============================================================================
-- FIN DEL SCRIPT
-- ============================================================================
-- Si todo salió bien, deberías ver:
-- ✅ Columna observaciones eliminada correctamente
-- ✅ Columna numero_comprobante eliminada correctamente
-- ============================================================================

