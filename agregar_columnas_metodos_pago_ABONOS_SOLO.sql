-- ═══════════════════════════════════════════════════════════════════════════════
-- SCRIPT SQL ALTERNATIVO: Sin constraints para compatibilidad con datos existentes
-- ═══════════════════════════════════════════════════════════════════════════════

-- SOLO PARA TABLA ABONOS - Agregar columnas SIN constraint
ALTER TABLE abonos
ADD COLUMN monto_efectivo DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Monto abonado en efectivo',
ADD COLUMN monto_transferencia DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Monto abonado por transferencia bancaria',
ADD COLUMN monto_cheque DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Monto abonado con cheque',
ADD COLUMN monto_retencion DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Monto de retención en la fuente aplicado en este abono';

-- Crear índice para consultas por métodos de pago
CREATE INDEX idx_abonos_metodos_pago 
ON abonos(monto_efectivo, monto_transferencia, monto_cheque, monto_retencion);

-- NOTA: El constraint se validará solo en el backend (AbonoService.java)
-- para nuevos registros, pero no en la base de datos para permitir registros existentes
