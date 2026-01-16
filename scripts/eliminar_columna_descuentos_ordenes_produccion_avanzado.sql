-- =====================================================
-- SCRIPT SQL AVANZADO: ELIMINAR COLUMNA DESCUENTOS DE ÓRDENES
-- =====================================================
-- Para casos donde hay restricciones CHECK problemáticas
-- Base de datos: MariaDB
-- =====================================================

USE casaglassDB; -- ⚠️ CAMBIAR POR EL NOMBRE DE TU BASE DE DATOS

-- =====================================================
-- PASO 1: VERIFICAR ESTRUCTURA ACTUAL
-- =====================================================
-- Ver todas las columnas de la tabla ordenes
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ordenes'
ORDER BY ORDINAL_POSITION;

-- =====================================================
-- PASO 2: BUSCAR TODAS LAS RESTRICCIONES DE LA TABLA
-- =====================================================
-- Ver todas las restricciones de la tabla ordenes
SELECT 
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE,
    TABLE_NAME
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ordenes';

-- =====================================================
-- PASO 3: BUSCAR RESTRICCIONES CHECK ESPECÍFICAS
-- =====================================================
-- En MariaDB, las restricciones CHECK pueden estar en diferentes lugares
-- Intentar buscar en INFORMATION_SCHEMA.CHECK_CONSTRAINTS (si está disponible)
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    CHECK_CLAUSE
FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ordenes';

-- =====================================================
-- PASO 4: VER LA DEFINICIÓN COMPLETA DE LA TABLA
-- =====================================================
-- Esto mostrará todas las restricciones incluidas en la definición de la tabla
SHOW CREATE TABLE ordenes;

-- =====================================================
-- PASO 5: SOLUCIÓN ALTERNATIVA - RECREAR LA TABLA SIN LA COLUMNA
-- =====================================================
-- Si hay restricciones CHECK problemáticas, puede ser necesario:
-- 1. Crear una tabla temporal con la estructura correcta
-- 2. Copiar los datos
-- 3. Eliminar la tabla original
-- 4. Renombrar la temporal

-- ⚠️ ADVERTENCIA: Este proceso requiere más cuidado y puede requerir
-- eliminar foreign keys temporalmente

-- =====================================================
-- PASO 6: INTENTAR ELIMINAR CON IF EXISTS (si MariaDB lo soporta)
-- =====================================================
-- MariaDB 10.2.7+ soporta IF EXISTS para DROP COLUMN
-- ALTER TABLE ordenes DROP COLUMN IF EXISTS descuentos;

-- =====================================================
-- PASO 7: ELIMINAR COLUMNA CON MANEJO DE ERRORES
-- =====================================================
-- Si el error persiste, puede ser necesario eliminar la restricción CHECK primero
-- Ejecuta SHOW CREATE TABLE ordenes y busca restricciones CHECK que mencionen descuentos
-- Luego elimínalas manualmente con:
-- ALTER TABLE ordenes DROP CONSTRAINT nombre_constraint;

ALTER TABLE ordenes
DROP COLUMN descuentos;

-- =====================================================
-- FIN DEL SCRIPT
-- =====================================================

