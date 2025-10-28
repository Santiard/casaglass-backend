-- ==========================================
-- SCRIPT DE ACTUALIZACIÓN DE TABLA facturas
-- IMPORTANTE: Ejecutar en el orden indicado
-- ==========================================

-- 1. PRIMERO: ELIMINAR FOREIGN KEYS (si existen)
-- Nota: Los nombres de las FK pueden variar, ajusta según tu esquema

-- Para cliente_id
ALTER TABLE facturas DROP FOREIGN KEY FK_factura_cliente;
ALTER TABLE facturas DROP FOREIGN KEY FKebytgljwj03rs91cbtjvc3cdk;

-- Para sede_id
ALTER TABLE facturas DROP FOREIGN KEY FK_factura_sdeclare;

-- Para trabajador_id
ALTER TABLE factura DROP FOREIGN KEY FK_factura_trabajador;

-- 2. SEGUNDO: ELIMINAR ÍNDICES
DROP INDEX IF EXISTS idx_factura_cliente ON facturas;

-- 3. TERCERO: ELIMINAR COLUMNAS
ALTER TABLE facturas DROP COLUMN IF EXISTS cliente_id;
ALTER TABLE facturas DROP COLUMN IF EXISTS sede_id;
ALTER TABLE facturas DROP COLUMN IF EXISTS trabajador_id;
ALTER TABLE facturas DROP COLUMN IF EXISTS otros_impuestos;

-- 4. CUARTO: RENOMBRAR COLUMNA numero A numero_factura
ALTER TABLE facturas CHANGE COLUMN numero numero_factura VARCHAR(255);

-- ==========================================
-- VERIFICAR QUE LA TABLA QUEDÓ CORRECTAMENTE
-- ==========================================
DESCRIBE facturas;

