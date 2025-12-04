-- =====================================================
-- Script SQL Simple: Agregar campo tieneRetencionFuente
-- =====================================================
-- EJECUTAR ESTE SCRIPT EN TU BASE DE DATOS
-- =====================================================

-- Agregar columna tiene_retencion_fuente a la tabla ordenes
ALTER TABLE ordenes 
ADD COLUMN tiene_retencion_fuente BOOLEAN NOT NULL DEFAULT FALSE;

-- Verificar que se agregó correctamente (opcional)
DESCRIBE ordenes;
