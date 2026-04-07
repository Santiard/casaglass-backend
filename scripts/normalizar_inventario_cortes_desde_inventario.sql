-- Normaliza inventario de cortes:
-- 1) Migra cantidades de productos que son cortes desde `inventario` a `inventario_cortes` por sede.
-- 2) Elimina los registros migrados de `inventario`.
--
-- Seguro para ejecutar una sola vez en ventana de mantenimiento.
-- Si se ejecuta de nuevo sin nuevos datos erróneos, no tendrá efecto.

START TRANSACTION;

-- Pre-chequeo: cuántos registros de inventario normal corresponden a cortes
SELECT COUNT(*) AS registros_cortes_en_inventario
FROM inventario i
WHERE EXISTS (
    SELECT 1
    FROM cortes c
    WHERE c.id = i.producto_id
);

-- Pasar stock de cortes (incluye negativos y positivos) desde inventario -> inventario_cortes
INSERT INTO inventario_cortes (corte_id, sede_id, cantidad)
SELECT
    i.producto_id AS corte_id,
    i.sede_id,
    i.cantidad
FROM inventario i
INNER JOIN cortes c ON c.id = i.producto_id
ON DUPLICATE KEY UPDATE
    cantidad = inventario_cortes.cantidad + VALUES(cantidad);

-- Eliminar del inventario normal los productos que son cortes
-- (misma lógica, pero con WHERE explícito para evitar alertas de cliente SQL)
DELETE FROM inventario
WHERE EXISTS (
    SELECT 1
    FROM cortes c
    WHERE c.id = inventario.producto_id
);

COMMIT;

-- Auditoría rápida post-migración
-- Debe devolver 0 filas si no quedan cortes en inventario normal.
-- SELECT i.* FROM inventario i INNER JOIN cortes c ON c.id = i.producto_id;
