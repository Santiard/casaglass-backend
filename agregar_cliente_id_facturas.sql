-- ============================================================================
-- SCRIPT SQL: AGREGAR COLUMNA "cliente_id" A facturas
-- ============================================================================
-- FECHA: 2025-01-XX
-- DESCRIPCIÓN: Agrega la columna "cliente_id" a la tabla facturas para permitir
--              facturar a un cliente diferente al de la orden asociada.
-- ============================================================================
-- IMPORTANTE: 
-- 1. Hacer BACKUP de la base de datos antes de ejecutar
-- 2. Ejecutar este script en orden
-- 3. Verificar los resultados al final
-- ============================================================================

-- ============================================================================
-- PASO 1: VERIFICAR SI LA COLUMNA YA EXISTE
-- ============================================================================

SET @col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'facturas'
    AND COLUMN_NAME = 'cliente_id'
);

-- ============================================================================
-- PASO 2: AGREGAR COLUMNA "cliente_id" SI NO EXISTE
-- ============================================================================

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE facturas ADD COLUMN cliente_id BIGINT NULL AFTER orden_id',
    'SELECT "La columna cliente_id ya existe en la tabla facturas" AS mensaje'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================================
-- PASO 3: AGREGAR FOREIGN KEY CONSTRAINT
-- ============================================================================

-- Verificar si el constraint ya existe
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'facturas'
    AND CONSTRAINT_NAME = 'fk_factura_cliente'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

-- Agregar foreign key si no existe
SET @sql_fk = IF(@fk_exists = 0,
    'ALTER TABLE facturas ADD CONSTRAINT fk_factura_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE SET NULL',
    'SELECT "El constraint fk_factura_cliente ya existe" AS mensaje'
);

PREPARE stmt FROM @sql_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================================
-- PASO 4: AGREGAR ÍNDICE PARA MEJORAR RENDIMIENTO
-- ============================================================================

-- Verificar si el índice ya existe
SET @idx_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'facturas'
    AND INDEX_NAME = 'idx_factura_cliente'
);

-- Agregar índice si no existe
SET @sql_idx = IF(@idx_exists = 0,
    'CREATE INDEX idx_factura_cliente ON facturas(cliente_id)',
    'SELECT "El índice idx_factura_cliente ya existe" AS mensaje'
);

PREPARE stmt FROM @sql_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================================
-- VERIFICACIÓN FINAL
-- ============================================================================

-- Verificar que la columna fue agregada
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '✅ Columna cliente_id agregada correctamente'
        ELSE '❌ ERROR: La columna cliente_id no existe'
    END AS resultado_columna
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'facturas'
AND COLUMN_NAME = 'cliente_id';

-- Verificar que el foreign key fue agregado
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '✅ Foreign key fk_factura_cliente agregado correctamente'
        ELSE '❌ ERROR: El foreign key fk_factura_cliente no existe'
    END AS resultado_fk
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'facturas'
AND CONSTRAINT_NAME = 'fk_factura_cliente'
AND CONSTRAINT_TYPE = 'FOREIGN KEY';

-- Verificar que el índice fue agregado
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '✅ Índice idx_factura_cliente agregado correctamente'
        ELSE '❌ ERROR: El índice idx_factura_cliente no existe'
    END AS resultado_idx
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'facturas'
AND INDEX_NAME = 'idx_factura_cliente';

-- Mostrar estructura actualizada de la tabla
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'facturas'
ORDER BY ORDINAL_POSITION;

-- ============================================================================
-- RESUMEN DE CAMBIOS
-- ============================================================================

SELECT 
    'RESUMEN DE CAMBIOS APLICADOS' AS titulo,
    '✅ cliente_id: AGREGADO (opcional, permite NULL)' AS cambio_1,
    '✅ Foreign key: fk_factura_cliente agregado' AS cambio_2,
    '✅ Índice: idx_factura_cliente agregado' AS cambio_3,
    '✅ Estructura lista para facturar a cliente diferente' AS estado;

-- ============================================================================
-- FIN DEL SCRIPT
-- ============================================================================
-- Si todo salió bien, deberías ver:
-- ✅ Columna cliente_id agregada correctamente
-- ✅ Foreign key fk_factura_cliente agregado correctamente
-- ✅ Índice idx_factura_cliente agregado correctamente
-- ✅ Estructura de la tabla actualizada
-- ============================================================================

