-- =====================================================
-- SCRIPT: ELIMINAR COLUMNAS "fecha_desde" Y "fecha_hasta" DE entregas_dinero
-- =====================================================
-- Descripción: Elimina las columnas "fecha_desde" y "fecha_hasta" de la tabla entregas_dinero
--              ya que ya no se utilizan en la lógica simplificada de entregas.
-- Fecha: 2024
-- =====================================================

-- Verificar si las columnas existen antes de eliminarlas
SET @col_fecha_desde_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'entregas_dinero'
    AND COLUMN_NAME = 'fecha_desde'
);

SET @col_fecha_hasta_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'entregas_dinero'
    AND COLUMN_NAME = 'fecha_hasta'
);

-- Eliminar fecha_desde si existe
SET @sql_fecha_desde = IF(@col_fecha_desde_exists > 0,
    'ALTER TABLE entregas_dinero DROP COLUMN fecha_desde',
    'SELECT "La columna fecha_desde no existe en la tabla entregas_dinero" AS mensaje'
);

PREPARE stmt FROM @sql_fecha_desde;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Eliminar fecha_hasta si existe
SET @sql_fecha_hasta = IF(@col_fecha_hasta_exists > 0,
    'ALTER TABLE entregas_dinero DROP COLUMN fecha_hasta',
    'SELECT "La columna fecha_hasta no existe en la tabla entregas_dinero" AS mensaje'
);

PREPARE stmt FROM @sql_fecha_hasta;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verificar que las columnas fueron eliminadas
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ Columna fecha_desde eliminada correctamente'
        ELSE '❌ ERROR: La columna fecha_desde aún existe'
    END AS resultado_fecha_desde
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'entregas_dinero'
AND COLUMN_NAME = 'fecha_desde';

SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ Columna fecha_hasta eliminada correctamente'
        ELSE '❌ ERROR: La columna fecha_hasta aún existe'
    END AS resultado_fecha_hasta
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'entregas_dinero'
AND COLUMN_NAME = 'fecha_hasta';

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

