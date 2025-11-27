-- ============================================================================
-- SCRIPT SQL: SIMPLIFICACIÓN COMPLETA DE entregas_dinero
-- ============================================================================
-- FECHA: 2025-01-XX
-- DESCRIPCIÓN: Elimina columnas que ya no se utilizan en la lógica simplificada:
--              1. diferencia
--              2. fecha_desde
--              3. fecha_hasta
-- ============================================================================
-- IMPORTANTE: 
-- 1. Hacer BACKUP de la base de datos antes de ejecutar
-- 2. Ejecutar este script en orden
-- 3. Verificar los resultados al final
-- ============================================================================

-- ============================================================================
-- PASO 1: ELIMINAR COLUMNA "diferencia"
-- ============================================================================

SET @col_diferencia_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'entregas_dinero'
    AND COLUMN_NAME = 'diferencia'
);

SET @sql_diferencia = IF(@col_diferencia_exists > 0,
    'ALTER TABLE entregas_dinero DROP COLUMN diferencia',
    'SELECT "La columna diferencia no existe en la tabla entregas_dinero" AS mensaje'
);

PREPARE stmt FROM @sql_diferencia;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================================
-- PASO 2: ELIMINAR COLUMNA "fecha_desde"
-- ============================================================================

SET @col_fecha_desde_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'entregas_dinero'
    AND COLUMN_NAME = 'fecha_desde'
);

SET @sql_fecha_desde = IF(@col_fecha_desde_exists > 0,
    'ALTER TABLE entregas_dinero DROP COLUMN fecha_desde',
    'SELECT "La columna fecha_desde no existe en la tabla entregas_dinero" AS mensaje'
);

PREPARE stmt FROM @sql_fecha_desde;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================================
-- PASO 3: ELIMINAR COLUMNA "fecha_hasta"
-- ============================================================================

SET @col_fecha_hasta_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'entregas_dinero'
    AND COLUMN_NAME = 'fecha_hasta'
);

SET @sql_fecha_hasta = IF(@col_fecha_hasta_exists > 0,
    'ALTER TABLE entregas_dinero DROP COLUMN fecha_hasta',
    'SELECT "La columna fecha_hasta no existe en la tabla entregas_dinero" AS mensaje'
);

PREPARE stmt FROM @sql_fecha_hasta;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================================
-- VERIFICACIÓN FINAL
-- ============================================================================

-- Verificar que la columna diferencia fue eliminada
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ Columna diferencia eliminada correctamente'
        ELSE '❌ ERROR: La columna diferencia aún existe'
    END AS resultado_diferencia
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'entregas_dinero'
AND COLUMN_NAME = 'diferencia';

-- Verificar que la columna fecha_desde fue eliminada
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ Columna fecha_desde eliminada correctamente'
        ELSE '❌ ERROR: La columna fecha_desde aún existe'
    END AS resultado_fecha_desde
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'entregas_dinero'
AND COLUMN_NAME = 'fecha_desde';

-- Verificar que la columna fecha_hasta fue eliminada
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ Columna fecha_hasta eliminada correctamente'
        ELSE '❌ ERROR: La columna fecha_hasta aún existe'
    END AS resultado_fecha_hasta
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'entregas_dinero'
AND COLUMN_NAME = 'fecha_hasta';

-- ============================================================================
-- MOSTRAR ESTRUCTURA ACTUALIZADA DE LA TABLA
-- ============================================================================

SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'entregas_dinero'
ORDER BY ORDINAL_POSITION;

-- ============================================================================
-- RESUMEN DE CAMBIOS
-- ============================================================================

SELECT 
    'RESUMEN DE CAMBIOS APLICADOS' AS titulo,
    '✅ diferencia: ELIMINADA' AS cambio_1,
    '✅ fecha_desde: ELIMINADA' AS cambio_2,
    '✅ fecha_hasta: ELIMINADA' AS cambio_3,
    '✅ Estructura simplificada lista para usar' AS estado;

-- ============================================================================
-- FIN DEL SCRIPT
-- ============================================================================
-- Si todo salió bien, deberías ver:
-- ✅ Columna diferencia eliminada correctamente
-- ✅ Columna fecha_desde eliminada correctamente
-- ✅ Columna fecha_hasta eliminada correctamente
-- ✅ Estructura de la tabla actualizada
-- ============================================================================

