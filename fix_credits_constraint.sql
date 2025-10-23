-- ============================================================
-- SCRIPT PARA CORREGIR RESTRICCIÓN ÚNICA DE CRÉDITOS
-- ============================================================
-- Este script elimina la restricción problemática que impide
-- múltiples créditos por cliente y establece la restricción correcta

-- 1. Verificar la estructura actual de la tabla creditos
SHOW CREATE TABLE creditos;

-- 2. Eliminar la restricción única problemática
-- Esta restricción impide múltiples créditos por cliente
ALTER TABLE creditos DROP CONSTRAINT UKeekhj1y0ctuc100p6o37ee7nk;

-- 3. Verificar que se eliminó correctamente
SHOW CREATE TABLE creditos;

-- 4. Agregar restricción única correcta solo en orden_id
-- Cada orden puede tener máximo un crédito (esto sí es correcto)
ALTER TABLE creditos ADD CONSTRAINT uk_credito_orden UNIQUE (orden_id);

-- 5. Verificar la nueva estructura final
SHOW CREATE TABLE creditos;

-- ============================================================
-- RESULTADO ESPERADO:
-- - Un cliente puede tener múltiples créditos activos
-- - Cada orden solo puede tener un crédito (correcto)
-- - La restricción UKeekhj1y0ctuc100p6o37ee7nk eliminada
-- ============================================================
