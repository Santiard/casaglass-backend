-- ============================================================
-- Script SQL para MariaDB: Actualizar Version de Productos
-- ============================================================
-- 
-- Descripción:
-- Este script actualiza el campo 'version' a 1 para todos los productos
-- que tienen version = NULL en la base de datos.
-- 
-- El campo 'version' es usado por Hibernate para control de concurrencia
-- optimista (@Version). Si es NULL, puede causar problemas al guardar/actualizar.
--
-- ============================================================

-- ============================================================
-- ACTUALIZAR VERSION A 1 EN PRODUCTOS CON VERSION NULL
-- ============================================================

-- Verificar cuántos productos tienen version NULL
SELECT 
    COUNT(*) AS productos_sin_version
FROM productos
WHERE version IS NULL;

-- Actualizar todos los productos con version NULL a version = 1
UPDATE productos
SET version = 1
WHERE version IS NULL;

-- Verificar que se actualizaron correctamente
SELECT 
    COUNT(*) AS productos_con_version,
    MIN(version) AS version_minima,
    MAX(version) AS version_maxima
FROM productos
WHERE version IS NOT NULL;

-- ============================================================
-- OPCIONAL: Ver productos que aún tienen version NULL (no debería haber)
-- ============================================================
-- SELECT 
--     id,
--     codigo,
--     nombre,
--     version
-- FROM productos
-- WHERE version IS NULL;

-- ============================================================
-- NOTAS IMPORTANTES:
-- ============================================================
-- 1. Este script solo actualiza productos con version = NULL
-- 2. Los productos que ya tienen version NO se modifican
-- 3. El campo version es usado por Hibernate para control de concurrencia
-- 4. Después de ejecutar este script, todos los productos deberían tener version >= 1
--
-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================

