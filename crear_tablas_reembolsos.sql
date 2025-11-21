-- ============================================
-- Script SQL para crear tablas de REEMBOLSOS
-- Base de datos: MariaDB/MySQL
-- Para ejecutar en DBeaver o cliente SQL
-- ============================================

-- ============================================
-- TABLA 1: reembolsos_ingreso
-- ============================================
CREATE TABLE IF NOT EXISTS reembolsos_ingreso (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha DATE NOT NULL,
    ingreso_id BIGINT NOT NULL,
    proveedor_id BIGINT NOT NULL,
    numero_factura_devolucion VARCHAR(100),
    motivo VARCHAR(500),
    total_reembolso DOUBLE NOT NULL DEFAULT 0.0,
    procesado BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    
    -- Índices
    INDEX idx_reembolso_ingreso_fecha (fecha),
    INDEX idx_reembolso_ingreso_ingreso (ingreso_id),
    INDEX idx_reembolso_ingreso_proveedor (proveedor_id),
    
    -- Foreign Keys
    CONSTRAINT fk_reembolso_ingreso_ingreso 
        FOREIGN KEY (ingreso_id) 
        REFERENCES ingresos(id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    
    CONSTRAINT fk_reembolso_ingreso_proveedor 
        FOREIGN KEY (proveedor_id) 
        REFERENCES proveedores(id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    
    -- Check constraint para estado
    CONSTRAINT chk_reembolso_ingreso_estado 
        CHECK (estado IN ('PENDIENTE', 'PROCESADO', 'ANULADO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLA 2: reembolso_ingreso_detalles
-- ============================================
CREATE TABLE IF NOT EXISTS reembolso_ingreso_detalles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reembolso_ingreso_id BIGINT NOT NULL,
    ingreso_detalle_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    costo_unitario DOUBLE NOT NULL,
    total_linea DOUBLE NOT NULL DEFAULT 0.0,
    
    -- Índices
    INDEX idx_reembolso_ingreso_detalle_reembolso (reembolso_ingreso_id),
    INDEX idx_reembolso_ingreso_detalle_ingreso (ingreso_detalle_id),
    INDEX idx_reembolso_ingreso_detalle_producto (producto_id),
    
    -- Foreign Keys
    CONSTRAINT fk_reembolso_ingreso_detalle_reembolso 
        FOREIGN KEY (reembolso_ingreso_id) 
        REFERENCES reembolsos_ingreso(id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE,
    
    CONSTRAINT fk_reembolso_ingreso_detalle_ingreso_detalle 
        FOREIGN KEY (ingreso_detalle_id) 
        REFERENCES ingreso_detalles(id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    
    CONSTRAINT fk_reembolso_ingreso_detalle_producto 
        FOREIGN KEY (producto_id) 
        REFERENCES productos(id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    
    -- Check constraint para cantidad
    CONSTRAINT chk_reembolso_ingreso_detalle_cantidad 
        CHECK (cantidad > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLA 3: reembolsos_venta
-- ============================================
CREATE TABLE IF NOT EXISTS reembolsos_venta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha DATE NOT NULL,
    orden_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    sede_id BIGINT NOT NULL,
    motivo VARCHAR(500),
    subtotal DOUBLE NOT NULL DEFAULT 0.0,
    descuentos DOUBLE NOT NULL DEFAULT 0.0,
    total_reembolso DOUBLE NOT NULL DEFAULT 0.0,
    forma_reembolso VARCHAR(20) NOT NULL,
    procesado BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    
    -- Índices
    INDEX idx_reembolso_venta_fecha (fecha),
    INDEX idx_reembolso_venta_orden (orden_id),
    INDEX idx_reembolso_venta_cliente (cliente_id),
    
    -- Foreign Keys
    CONSTRAINT fk_reembolso_venta_orden 
        FOREIGN KEY (orden_id) 
        REFERENCES ordenes(id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    
    CONSTRAINT fk_reembolso_venta_cliente 
        FOREIGN KEY (cliente_id) 
        REFERENCES clientes(id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    
    CONSTRAINT fk_reembolso_venta_sede 
        FOREIGN KEY (sede_id) 
        REFERENCES sedes(id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    
    -- Check constraints
    CONSTRAINT chk_reembolso_venta_estado 
        CHECK (estado IN ('PENDIENTE', 'PROCESADO', 'ANULADO')),
    
    CONSTRAINT chk_reembolso_venta_forma 
        CHECK (forma_reembolso IN ('EFECTIVO', 'TRANSFERENCIA', 'NOTA_CREDITO', 'AJUSTE_CREDITO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLA 4: reembolso_venta_detalles
-- ============================================
CREATE TABLE IF NOT EXISTS reembolso_venta_detalles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reembolso_venta_id BIGINT NOT NULL,
    orden_item_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DOUBLE NOT NULL,
    total_linea DOUBLE NOT NULL DEFAULT 0.0,
    
    -- Índices
    INDEX idx_reembolso_venta_detalle_reembolso (reembolso_venta_id),
    INDEX idx_reembolso_venta_detalle_orden (orden_item_id),
    INDEX idx_reembolso_venta_detalle_producto (producto_id),
    
    -- Foreign Keys
    CONSTRAINT fk_reembolso_venta_detalle_reembolso 
        FOREIGN KEY (reembolso_venta_id) 
        REFERENCES reembolsos_venta(id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE,
    
    CONSTRAINT fk_reembolso_venta_detalle_orden_item 
        FOREIGN KEY (orden_item_id) 
        REFERENCES orden_items(id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    
    CONSTRAINT fk_reembolso_venta_detalle_producto 
        FOREIGN KEY (producto_id) 
        REFERENCES productos(id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    
    -- Check constraint para cantidad
    CONSTRAINT chk_reembolso_venta_detalle_cantidad 
        CHECK (cantidad > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- VERIFICACIÓN DE TABLAS CREADAS
-- ============================================
-- Ejecutar estas consultas para verificar que las tablas se crearon correctamente:

-- Ver estructura de reembolsos_ingreso
-- DESCRIBE reembolsos_ingreso;

-- Ver estructura de reembolso_ingreso_detalles
-- DESCRIBE reembolso_ingreso_detalles;

-- Ver estructura de reembolsos_venta
-- DESCRIBE reembolsos_venta;

-- Ver estructura de reembolso_venta_detalles
-- DESCRIBE reembolso_venta_detalles;

-- Ver todas las tablas de reembolsos
-- SHOW TABLES LIKE 'reembolso%';

-- ============================================
-- NOTAS IMPORTANTES
-- ============================================
-- 1. Las tablas usan InnoDB para soportar transacciones y foreign keys
-- 2. Los índices mejoran el rendimiento de las consultas
-- 3. Las foreign keys aseguran integridad referencial
-- 4. ON DELETE CASCADE en detalles: si se elimina un reembolso, se eliminan sus detalles
-- 5. ON DELETE RESTRICT en relaciones principales: no se puede eliminar un ingreso/orden si tiene reembolsos
-- 6. Los check constraints validan valores permitidos para estado y forma_reembolso
-- 7. Las cantidades deben ser mayores a 0 (validado por CHECK constraint)
-- 8. Los totales se calculan automáticamente en la aplicación (triggers opcionales)
-- ============================================

