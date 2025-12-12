-- ============================================================================
-- SCRIPT SQL: AGREGAR COLUMNA RETENCIÓN DE FUENTE A LA TABLA ORDENES
-- ============================================================================
-- FECHA: 2025-01-XX
-- DESCRIPCIÓN: Agrega la columna retencion_fuente a la tabla ordenes
--              para almacenar el valor monetario de la retención en la fuente
-- ============================================================================

-- Agregar columna retencion_fuente a la tabla ordenes
ALTER TABLE ordenes 
ADD COLUMN retencion_fuente DECIMAL(19, 2) NOT NULL DEFAULT 0.00 
COMMENT 'Valor monetario de la retención en la fuente. Se calcula automáticamente cuando tiene_retencion_fuente = true y la base imponible supera el umbral configurado';

-- Verificar que la columna se agregó correctamente
-- SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT
-- FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME = 'ordenes'
--   AND COLUMN_NAME = 'retencion_fuente';

-- ============================================================================
-- NOTAS:
-- ============================================================================
-- 1. La columna se inicializa con valor 0.00 para todas las órdenes existentes
-- 2. Tipo de dato: DECIMAL(19, 2)
--    - 19 dígitos en total (17 enteros + 2 decimales)
--    - Máximo valor: 99,999,999,999,999,999.99
--    - Esto permite órdenes de hasta ~4 billones de COP con retención del 2.5%
-- 3. El backend calculará automáticamente el valor cuando:
--    - tiene_retencion_fuente = true
--    - base_imponible (subtotal - descuentos) >= umbral configurado en business_settings
-- 4. Fórmula de cálculo: retencion_fuente = base_imponible * (rete_rate / 100)
-- 5. El total de la orden se calcula como: total = subtotal - descuentos - retencion_fuente
-- ============================================================================

