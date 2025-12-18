-- ═══════════════════════════════════════════════════════════════════════════════
-- SCRIPT SQL: MEJORA DE ALMACENAMIENTO DE MÉTODOS DE PAGO Y RETENCIÓN DE FUENTE
-- Base de Datos: MariaDB
-- Fecha: 2025-12-18
-- ═══════════════════════════════════════════════════════════════════════════════
-- 
-- OBJETIVO:
-- Agregar columnas numéricas para almacenar montos de métodos de pago de forma
-- estructurada en las tablas `ordenes`, `abonos` y `entrega_dinero`, conservando
-- los campos de texto existentes para compatibilidad.
-- ═══════════════════════════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════════════════════════
-- 1️⃣ TABLA ORDENES: Agregar columnas de métodos de pago
-- ═══════════════════════════════════════════════════════════════════════════════

-- Agregar columnas nuevas con valores por defecto
ALTER TABLE ordenes
ADD COLUMN monto_efectivo DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Monto pagado en efectivo',
ADD COLUMN monto_transferencia DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Monto pagado por transferencia bancaria',
ADD COLUMN monto_cheque DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Monto pagado con cheque';

-- Agregar constraint de validación
-- Solo validar en órdenes de CONTADO (credito = false) Y cuando los campos tienen valores
-- Permite registros existentes con valores en 0
ALTER TABLE ordenes
ADD CONSTRAINT check_suma_metodos_pago_orden
CHECK (
  credito = TRUE OR 
  (monto_efectivo = 0 AND monto_transferencia = 0 AND monto_cheque = 0) OR
  (monto_efectivo + monto_transferencia + monto_cheque = total - descuentos)
);

-- Crear índice para consultas por métodos de pago
CREATE INDEX idx_ordenes_metodos_pago 
ON ordenes(monto_efectivo, monto_transferencia, monto_cheque);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 2️⃣ TABLA ABONOS: Agregar columnas de métodos de pago y retención
-- ═══════════════════════════════════════════════════════════════════════════════

-- Agregar columnas nuevas con valores por defecto
ALTER TABLE abonos
ADD COLUMN monto_efectivo DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Monto abonado en efectivo',
ADD COLUMN monto_transferencia DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Monto abonado por transferencia bancaria',
ADD COLUMN monto_cheque DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Monto abonado con cheque',
ADD COLUMN monto_retencion DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'Monto de retención en la fuente aplicado en este abono';

-- Agregar constraint de validación
-- La suma de métodos de pago debe igualar el total del abono
-- Permite registros existentes con valores en 0
ALTER TABLE abonos
ADD CONSTRAINT check_suma_metodos_pago_abono
CHECK (
  (monto_efectivo = 0 AND monto_transferencia = 0 AND monto_cheque = 0) OR
  (monto_efectivo + monto_transferencia + monto_cheque = total)
);

-- Crear índice para consultas por métodos de pago
CREATE INDEX idx_abonos_metodos_pago 
ON abonos(monto_efectivo, monto_transferencia, monto_cheque, monto_retencion);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 3️⃣ TABLA ENTREGAS_DINERO: Agregar columna de retención
-- ═══════════════════════════════════════════════════════════════════════════════

-- Agregar columna de retención en entregas
-- El montoRetencion de la entrega es la SUMA de todos los montoRetencion de los abonos
ALTER TABLE entregas_dinero
ADD COLUMN monto_retencion DECIMAL(15,2) NOT NULL DEFAULT 0.00 
COMMENT 'Suma de todas las retenciones en la fuente de los abonos incluidos en esta entrega';

-- Crear índice para consultas por retención
CREATE INDEX idx_entregas_dinero_retencion 
ON entregas_dinero(monto_retencion);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 4️⃣ VERIFICAR CAMBIOS REALIZADOS
-- ═══════════════════════════════════════════════════════════════════════════════

-- Verificar columnas agregadas en ORDENES
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    COLUMN_DEFAULT, 
    IS_NULLABLE, 
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ordenes'
  AND COLUMN_NAME IN ('monto_efectivo', 'monto_transferencia', 'monto_cheque');

-- Verificar columnas agregadas en ABONOS
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    COLUMN_DEFAULT, 
    IS_NULLABLE, 
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'abonos'
  AND COLUMN_NAME IN ('monto_efectivo', 'monto_transferencia', 'monto_cheque', 'monto_retencion');

-- Verificar columna agregada en ENTREGAS_DINERO
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    COLUMN_DEFAULT, 
    IS_NULLABLE, 
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'entregas_dinero'
  AND COLUMN_NAME = 'monto_retencion';

-- Verificar constraints creados
SELECT 
    CONSTRAINT_NAME, 
    TABLE_NAME, 
    CONSTRAINT_TYPE
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('ordenes', 'abonos')
  AND CONSTRAINT_NAME LIKE 'check_suma_metodos_pago%';

-- ═══════════════════════════════════════════════════════════════════════════════
-- 5️⃣ NOTAS IMPORTANTES
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- ✅ COMPATIBILIDAD:
-- - Los campos de texto (descripcion, metodoPago) se mantienen sin cambios
-- - Nuevas columnas tienen DEFAULT 0.00 para evitar errores en registros existentes
-- - Frontend antiguo seguirá funcionando normalmente
--
-- ⚠️ MIGRACIÓN DE DATOS:
-- - Este script NO migra datos existentes de descripcion/metodoPago a las nuevas columnas
-- - Los registros existentes tendrán valores 0.00 en las columnas numéricas
-- - Se requiere un script adicional de parseo y migración de datos
--
-- 🔒 VALIDACIONES:
-- - ORDENES: Solo valida suma en órdenes de contado (credito = false)
-- - ABONOS: Siempre valida que suma de métodos = total del abono
-- - RETENCIÓN: NO se suma a los métodos de pago (es informativa/contable)
--
-- 📊 TIPOS DE DATOS:
-- - DECIMAL(15,2): Permite hasta 9.999.999.999.999,99
-- - Precisión de 2 decimales (centavos)
-- - Almacenamiento: 2555168.07 (sin puntos de miles, punto decimal)
--
-- ═══════════════════════════════════════════════════════════════════════════════
-- FIN DEL SCRIPT
-- ═══════════════════════════════════════════════════════════════════════════════
