-- ============================================================================
-- ACTUALIZAR COLUMNA metodo_pago EN TABLA abonos
-- ============================================================================
-- 
-- PROBLEMA:
-- La columna metodo_pago tiene un tamaño muy pequeño (VARCHAR(20)) que no
-- permite almacenar descripciones largas con múltiples métodos de pago,
-- retenciones y observaciones.
--
-- SOLUCIÓN:
-- Cambiar la columna a VARCHAR(3000) para soportar descripciones largas
-- con múltiples métodos, retenciones y observaciones
--
-- ============================================================================

-- Verificar el tamaño actual de la columna
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'abonos'
  AND COLUMN_NAME = 'metodo_pago';

-- Cambiar la columna a VARCHAR(3000)
ALTER TABLE abonos 
MODIFY COLUMN metodo_pago VARCHAR(3000) NOT NULL;

-- Verificar el cambio
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'abonos'
  AND COLUMN_NAME = 'metodo_pago';

-- ============================================================================
-- NOTAS:
-- ============================================================================
-- 
-- - VARCHAR(3000) permite almacenar hasta 3,000 caracteres
-- - Esto es suficiente para descripciones largas con múltiples métodos,
--   retenciones y observaciones
-- - El límite de 3,000 caracteres es un buen balance entre flexibilidad
--   y rendimiento
--
-- ============================================================================

