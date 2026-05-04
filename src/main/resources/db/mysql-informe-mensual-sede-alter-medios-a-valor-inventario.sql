-- BD actual con columnas dinero_efectivo_mes, … (desglose): quitarlas y añadir valor_inventario.
-- Ejecutar UNA VEZ después de desplegar el backend alineado.
-- Orden típico: primero mysql-informe-mensual-sede-alter-quitar-campos.sql (si aplica); luego este.
--
-- Si valor_inventario ya existe en tu tabla (esquema viejo sin haber corrido alter-quitar-campos):
-- ejecute solo las cuatro DROP COLUMN dinero_* y omita el ADD.

ALTER TABLE cierre_informe_mensual_sede
    DROP COLUMN dinero_efectivo_mes,
    DROP COLUMN dinero_transferencia_mes,
    DROP COLUMN dinero_cheque_mes,
    DROP COLUMN dinero_deposito_mes,
    ADD COLUMN valor_inventario DOUBLE NULL AFTER deudas_activas_totales;
