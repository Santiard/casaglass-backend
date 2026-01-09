-- ============================================
-- 🔧 ELIMINAR COLUMNA ACTIVO DE BANCOS
-- ============================================

-- Eliminar la columna activo y el índice asociado
ALTER TABLE bancos 
DROP COLUMN activo;

-- Verificar estructura actualizada
DESCRIBE bancos;

-- ============================================
-- ✅ RESULTADO ESPERADO:
-- id       BIGINT       PK
-- nombre   VARCHAR(100) UNIQUE
-- ============================================
