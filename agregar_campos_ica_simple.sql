-- Script SQL SIMPLE para agregar campos ICA
-- Ejecuta solo las columnas que faltan

-- 1. BusinessSettings
-- Solo agregar ica_threshold si ica_rate ya existe pero ica_threshold no
ALTER TABLE `business_settings` 
ADD COLUMN IF NOT EXISTS `ica_threshold` BIGINT(20) NOT NULL DEFAULT 1000000 AFTER `ica_rate`;

-- 2. Orden
ALTER TABLE `ordenes` 
ADD COLUMN IF NOT EXISTS `tiene_retencion_ica` BIT(1) NOT NULL DEFAULT 0 AFTER `retencion_fuente`,
ADD COLUMN IF NOT EXISTS `porcentaje_ica` DOUBLE NULL AFTER `tiene_retencion_ica`,
ADD COLUMN IF NOT EXISTS `retencion_ica` DOUBLE NOT NULL DEFAULT 0.0 AFTER `porcentaje_ica`;

-- 3. Factura
ALTER TABLE `facturas` 
ADD COLUMN IF NOT EXISTS `retencion_ica` DOUBLE NOT NULL DEFAULT 0.0 AFTER `retencion_fuente`;

