-- ============================================
-- Script para agregar "MT" al enum tipo en productos
-- Base de datos: MariaDB
-- Tabla: productos
-- Columna: tipo
-- ============================================

-- Paso 1: Verificar el estado actual del enum
SHOW COLUMNS FROM productos LIKE 'tipo';

-- Paso 2: Modificar la columna para agregar 'MT' al enum
-- IMPORTANTE: En MariaDB, para modificar un enum, debes especificar TODOS los valores
ALTER TABLE productos 
MODIFY COLUMN tipo ENUM('UNID', 'PERFIL', 'MT') NULL;

-- Paso 3: Verificar que el cambio se aplicó correctamente
SHOW COLUMNS FROM productos LIKE 'tipo';

-- ============================================
-- NOTAS:
-- - Si la columna tiene valores existentes que no están en el nuevo enum, 
--   MariaDB puede rechazar el cambio. En ese caso, primero actualiza los valores
--   existentes a uno válido (por ejemplo, 'UNID' o 'PERFIL').
-- 
-- - Si hay productos con tipo = NULL, no hay problema, el enum permite NULL.
-- ============================================

