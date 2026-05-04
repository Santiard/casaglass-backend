-- =============================================================================
-- INFORME MENSUAL POR SEDE — reset completo (MySQL)
-- =============================================================================
-- ADVERTENCIA: DROP TABLE borra TODOS los cierres mensuales guardados.
-- Usar solo si no necesitas conservar esos datos o ya están vacíos.
--
-- Cómo ejecutar (ej. MySQL Workbench o cli):
--   mysql -u USUARIO -p NOMBRE_BD < mysql-informe-mensual-sede-reset-produccion.sql
-- O pegar todo el contenido en una pestaña y ejecutar (⚡ ícono rayo).
-- =============================================================================

-- Por si algo externo llegara a referenciar la tabla (poco habitual aquí).
SET SESSION foreign_key_checks = 0;

DROP TABLE IF EXISTS cierre_informe_mensual_sede;

SET SESSION foreign_key_checks = 1;

CREATE TABLE cierre_informe_mensual_sede (
    id BIGINT NOT NULL AUTO_INCREMENT,

    sede_id BIGINT NOT NULL,
    anio SMALLINT NOT NULL COMMENT 'Ej. 2026',
    mes TINYINT NOT NULL COMMENT '1-12',
    estado VARCHAR(20) NOT NULL DEFAULT 'CERRADO',

    ventas_mes DOUBLE NULL,
    dinero_recogido_mes DOUBLE NULL,
    deudas_mes DOUBLE NULL,
    deudas_activas_totales DOUBLE NULL,
    valor_inventario DOUBLE NULL,

    orden_numero_min BIGINT NULL,
    orden_numero_max BIGINT NULL,
    cantidad_ordenes_ventas_mes INT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_cierre_sede_anio_mes (sede_id, anio, mes),
    CONSTRAINT fk_cierre_informe_sede FOREIGN KEY (sede_id) REFERENCES sedes (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE INDEX idx_cierre_informe_sede_anio ON cierre_informe_mensual_sede (sede_id, anio);
