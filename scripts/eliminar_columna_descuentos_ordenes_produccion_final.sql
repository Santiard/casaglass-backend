-- =====================================================
-- SCRIPT SQL: ELIMINAR COLUMNA DESCUENTOS DE ÓRDENES (PRODUCCIÓN)
-- =====================================================
-- Fecha: 2025-01-XX
-- Descripción: Elimina la columna 'descuentos' de la tabla 'ordenes'
--              PRIMERO elimina la restricción CHECK que la referencia
-- Base de datos: MariaDB
-- =====================================================

-- ⚠️ IMPORTANTE: Hacer backup de la base de datos antes de ejecutar este script
-- ⚠️ Este script eliminará permanentemente la columna 'descuentos' y su restricción CHECK

USE casaglassDB; -- ⚠️ CAMBIAR POR EL NOMBRE DE TU BASE DE DATOS DE PRODUCCIÓN

-- =====================================================
-- PASO 1: VERIFICAR QUE LA COLUMNA EXISTE
-- =====================================================
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ordenes'
  AND COLUMN_NAME = 'descuentos';

-- =====================================================
-- PASO 2: VER LA DEFINICIÓN COMPLETA DE LA TABLA
-- =====================================================
-- Esto mostrará todas las restricciones CHECK que referencian descuentos
SHOW CREATE TABLE ordenes;

-- =====================================================
-- PASO 3: IDENTIFICAR LA RESTRICCIÓN CHECK
-- =====================================================
-- Buscar restricciones CHECK en la tabla ordenes
SELECT 
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE,
    TABLE_NAME
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ordenes'
  AND CONSTRAINT_TYPE = 'CHECK';

-- =====================================================
-- PASO 4: ELIMINAR LA RESTRICCIÓN CHECK QUE REFERENCIA DESCUENTOS
-- =====================================================
-- Según la documentación, la restricción se llama: check_suma_metodos_pago_orden
-- Esta restricción verifica: monto_efectivo + monto_transferencia + monto_cheque = total - descuentos
-- Necesitamos eliminarla primero antes de eliminar la columna

-- Eliminar la restricción CHECK (si existe)
ALTER TABLE ordenes
DROP CONSTRAINT IF EXISTS check_suma_metodos_pago_orden;

-- Si el comando anterior falla, intentar con el nombre exacto que aparece en SHOW CREATE TABLE
-- Ejemplo alternativo si el nombre es diferente:
-- ALTER TABLE ordenes DROP CONSTRAINT nombre_exacto_de_la_constraint;

-- =====================================================
-- PASO 5: VERIFICAR QUE LA RESTRICCIÓN FUE ELIMINADA
-- =====================================================
SELECT 
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE,
    TABLE_NAME
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ordenes'
  AND CONSTRAINT_TYPE = 'CHECK';

-- =====================================================
-- PASO 6: ELIMINAR LA COLUMNA DESCUENTOS
-- =====================================================
-- Ahora que la restricción CHECK fue eliminada, podemos eliminar la columna
ALTER TABLE ordenes
DROP COLUMN descuentos;

-- =====================================================
-- PASO 7: RECREAR LA RESTRICCIÓN CHECK SIN DESCUENTOS
-- =====================================================
-- Recrear la restricción CHECK pero SIN la referencia a descuentos
-- Nueva fórmula: monto_efectivo + monto_transferencia + monto_cheque = total
ALTER TABLE ordenes
ADD CONSTRAINT check_suma_metodos_pago_orden 
CHECK (
    credito = true OR 
    monto_efectivo + monto_transferencia + monto_cheque = total OR
    (monto_efectivo = 0 AND monto_transferencia = 0 AND monto_cheque = 0)
);

-- =====================================================
-- PASO 8: VERIFICAR QUE LA COLUMNA FUE ELIMINADA
-- =====================================================
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ordenes'
ORDER BY ORDINAL_POSITION;

-- =====================================================
-- PASO 9: VERIFICAR QUE LA RESTRICCIÓN FUE RECREADA
-- =====================================================
SHOW CREATE TABLE ordenes;

-- =====================================================
-- FIN DEL SCRIPT
-- =====================================================

