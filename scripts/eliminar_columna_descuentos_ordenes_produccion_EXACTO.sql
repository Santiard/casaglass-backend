-- =====================================================
-- SCRIPT SQL: ELIMINAR COLUMNA DESCUENTOS DE ÓRDENES (PRODUCCIÓN)
-- =====================================================
-- Basado en la estructura real de la tabla ordenes
-- Fecha: 2025-01-XX
-- =====================================================

USE casaglassDB;

-- =====================================================
-- PASO 1: VERIFICAR QUE LA COLUMNA EXISTE
-- =====================================================
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ordenes'
  AND COLUMN_NAME = 'descuentos';

-- =====================================================
-- PASO 2: ELIMINAR LA RESTRICCIÓN CHECK QUE REFERENCIA DESCUENTOS
-- =====================================================
-- La restricción actual es:
-- CHECK (`credito` = 1 or `monto_efectivo` = 0 and `monto_transferencia` = 0 and `monto_cheque` = 0 or `monto_efectivo` + `monto_transferencia` + `monto_cheque` = `total` - `descuentos`)
-- Necesitamos eliminarla primero

ALTER TABLE ordenes
DROP CONSTRAINT check_suma_metodos_pago_orden;

-- Si el comando anterior falla, intentar:
-- ALTER TABLE ordenes DROP CHECK check_suma_metodos_pago_orden;

-- =====================================================
-- PASO 3: ELIMINAR LA COLUMNA DESCUENTOS
-- =====================================================
ALTER TABLE ordenes
DROP COLUMN descuentos;

-- =====================================================
-- PASO 4: RECREAR LA RESTRICCIÓN CHECK SIN DESCUENTOS
-- =====================================================
-- Nueva restricción: monto_efectivo + monto_transferencia + monto_cheque = total
-- (sin restar descuentos)
-- 
-- La restricción original era:
-- CHECK (`credito` = 1 or `monto_efectivo` = 0 and `monto_transferencia` = 0 and `monto_cheque` = 0 or `monto_efectivo` + `monto_transferencia` + `monto_cheque` = `total` - `descuentos`)
-- 
-- La nueva restricción es:
-- CHECK (`credito` = 1 or `monto_efectivo` = 0 and `monto_transferencia` = 0 and `monto_cheque` = 0 or `monto_efectivo` + `monto_transferencia` + `monto_cheque` = `total`)

ALTER TABLE ordenes
ADD CONSTRAINT check_suma_metodos_pago_orden 
CHECK (`credito` = 1 or `monto_efectivo` = 0 and `monto_transferencia` = 0 and `monto_cheque` = 0 or `monto_efectivo` + `monto_transferencia` + `monto_cheque` = `total`);

-- =====================================================
-- PASO 5: VERIFICAR QUE LA COLUMNA FUE ELIMINADA
-- =====================================================
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ordenes'
ORDER BY ORDINAL_POSITION;

-- =====================================================
-- PASO 6: VERIFICAR QUE LA RESTRICCIÓN FUE RECREADA CORRECTAMENTE
-- =====================================================
SHOW CREATE TABLE ordenes;

-- =====================================================
-- FIN DEL SCRIPT
-- =====================================================

