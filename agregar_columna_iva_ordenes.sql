-- ============================================================================
-- SCRIPT SQL: AGREGAR COLUMNA IVA A LA TABLA ORDENES
-- ============================================================================
-- FECHA: 2025-01-XX
-- DESCRIPCIÓN: Agrega la columna iva a la tabla ordenes
--              para almacenar el valor monetario del IVA calculado
-- ============================================================================

-- Agregar columna iva a la tabla ordenes
ALTER TABLE ordenes 
ADD COLUMN iva DECIMAL(19, 2) NOT NULL DEFAULT 0.00 
COMMENT 'Valor monetario del IVA calculado. Se calcula como: (total facturado - descuentos) - subtotal (base sin IVA)';

-- Calcular IVA para órdenes existentes (si es necesario)
-- NOTA: Esto es opcional, ya que el valor por defecto es 0.00
-- Si se desea calcular el IVA para órdenes existentes, usar:
-- UPDATE ordenes 
-- SET iva = (subtotal * 0.19) / 1.19
-- WHERE subtotal > 0;

-- ============================================================================
-- NOTAS:
-- ============================================================================
-- 1. La columna se inicializa con valor 0.00 para todas las órdenes existentes
-- 2. Tipo de dato: DECIMAL(19, 2)
--    - 19 dígitos en total (17 enteros + 2 decimales)
--    - Máximo valor: 99,999,999,999,999,999.99
-- 3. El backend calculará automáticamente el valor al crear/actualizar órdenes
-- 4. Fórmula de cálculo: 
--    - subtotalFacturado = suma de (precioUnitario × cantidad) de todos los items
--    - subtotal (base sin IVA) = (subtotalFacturado - descuentos) / 1.19
--    - iva = (subtotalFacturado - descuentos) - subtotal
-- ============================================================================


