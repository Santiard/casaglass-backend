-- ============================================
-- Script para actualizar ProductoVidrio: m1m2 y laminas -> m1 y m2
-- Base de datos: MariaDB
-- Tabla: productos_vidrio
-- Para ejecutar en DBeaver
-- ============================================

-- Paso 1: Verificar el estado actual de la tabla
SHOW COLUMNS FROM productos_vidrio;

-- Paso 2: Agregar las nuevas columnas m1 y m2 (temporalmente NULL para permitir migración)
ALTER TABLE productos_vidrio 
ADD COLUMN m1 DOUBLE NULL AFTER mm,
ADD COLUMN m2 DOUBLE NULL AFTER m1;

-- Paso 3: Migrar datos existentes (si tienes datos)
-- OPCIÓN A: Si m1m2 contenía un solo valor, copiarlo a ambas columnas
-- UPDATE productos_vidrio SET m1 = m1m2, m2 = m1m2 WHERE m1m2 IS NOT NULL;

-- OPCIÓN B: Si m1m2 era m² (metros cuadrados), calcular m1 y m2 como raíz cuadrada
-- UPDATE productos_vidrio SET m1 = SQRT(m1m2), m2 = SQRT(m1m2) WHERE m1m2 IS NOT NULL;

-- OPCIÓN C: Si no tienes datos o quieres valores por defecto
-- UPDATE productos_vidrio SET m1 = 0.0, m2 = 0.0 WHERE m1 IS NULL OR m2 IS NULL;

-- NOTA: Descomenta y ajusta la opción que necesites según tu lógica de negocio.
-- Si no tienes datos, puedes omitir este paso.

-- Paso 4: Hacer las columnas NOT NULL (solo después de migrar todos los datos)
ALTER TABLE productos_vidrio 
MODIFY COLUMN m1 DOUBLE NOT NULL,
MODIFY COLUMN m2 DOUBLE NOT NULL;

-- Paso 5: Eliminar las columnas antiguas
ALTER TABLE productos_vidrio 
DROP COLUMN m1m2,
DROP COLUMN laminas;

-- Paso 6: Verificar que el cambio se aplicó correctamente
SHOW COLUMNS FROM productos_vidrio;

-- ============================================
-- RESUMEN DE CAMBIOS:
-- ✅ Agregadas columnas: m1 (DOUBLE NOT NULL), m2 (DOUBLE NOT NULL)
-- ❌ Eliminadas columnas: m1m2, laminas
-- ============================================
