-- ============================================================================
-- SCRIPT PARA ELIMINAR COLUMNA monto_gastos DE entregas_dinero
-- ============================================================================
-- Este script elimina la columna monto_gastos que ya no se usa
-- ============================================================================

-- Eliminar la columna monto_gastos de la tabla entregas_dinero
ALTER TABLE entregas_dinero DROP COLUMN monto_gastos;

-- Verificar que la columna fue eliminada
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ Columna monto_gastos eliminada correctamente'
        ELSE '❌ ERROR: La columna monto_gastos aún existe'
    END AS resultado
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'entregas_dinero' 
AND COLUMN_NAME = 'monto_gastos';

-- Mostrar la estructura actualizada de entregas_dinero
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'entregas_dinero'
ORDER BY ORDINAL_POSITION;

