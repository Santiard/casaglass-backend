-- Script para verificar qué columnas ICA ya existen en las tablas

-- Verificar BusinessSettings
SELECT 
    'business_settings' AS tabla,
    COLUMN_NAME AS columna,
    DATA_TYPE AS tipo,
    IS_NULLABLE AS nullable,
    COLUMN_DEFAULT AS valor_default
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'business_settings'
AND COLUMN_NAME IN ('ica_rate', 'ica_threshold')
ORDER BY COLUMN_NAME;

-- Verificar Orden
SELECT 
    'ordenes' AS tabla,
    COLUMN_NAME AS columna,
    DATA_TYPE AS tipo,
    IS_NULLABLE AS nullable,
    COLUMN_DEFAULT AS valor_default
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'ordenes'
AND COLUMN_NAME IN ('tiene_retencion_ica', 'porcentaje_ica', 'retencion_ica')
ORDER BY COLUMN_NAME;

-- Verificar Factura
SELECT 
    'facturas' AS tabla,
    COLUMN_NAME AS columna,
    DATA_TYPE AS tipo,
    IS_NULLABLE AS nullable,
    COLUMN_DEFAULT AS valor_default
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'facturas'
AND COLUMN_NAME = 'retencion_ica'
ORDER BY COLUMN_NAME;

