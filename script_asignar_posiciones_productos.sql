-- ============================================================
-- Script SQL para MariaDB: Asignar Posiciones a Productos
-- ============================================================
-- 
-- Descripción:
-- Este script asigna números secuenciales (1, 2, 3, 4, 5...)
-- a cada producto en la tabla 'productos', ordenados por ID ascendente.
-- 
-- La columna 'posicion' se actualiza con estos números para permitir
-- un ordenamiento personalizado en el frontend.
--
-- ============================================================

-- ============================================================
-- OPCIÓN 1: Usando ROW_NUMBER() (Recomendado para MariaDB 10.2+)
-- ============================================================
-- Esta versión es más moderna y eficiente

UPDATE productos p
INNER JOIN (
    SELECT 
        id,
        ROW_NUMBER() OVER (ORDER BY id ASC) AS nueva_posicion
    FROM productos
) AS numerados ON p.id = numerados.id
SET p.posicion = CAST(numerados.nueva_posicion AS CHAR);

-- ============================================================
-- OPCIÓN 2: Usando Variables SET (Compatible con versiones anteriores)
-- ============================================================
-- Usa esta versión si tu MariaDB es anterior a 10.2

-- Descomenta las siguientes líneas si necesitas usar esta opción:
/*
SET @posicion = 0;

UPDATE productos
SET posicion = CAST((@posicion := @posicion + 1) AS CHAR)
ORDER BY id ASC;
*/

-- ============================================================
-- VERIFICACIÓN: Consulta para verificar los resultados
-- ============================================================
-- Ejecuta esta consulta después del UPDATE para verificar:

-- SELECT 
--     id,
--     codigo,
--     nombre,
--     posicion,
--     CASE 
--         WHEN posicion IS NULL THEN '❌ Sin posición'
--         WHEN CAST(posicion AS UNSIGNED) = 0 THEN '⚠️ Posición inválida'
--         ELSE '✅ OK'
--     END AS estado
-- FROM productos
-- ORDER BY CAST(posicion AS UNSIGNED) ASC
-- LIMIT 20;

-- ============================================================
-- ESTADÍSTICAS: Ver cuántos productos tienen posición asignada
-- ============================================================
-- SELECT 
--     COUNT(*) AS total_productos,
--     COUNT(posicion) AS productos_con_posicion,
--     COUNT(*) - COUNT(posicion) AS productos_sin_posicion,
--     MIN(CAST(posicion AS UNSIGNED)) AS posicion_minima,
--     MAX(CAST(posicion AS UNSIGNED)) AS posicion_maxima
-- FROM productos;

-- ============================================================
-- NOTAS IMPORTANTES:
-- ============================================================
-- 1. La columna 'posicion' es de tipo VARCHAR/STRING en la BD
--    por lo que los números se almacenan como texto.
--
-- 2. Si necesitas ordenar por posición numérica en SQL, usa:
--    ORDER BY CAST(posicion AS UNSIGNED) ASC
--
-- 3. Este script asigna posiciones basadas en el orden de los IDs.
--    Si quieres cambiar el orden (por ejemplo, por código o nombre),
--    modifica la cláusula ORDER BY en el ROW_NUMBER().
--
-- 4. Si ejecutas este script múltiples veces, reasignará todas
--    las posiciones desde el principio. Esto es útil si agregaste
--    nuevos productos y quieres reordenar todo.
--
-- 5. Para asignar posiciones solo a productos que no tienen posición:
--    Agrega WHERE posicion IS NULL en el UPDATE.
--
-- ============================================================
-- VERSIÓN INCREMENTAL: Solo asignar a productos sin posición
-- ============================================================
-- Si quieres asignar posiciones solo a productos que aún no tienen:

/*
UPDATE productos p
INNER JOIN (
    SELECT 
        id,
        ROW_NUMBER() OVER (ORDER BY id ASC) AS nueva_posicion
    FROM productos
    WHERE posicion IS NULL
) AS numerados ON p.id = numerados.id
SET p.posicion = CAST(
    (SELECT COALESCE(MAX(CAST(posicion AS UNSIGNED)), 0) FROM productos WHERE posicion IS NOT NULL) + numerados.nueva_posicion 
    AS CHAR
)
WHERE p.posicion IS NULL;
*/

-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================

