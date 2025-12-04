-- =====================================================
-- Script SQL: Agregar campo tieneRetencionFuente a Orden
-- =====================================================
-- Descripción: Agrega un campo booleano para indicar si la orden tiene retención de fuente aplicada
-- Fecha: 2025-01-XX
-- Base de datos: MySQL/MariaDB
-- =====================================================

-- Paso 1: Agregar columna tiene_retencion_fuente a la tabla ordenes
-- Si la columna ya existe, este comando fallará con un error (eso está bien)
ALTER TABLE ordenes 
ADD COLUMN tiene_retencion_fuente BOOLEAN NOT NULL DEFAULT FALSE 
COMMENT 'Indica si la orden tiene retención de fuente aplicada';

-- Paso 2: Verificar que la columna se agregó correctamente
-- Este query muestra información sobre la nueva columna
SELECT 
    column_name AS 'Nombre Columna',
    data_type AS 'Tipo de Dato',
    is_nullable AS 'Permite NULL',
    column_default AS 'Valor por Defecto',
    column_comment AS 'Comentario'
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'ordenes' 
  AND column_name = 'tiene_retencion_fuente';

-- Paso 3: Verificar valores actuales en órdenes existentes
-- Todas las órdenes existentes deberían tener FALSE por defecto
SELECT 
    id,
    numero,
    fecha,
    tiene_retencion_fuente,
    CASE 
        WHEN tiene_retencion_fuente = TRUE THEN 'SÍ tiene retención'
        ELSE 'NO tiene retención'
    END AS estado_retencion
FROM ordenes 
ORDER BY id DESC
LIMIT 20;

-- Paso 4: Contar órdenes con y sin retención de fuente
SELECT 
    tiene_retencion_fuente AS 'Tiene Retención',
    COUNT(*) AS 'Cantidad de Órdenes'
FROM ordenes
GROUP BY tiene_retencion_fuente;

-- =====================================================
-- Notas Importantes:
-- =====================================================
-- 1. El campo tiene valor por defecto FALSE
-- 2. El campo es NOT NULL (siempre tiene un valor)
-- 3. Las órdenes existentes se establecerán automáticamente en FALSE
-- 4. Si necesitas cambiar órdenes existentes a TRUE, usa:
--    UPDATE ordenes SET tiene_retencion_fuente = TRUE WHERE id = X;
-- 5. Este script es seguro de ejecutar múltiples veces si usas:
--    ALTER TABLE ordenes ADD COLUMN IF NOT EXISTS tiene_retencion_fuente ...
--    (pero MySQL no soporta IF NOT EXISTS en ALTER TABLE)
-- =====================================================

-- =====================================================
-- Script Alternativo (si la columna ya existe y quieres recrearla):
-- =====================================================
-- PASO 1: Eliminar columna si existe (CUIDADO: esto borra los datos)
-- ALTER TABLE ordenes DROP COLUMN IF EXISTS tiene_retencion_fuente;
--
-- PASO 2: Agregar columna nuevamente
-- ALTER TABLE ordenes 
-- ADD COLUMN tiene_retencion_fuente BOOLEAN NOT NULL DEFAULT FALSE;
-- =====================================================

