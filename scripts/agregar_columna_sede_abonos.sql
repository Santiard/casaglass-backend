-- ============================================================
-- 📋 SCRIPT: Agregar columna sede_id a la tabla abonos
-- ============================================================
-- Este script agrega la columna sede_id a la tabla abonos
-- para que cada abono tenga su propia sede (donde se registra el pago),
-- independientemente de la sede de la orden original.
--
-- IMPORTANTE: Después de ejecutar este script, los abonos existentes
-- quedarán con sede_id = NULL. Debes actualizarlos manualmente o con
-- un script de migración de datos.
--
-- Fecha: 2026-01-20
-- ============================================================

-- Agregar columna sede_id (nullable inicialmente para no romper datos existentes)
ALTER TABLE abonos
ADD COLUMN sede_id BIGINT NULL AFTER cliente_id;

-- Agregar índice para mejorar rendimiento de consultas por sede
ALTER TABLE abonos
ADD INDEX idx_abono_sede (sede_id);

-- Agregar foreign key constraint
ALTER TABLE abonos
ADD CONSTRAINT fk_abono_sede
FOREIGN KEY (sede_id) REFERENCES sedes(id);

-- ============================================================
-- 🔄 MIGRACIÓN DE DATOS EXISTENTES (OPCIONAL)
-- ============================================================
-- Si tienes abonos existentes, puedes migrarlos usando la sede
-- de la orden asociada. Ejecuta esto DESPUÉS de verificar que
-- todos los abonos tienen una orden asociada:
--
-- UPDATE abonos a
-- INNER JOIN ordenes o ON a.orden_id = o.id
-- SET a.sede_id = o.sede_id
-- WHERE a.sede_id IS NULL;
--
-- ============================================================
-- ✅ HACER LA COLUMNA OBLIGATORIA (DESPUÉS DE MIGRAR DATOS)
-- ============================================================
-- Una vez que todos los abonos tengan sede_id asignado, puedes
-- hacer la columna obligatoria:
--
-- ALTER TABLE abonos
-- MODIFY COLUMN sede_id BIGINT NOT NULL;
--
-- ============================================================

