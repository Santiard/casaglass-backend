-- ============================================
-- 🏦 CREACIÓN DE TABLA BANCOS
-- ============================================

CREATE TABLE IF NOT EXISTS bancos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 🏦 DATOS INICIALES - BANCOS DE COLOMBIA
-- ============================================

INSERT INTO bancos (nombre) VALUES
('Bancolombia'),
('Banco de Bogotá'),
('Davivienda'),
('BBVA Colombia'),
('Banco de Occidente'),
('Banco Popular'),
('Banco AV Villas'),
('Banco Caja Social'),
('Banco Agrario'),
('Banco GNB Sudameris'),
('Banco Pichincha'),
('Banco Falabella'),
('Banco Cooperativo Coopcentral'),
('Banco Santander'),
('Scotiabank Colpatria'),
('Citibank'),
('Banco Finandina'),
('Banco Mundo Mujer'),
('Banco W'),
('Banco Itaú'),
('Nequi'),
('Daviplata'),
('Efectivo'),
('Cheque'),
('Transferencia Bancaria'),
('Otro')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- ============================================
-- ✅ VERIFICACIÓN
-- ============================================
SELECT * FROM bancos ORDER BY nombre;
