-- =====================================================
-- SCRIPT SQL: ELIMINAR COLUMNA DESCUENTOS DE ÓRDENES
-- =====================================================
-- Fecha: 2025-01-XX
-- Descripción: Elimina la columna 'descuentos' de la tabla 'ordenes'
-- Base de datos: MariaDB
-- =====================================================

-- ⚠️ IMPORTANTE: Hacer backup de la base de datos antes de ejecutar este script
-- ⚠️ Este script eliminará permanentemente la columna 'descuentos' de la tabla 'ordenes'

USE tu_base_de_datos; -- ⚠️ CAMBIAR POR EL NOMBRE DE TU BASE DE DATOS

-- Verificar que la columna existe antes de eliminarla
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ordenes'
  AND COLUMN_NAME = 'descuentos';

-- Eliminar la columna descuentos de la tabla ordenes
ALTER TABLE ordenes
DROP COLUMN descuentos;

-- Verificar que la columna fue eliminada
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ordenes'
ORDER BY ORDINAL_POSITION;

-- =====================================================
-- FIN DEL SCRIPT
-- =====================================================

