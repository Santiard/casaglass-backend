-- ============================================================================
-- SCRIPT SQL PARA ACTUALIZAR ENTREGAS_DINERO - SIMPLIFICACIÓN DE MONTO
-- ============================================================================
-- FECHA: 2025-01-XX
-- DESCRIPCIÓN: Elimina monto_esperado y monto_entregado, los reemplaza por 
--              un único campo "monto". También elimina el campo "diferencia".
-- ============================================================================
-- IMPORTANTE: 
-- 1. Hacer BACKUP de la base de datos antes de ejecutar
-- 2. Ejecutar este script en orden
-- 3. Verificar los resultados al final
-- ============================================================================

-- ============================================================================
-- PASO 1: CREAR COLUMNA TEMPORAL "monto" CON EL VALOR CORRECTO
-- ============================================================================
-- Usar monto_entregado si existe, sino usar monto_esperado

ALTER TABLE entregas_dinero 
ADD COLUMN monto_temp DOUBLE NULL;

UPDATE entregas_dinero 
SET monto_temp = COALESCE(monto_entregado, monto_esperado, 0.0);

-- ============================================================================
-- PASO 2: ELIMINAR COLUMNAS ANTIGUAS
-- ============================================================================

ALTER TABLE entregas_dinero DROP COLUMN IF EXISTS monto_esperado;
ALTER TABLE entregas_dinero DROP COLUMN IF EXISTS monto_entregado;
ALTER TABLE entregas_dinero DROP COLUMN IF EXISTS diferencia;

-- ============================================================================
-- PASO 3: RENOMBRAR COLUMNA TEMPORAL A "monto" Y HACERLA NOT NULL
-- ============================================================================

ALTER TABLE entregas_dinero 
CHANGE COLUMN monto_temp monto DOUBLE NOT NULL DEFAULT 0.0;

-- ============================================================================
-- PASO 4: ACTUALIZAR EL MONTO DESDE EL DESGLOSE SI ES NECESARIO
-- ============================================================================
-- Asegurar que el monto coincida con la suma del desglose

UPDATE entregas_dinero 
SET monto = COALESCE(monto_efectivo, 0.0) + 
            COALESCE(monto_transferencia, 0.0) + 
            COALESCE(monto_cheque, 0.0) + 
            COALESCE(monto_deposito, 0.0)
WHERE monto = 0.0 
   OR ABS(monto - (COALESCE(monto_efectivo, 0.0) + 
                   COALESCE(monto_transferencia, 0.0) + 
                   COALESCE(monto_cheque, 0.0) + 
                   COALESCE(monto_deposito, 0.0))) > 0.01;

-- ============================================================================
-- VERIFICACIÓN FINAL
-- ============================================================================

-- Verificar que las columnas fueron eliminadas
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ Columna monto_esperado eliminada correctamente'
        ELSE '❌ ERROR: La columna monto_esperado aún existe'
    END AS resultado
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'entregas_dinero' 
AND COLUMN_NAME = 'monto_esperado';

SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ Columna monto_entregado eliminada correctamente'
        ELSE '❌ ERROR: La columna monto_entregado aún existe'
    END AS resultado
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'entregas_dinero' 
AND COLUMN_NAME = 'monto_entregado';

SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ Columna diferencia eliminada correctamente'
        ELSE '❌ ERROR: La columna diferencia aún existe'
    END AS resultado
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'entregas_dinero' 
AND COLUMN_NAME = 'diferencia';

-- Verificar que la columna monto existe
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '✅ Columna monto creada correctamente'
        ELSE '❌ ERROR: La columna monto no existe'
    END AS resultado
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'entregas_dinero' 
AND COLUMN_NAME = 'monto';

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

-- Mostrar resumen de entregas actualizadas
SELECT 
    COUNT(*) AS total_entregas,
    SUM(CASE WHEN monto > 0 THEN 1 ELSE 0 END) AS entregas_con_monto,
    SUM(CASE WHEN ABS(monto - (COALESCE(monto_efectivo, 0) + 
                               COALESCE(monto_transferencia, 0) + 
                               COALESCE(monto_cheque, 0) + 
                               COALESCE(monto_deposito, 0))) < 0.01 THEN 1 ELSE 0 END) AS entregas_con_monto_coincidente
FROM entregas_dinero;

-- ============================================================================
-- FIN DEL SCRIPT
-- ============================================================================
-- Si todo salió bien, deberías ver:
-- ✅ Columna monto_esperado eliminada correctamente
-- ✅ Columna monto_entregado eliminada correctamente
-- ✅ Columna diferencia eliminada correctamente
-- ✅ Columna monto creada correctamente
-- ============================================================================

