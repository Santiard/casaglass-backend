-- =====================================================
-- SCRIPT: MIGRAR PRODUCTOS VIDRIO FALTANTES
-- =====================================================
-- Descripción: Inserta registros en productos_vidrio para productos
--              que deberían ser vidrios pero no tienen registro.
-- Fecha: 2025-11-29
-- =====================================================

-- IMPORTANTE:
-- 1. Hacer BACKUP de la base de datos antes de ejecutar
-- 2. Este script asume que los productos vidrio tienen valores por defecto
-- 3. Ajusta los valores de mm, m1, m2, m1m2 según tus necesidades
-- =====================================================

-- =====================================================
-- PASO 1: IDENTIFICAR PRODUCTOS VIDRIO SIN REGISTRO
-- =====================================================
-- Ver productos que probablemente son vidrios pero no tienen registro en productos_vidrio
-- Ajusta la condición según cómo identifiques los vidrios (categoría, nombre, etc.)
SELECT 
    p.id,
    p.codigo,
    p.nombre,
    p.categoria_id,
    c.nombre as categoria_nombre
FROM productos p
LEFT JOIN productos_vidrio pv ON p.id = pv.id
LEFT JOIN categorias c ON p.categoria_id = c.id
WHERE pv.id IS NULL
  AND (
    -- Ajusta estas condiciones según cómo identifiques los vidrios
    c.nombre LIKE '%VIDRIO%' 
    OR c.nombre LIKE '%VIDRI%'
    OR p.nombre LIKE '%VIDRIO%'
    OR p.codigo LIKE 'VID-%'
    -- O especifica los IDs directamente:
    -- p.id IN (104, 105, 112, 113)
  )
ORDER BY p.id;

-- =====================================================
-- PASO 2: INSERTAR REGISTROS FALTANTES
-- =====================================================
-- IMPORTANTE: Ajusta los valores de mm, m1, m2 según los productos reales
-- Si no conoces los valores, usa valores por defecto o NULL (si la columna lo permite)

-- OPCIÓN A: Insertar con valores por defecto (ajusta según necesites)
INSERT INTO productos_vidrio (id, mm, m1, m2, m1m2)
SELECT 
    p.id,
    3.0 as mm,        -- Ajusta este valor
    1.0 as m1,        -- Ajusta este valor
    1.0 as m2,        -- Ajusta este valor
    1.0 as m1m2       -- m1 * m2
FROM productos p
LEFT JOIN productos_vidrio pv ON p.id = pv.id
LEFT JOIN categorias c ON p.categoria_id = c.id
WHERE pv.id IS NULL
  AND (
    -- Mismas condiciones que en el SELECT anterior
    c.nombre LIKE '%VIDRIO%' 
    OR c.nombre LIKE '%VIDRI%'
    OR p.nombre LIKE '%VIDRIO%'
    OR p.codigo LIKE 'VID-%'
    -- O especifica los IDs directamente:
    -- p.id IN (104, 105, 112, 113)
  );

-- OPCIÓN B: Insertar solo IDs específicos (más seguro)
-- Descomenta y ajusta los IDs y valores según tus productos reales
/*
INSERT INTO productos_vidrio (id, mm, m1, m2, m1m2) VALUES
(104, 3.0, 2.0, 4.0, 8.0),  -- Ajusta valores según el producto real
(105, 3.0, 2.0, 4.0, 8.0),  -- Ajusta valores según el producto real
(112, 3.0, 2.0, 4.0, 8.0),  -- Ajusta valores según el producto real
(113, 3.0, 2.0, 4.0, 8.0);  -- Ajusta valores según el producto real
*/

-- =====================================================
-- PASO 3: VERIFICAR RESULTADO
-- =====================================================
-- Verificar que los registros se insertaron correctamente
SELECT 
    p.id,
    p.codigo,
    p.nombre,
    pv.mm,
    pv.m1,
    pv.m2,
    pv.m1m2
FROM productos p
INNER JOIN productos_vidrio pv ON p.id = pv.id
ORDER BY p.id;

-- Contar productos vidrio
SELECT COUNT(*) as total_productos_vidrio
FROM productos_vidrio;

-- =====================================================
-- NOTAS IMPORTANTES
-- =====================================================
-- 1. Los valores mm, m1, m2, m1m2 son obligatorios (NOT NULL)
-- 2. Asegúrate de usar valores reales de tus productos
-- 3. Si no conoces los valores, puedes consultarlos desde otra fuente
--    o usar valores por defecto temporalmente
-- 4. Después de ejecutar este script, los productos vidrio deberían
--    aparecer en el endpoint /api/inventario-completo/vidrios
-- =====================================================

