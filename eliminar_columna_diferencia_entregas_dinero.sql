-- =====================================================
-- SCRIPT: ELIMINAR COLUMNA "diferencia" DE entregas_dinero
-- =====================================================
-- Descripción: Elimina la columna "diferencia" de la tabla entregas_dinero
--              ya que ya no se utiliza en la lógica simplificada de entregas.
-- Fecha: 2024
-- =====================================================

-- Verificar si la columna existe antes de eliminarla
SET @col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'entregas_dinero'
    AND COLUMN_NAME = 'diferencia'
);

-- Eliminar la columna si existe
SET @sql = IF(@col_exists > 0,
    'ALTER TABLE entregas_dinero DROP COLUMN diferencia',
    'SELECT "La columna diferencia no existe en la tabla entregas_dinero" AS mensaje'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verificar que la columna fue eliminada
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ Columna diferencia eliminada correctamente'
        ELSE '❌ ERROR: La columna diferencia aún existe'
    END AS resultado
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'entregas_dinero'
AND COLUMN_NAME = 'diferencia';

-- Mostrar estructura actualizada de la tabla
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'entregas_dinero'
ORDER BY ORDINAL_POSITION;

