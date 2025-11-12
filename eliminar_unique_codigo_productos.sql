-- ============================================================
-- 🔓 SCRIPT PARA ELIMINAR LA RESTRICCIÓN UNIQUE DEL CAMPO 'codigo' EN productos
-- ============================================================
-- Base de datos: casaglassDB
-- Tabla: productos
-- Columna: codigo (actualmente tiene Key = 'UNI' - UNIQUE)
-- 
-- IMPORTANTE: Ejecuta este script paso a paso en DBeaver
-- Este script permite que múltiples productos tengan el mismo código
-- ============================================================

USE casaglassDB;

-- PASO 1: Verificar el tipo de dato actual de la columna
SHOW COLUMNS FROM productos LIKE 'codigo';
-- ⚠️ IMPORTANTE: Anota el valor de "Type" (ej: VARCHAR(255), CHAR(50), etc.)

-- PASO 2: Ver el nombre exacto del índice único
SHOW INDEX FROM productos WHERE Column_name = 'codigo';
-- ⚠️ IMPORTANTE: Anota el valor de "Key_name" - ese es el nombre del índice que hay que eliminar

-- PASO 3: Eliminar el índice único
-- Reemplaza 'NOMBRE_DEL_INDICE' con el nombre que encontraste en el PASO 2
-- Ejemplos comunes:
-- ALTER TABLE productos DROP INDEX codigo;
-- ALTER TABLE productos DROP INDEX UK_codigo;
-- ALTER TABLE productos DROP INDEX productos_codigo_unique;

-- ⚠️ EJECUTA ESTE COMANDO CON EL NOMBRE REAL DEL ÍNDICE:
-- ALTER TABLE productos DROP INDEX NOMBRE_DEL_INDICE;

-- PASO 4: Verificar que el índice se eliminó
SHOW INDEX FROM productos WHERE Column_name = 'codigo';
-- No debería aparecer ningún índice

-- PASO 5: Modificar la columna para asegurar que no tenga restricción UNIQUE
-- ⚠️ IMPORTANTE: Reemplaza VARCHAR(255) con el tipo que viste en el PASO 1
-- Si tu columna es CHAR(50), usa: ALTER TABLE productos MODIFY COLUMN codigo CHAR(50) NOT NULL;
-- Si tu columna es VARCHAR(100), usa: ALTER TABLE productos MODIFY COLUMN codigo VARCHAR(100) NOT NULL;
ALTER TABLE productos MODIFY COLUMN codigo VARCHAR(255) NOT NULL;

-- PASO 6: Verificar que la restricción se eliminó correctamente
SHOW COLUMNS FROM productos LIKE 'codigo';
-- La columna 'codigo' NO debe tener 'Key' = 'UNI' (UNIQUE)
-- Debe mostrar Key = '' (vacío) o NULL

-- ============================================================
-- ✅ FIN DEL SCRIPT
-- ============================================================
-- Si después de estos pasos sigue apareciendo UNI, puede ser que:
-- 1. El nombre del índice sea diferente - ejecuta el PASO 2 de nuevo
-- 2. Haya múltiples índices - elimínalos todos
-- 3. Necesites reiniciar la conexión a la base de datos
-- ============================================================
