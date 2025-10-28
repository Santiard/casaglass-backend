-- ==========================================
-- SCRIPT FINAL DE ACTUALIZACIÓN DE TABLA facturas
-- Ejecutar en este orden
-- ==========================================

-- PASO 1: ELIMINAR FOREIGN KEYS (nombres exactos)
ALTER TABLE facturas DROP FOREIGN KEY FK1qiuk10rfkovhlfpsk7oic0v8;
ALTER TABLE facturas DROP FOREIGN KEY FK50aq24ed41why8ffk66y5ve7y;
ALTER TABLE facturas DROP FOREIGN KEY FKdwck9q3lnw6fgrmef7ov7xb54;

-- PASO 2: ELIMINAR COLUMNAS
ALTER TABLE facturas DROP COLUMN cliente_id;
ALTER TABLE facturas DROP COLUMN sede_id;
ALTER TABLE facturas DROP COLUMN trabajador_id;
ALTER TABLE facturas DROP COLUMN otros_impuestos;

-- PASO 3: ELIMINAR LA COLUMNA ANTIGUA numero (si todavía existe)
-- Si ya está renombrada, esta línea dará error pero no pasa nada
ALTER TABLE facturas DROP COLUMN numero;

-- PASO 4: VERIFICAR LA TABLA
DESCRIBE facturas;

