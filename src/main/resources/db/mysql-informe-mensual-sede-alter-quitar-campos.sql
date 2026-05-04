-- Migración: quita columnas ya no usadas por el backend.
-- Ejecutar UNA VEZ en la BD donde ya corriste el CREATE antiguo de cierre_informe_mensual_sede.
--
-- Si falla por nombre de FK distinto:
--   SHOW CREATE TABLE cierre_informe_mensual_sede;
-- y reemplaza fk_cierre_informe_trabajador por el nombre real.
--
-- Si nunca existió la FK al trabajador, quita solo la línea DROP FOREIGN KEY ... y el resto igual.

ALTER TABLE cierre_informe_mensual_sede
    DROP FOREIGN KEY fk_cierre_informe_trabajador,
    DROP COLUMN cerrado_por_trabajador_id,
    DROP COLUMN fecha_cierre,
    DROP COLUMN notas,
    DROP COLUMN valor_inventario;
