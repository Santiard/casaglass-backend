-- ==========================================
-- SCRIPT DE ACTUALIZACIÓN DE TABLA facturas
-- IMPORTANTE: Ejecutar en el orden indicado
-- ==========================================

-- PASO 1: VER LOS NOMBRES DE LAS FOREIGN KEYS ACTUALES
-- Ejecuta esto primero para ver los nombres exactos:
SHOW CREATE TABLE facturas;

-- PASO 2: ELIMINAR FOREIGN KEYS (ajusta los nombres según lo que veas arriba)
-- Ejemplo típico de nombres de FK:
-- ALTER TABLE facturas DROP FOREIGN KEY FK_factura_cliente_id;
-- ALTER TABLE facturas DROP FOREIGN KEY FK_factura_sede_id;
-- ALTER TABLE facturas DROP FOREIGN KEY FK_factura_trabajador_id;

-- PASO 3: ELIMINAR COLUMNAS (esto eliminará automáticamente los índices asociados)
ALTER TABLE facturas DROP COLUMN cliente_id;
ALTER TABLE facturas DROP COLUMN sede_id;
ALTER TABLE facturas DROP COLUMN trabajador_id;
ALTER TABLE facturas DROP COLUMN otros_impuestos;

-- PASO 4: RENOMBRAR COLUMNA numero A numero_factura
ALTER TABLE facturas CHANGE COLUMN numero numero_factura VARCHAR(255);

-- ==========================================
-- VERIFICAR QUE LA TABLA QUEDÓ CORRECTAMENTE
-- ==========================================
DESCRIBE facturas;

