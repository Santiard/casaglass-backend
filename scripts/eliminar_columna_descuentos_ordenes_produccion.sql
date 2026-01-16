-- =====================================================
-- SCRIPT SQL: ELIMINAR COLUMNA DESCUENTOS DE ÓRDENES (PRODUCCIÓN)
-- =====================================================
-- Fecha: 2025-01-XX
-- Descripción: Elimina la columna 'descuentos' de la tabla 'ordenes'
--              Incluye eliminación de restricciones CHECK que referencien la columna
-- Base de datos: MariaDB
-- =====================================================

-- ⚠️ IMPORTANTE: Hacer backup de la base de datos antes de ejecutar este script
-- ⚠️ Este script eliminará permanentemente la columna 'descuentos' y sus restricciones

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
-- PASO 2: IDENTIFICAR Y ELIMINAR RESTRICCIONES CHECK
-- =====================================================
-- Buscar restricciones CHECK que referencien la columna descuentos
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    CHECK_CLAUSE
FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ordenes'
  AND CHECK_CLAUSE LIKE '%descuentos%';

-- Si hay restricciones CHECK, eliminarlas primero
-- NOTA: MariaDB puede almacenar CHECK constraints de diferentes formas
-- Intentar eliminar si existen

-- Buscar constraints en la tabla de constraints de MariaDB
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ordenes'
  AND CONSTRAINT_TYPE = 'CHECK';

-- =====================================================
-- PASO 3: ELIMINAR RESTRICCIONES CHECK MANUALMENTE
-- =====================================================
-- Si encuentras restricciones CHECK con nombres específicos, elimínalas así:
-- ALTER TABLE ordenes DROP CONSTRAINT nombre_de_la_constraint;

-- Alternativa: Buscar en la tabla de constraints de MariaDB
-- y eliminar las que referencien descuentos

-- =====================================================
-- PASO 4: ELIMINAR LA COLUMNA DESCUENTOS
-- =====================================================
-- Intentar eliminar la columna directamente
-- Si falla, puede ser por restricciones CHECK que no se detectaron arriba
ALTER TABLE ordenes
DROP COLUMN descuentos;

-- =====================================================
-- PASO 5: VERIFICAR QUE LA COLUMNA FUE ELIMINADA
-- =====================================================
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ordenes'
ORDER BY ORDINAL_POSITION;

-- =====================================================
-- FIN DEL SCRIPT
-- =====================================================

