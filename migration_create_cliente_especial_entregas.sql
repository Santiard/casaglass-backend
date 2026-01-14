-- Creación de tablas para registrar entregas del cliente especial

CREATE TABLE IF NOT EXISTS entregas_cliente_especial (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha_registro DATETIME NOT NULL,
    ejecutado_por VARCHAR(120),
    total_creditos INT NOT NULL DEFAULT 0,
    total_monto_credito DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_retencion DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    observaciones VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS entregas_cliente_especial_detalles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entrega_id BIGINT NOT NULL,
    credito_id BIGINT NOT NULL,
    orden_id BIGINT NOT NULL,
    numero_orden BIGINT,
    fecha_credito DATE,
    total_credito DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    saldo_anterior DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    retencion_fuente DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_entrega_cliente_especial_detalle_entrega FOREIGN KEY (entrega_id) REFERENCES entregas_cliente_especial(id),
    CONSTRAINT fk_entrega_cliente_especial_detalle_credito FOREIGN KEY (credito_id) REFERENCES creditos(id),
    CONSTRAINT fk_entrega_cliente_especial_detalle_orden FOREIGN KEY (orden_id) REFERENCES ordenes(id)
);

CREATE INDEX idx_entrega_ce_fecha ON entregas_cliente_especial(fecha_registro);
CREATE INDEX idx_entrega_ce_ejecutado ON entregas_cliente_especial(ejecutado_por);
CREATE INDEX idx_entrega_ce_det_entrega ON entregas_cliente_especial_detalles(entrega_id);
CREATE INDEX idx_entrega_ce_det_credito ON entregas_cliente_especial_detalles(credito_id);
