-- Migración para eliminar columnas de retención en entregas especiales

ALTER TABLE entregas_cliente_especial 
  DROP COLUMN total_retencion;

ALTER TABLE entregas_cliente_especial_detalles 
  DROP COLUMN retencion_fuente;
