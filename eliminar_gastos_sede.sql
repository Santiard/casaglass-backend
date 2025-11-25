-- ============================================================================
-- SCRIPT SQL PARA ELIMINAR GASTOS_SEDE Y ACTUALIZAR ENTREGAS_DINERO
-- ============================================================================
-- FECHA: 2025-01-XX
-- DESCRIPCIÓN: Elimina completamente la funcionalidad de gastos sede
--              y actualiza la tabla entregas_dinero para reflejar los cambios
-- ============================================================================
-- IMPORTANTE: 
-- 1. Hacer BACKUP de la base de datos antes de ejecutar
-- 2. Ejecutar este script en orden
-- 3. Si algún comando falla, verifica manualmente y continúa
-- ============================================================================

-- ============================================================================
-- PASO 1: VERIFICAR ESTRUCTURA DE gastos_sede (OPCIONAL - PARA REFERENCIA)
-- ============================================================================
-- Ejecuta esto primero para ver los nombres de las foreign keys:
-- SHOW CREATE TABLE gastos_sede;

-- ============================================================================
-- PASO 2: ELIMINAR FOREIGN KEYS DE gastos_sede
-- ============================================================================
-- IMPORTANTE: Ajusta los nombres de las foreign keys según el resultado de
-- SHOW CREATE TABLE gastos_sede. Los nombres pueden variar según tu base de datos.

-- Ejemplo de cómo eliminar foreign keys (ajusta los nombres):
-- ALTER TABLE gastos_sede DROP FOREIGN KEY fk_gasto_entrega;
-- ALTER TABLE gastos_sede DROP FOREIGN KEY fk_gasto_sede;
-- ALTER TABLE gastos_sede DROP FOREIGN KEY fk_gasto_empleado;
-- ALTER TABLE gastos_sede DROP FOREIGN KEY fk_gasto_proveedor;

-- Si no estás seguro de los nombres, ejecuta primero:
-- SHOW CREATE TABLE gastos_sede;
-- Y luego elimina las foreign keys una por una con los nombres correctos.

-- ============================================================================
-- PASO 3: ELIMINAR LA TABLA gastos_sede COMPLETAMENTE
-- ============================================================================

DROP TABLE IF EXISTS gastos_sede;

-- ============================================================================
-- PASO 4: ACTUALIZAR TABLA entregas_dinero
-- ============================================================================
-- Actualizar la diferencia para que sea: diferencia = monto_esperado - monto_entregado
-- (Ya no se restan gastos del cálculo)

UPDATE entregas_dinero 
SET diferencia = monto_esperado - monto_entregado;

-- ============================================================================
-- PASO 5: ELIMINAR COLUMNA monto_gastos DE entregas_dinero
-- ============================================================================
-- Eliminar la columna monto_gastos que ya no se usa

ALTER TABLE entregas_dinero DROP COLUMN monto_gastos;

-- ============================================================================
-- VERIFICACIÓN FINAL
-- ============================================================================

-- Verificar que la tabla gastos_sede fue eliminada
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ Tabla gastos_sede eliminada correctamente'
        ELSE '❌ ERROR: La tabla gastos_sede aún existe'
    END AS resultado
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'gastos_sede';

-- Verificar estructura de entregas_dinero (verificar si monto_gastos existe)
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'entregas_dinero'
ORDER BY ORDINAL_POSITION;

-- Mostrar resumen de entregas_dinero actualizadas
SELECT 
    COUNT(*) AS total_entregas,
    SUM(CASE WHEN ABS(diferencia - (monto_esperado - monto_entregado)) < 0.01 THEN 1 ELSE 0 END) AS entregas_con_diferencia_correcta,
    SUM(CASE WHEN ABS(diferencia - (monto_esperado - monto_entregado)) >= 0.01 THEN 1 ELSE 0 END) AS entregas_con_diferencia_incorrecta
FROM entregas_dinero;

-- ============================================================================
-- FIN DEL SCRIPT
-- ============================================================================
-- Si todo salió bien, deberías ver:
-- ✅ Tabla gastos_sede eliminada correctamente
-- La estructura de entregas_dinero sin monto_gastos (si decidiste eliminarla)
-- ============================================================================
