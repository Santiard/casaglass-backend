-- ============================================
-- Script para eliminar 'NATURAL' del enum color en productos
-- Base de datos: MariaDB
-- Tabla: productos
-- Columna: color
-- ============================================

-- Paso 1: Verificar el estado actual del enum
SHOW COLUMNS FROM productos LIKE 'color';

-- Paso 2: Verificar si hay productos con color = 'NATURAL'
SELECT COUNT(*) as productos_con_natural 
FROM productos 
WHERE color = 'NATURAL';

-- Paso 3: Actualizar productos que tienen color = 'NATURAL' a otro valor válido
-- IMPORTANTE: Elige el valor de reemplazo según tu lógica de negocio
-- Opciones: 'NA', 'MATE', 'BLANCO', 'NEGRO', 'BRONCE'
-- En este ejemplo, los actualizamos a 'NA' (No Aplica)
UPDATE productos 
SET color = 'NA' 
WHERE color = 'NATURAL';

-- Paso 4: Verificar que no queden productos con NATURAL
SELECT COUNT(*) as productos_con_natural_restantes 
FROM productos 
WHERE color = 'NATURAL';

-- Paso 5: Modificar la columna para eliminar 'NATURAL' del enum
-- IMPORTANTE: En MariaDB, para modificar un enum, debes especificar TODOS los valores que quieres mantener
ALTER TABLE productos 
MODIFY COLUMN color ENUM('MATE', 'BLANCO', 'NEGRO', 'BRONCE', 'NA') NULL;

-- Paso 6: Verificar que el cambio se aplicó correctamente
SHOW COLUMNS FROM productos LIKE 'color';

-- ============================================
-- NOTAS:
-- - Si tienes muchos productos con NATURAL y quieres cambiarlos a otro valor específico,
--   modifica el UPDATE en el Paso 3 antes de ejecutarlo.
-- 
-- - Si la columna tiene valores NULL, no hay problema, el enum permite NULL.
-- 
-- - Si hay algún error al ejecutar el ALTER TABLE, verifica que:
--   1. No queden productos con color = 'NATURAL' (Paso 4 debe retornar 0)
--   2. Todos los valores del nuevo enum sean válidos para tu aplicación
-- ============================================

