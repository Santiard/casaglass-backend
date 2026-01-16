# 🔧 GUÍA PASO A PASO: ELIMINAR COLUMNA DESCUENTOS EN PRODUCCIÓN

## ⚠️ PROBLEMA IDENTIFICADO

El error `Unknown column 'descuentos' in 'CHECK'` indica que existe una **restricción CHECK** en la tabla `ordenes` que hace referencia a la columna `descuentos`. Esta restricción debe eliminarse **ANTES** de eliminar la columna.

---

## 📋 PASOS PARA RESOLVER

### Paso 1: Identificar la restricción CHECK

Ejecuta este comando para ver todas las restricciones de la tabla:

```sql
USE casaglassDB;

SHOW CREATE TABLE ordenes;
```

Busca en el resultado una línea que contenga algo como:
```sql
CONSTRAINT `check_suma_metodos_pago_orden` CHECK (... descuentos ...)
```

O ejecuta:

```sql
SELECT 
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE,
    TABLE_NAME
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ordenes'
  AND CONSTRAINT_TYPE = 'CHECK';
```

---

### Paso 2: Eliminar la restricción CHECK

Una vez identificada la restricción (probablemente se llama `check_suma_metodos_pago_orden`), elimínala:

```sql
ALTER TABLE ordenes
DROP CONSTRAINT check_suma_metodos_pago_orden;
```

**Si el nombre es diferente**, usa el nombre exacto que apareció en `SHOW CREATE TABLE`.

**Si el comando falla** porque MariaDB no soporta `DROP CONSTRAINT` directamente, intenta:

```sql
ALTER TABLE ordenes
DROP CHECK check_suma_metodos_pago_orden;
```

---

### Paso 3: Eliminar la columna descuentos

Ahora que la restricción fue eliminada, puedes eliminar la columna:

```sql
ALTER TABLE ordenes
DROP COLUMN descuentos;
```

---

### Paso 4: Recrear la restricción CHECK (sin descuentos)

La restricción original verificaba:
```
monto_efectivo + monto_transferencia + monto_cheque = total - descuentos
```

Ahora debe verificarse:
```
monto_efectivo + monto_transferencia + monto_cheque = total
```

Recrea la restricción:

```sql
ALTER TABLE ordenes
ADD CONSTRAINT check_suma_metodos_pago_orden 
CHECK (
    credito = true OR 
    monto_efectivo + monto_transferencia + monto_cheque = total OR
    (monto_efectivo = 0 AND monto_transferencia = 0 AND monto_cheque = 0)
);
```

Esta restricción permite:
- Órdenes a crédito (no valida)
- Órdenes de contado con métodos de pago que suman el total
- Órdenes existentes con métodos de pago en 0 (compatibilidad)

---

### Paso 5: Verificar

Verifica que todo esté correcto:

```sql
-- Verificar que la columna fue eliminada
SELECT COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ordenes'
  AND COLUMN_NAME = 'descuentos';
-- Debe retornar 0 filas

-- Verificar que la restricción fue recreada
SHOW CREATE TABLE ordenes;
-- Debe mostrar la nueva restricción sin referencia a descuentos
```

---

## 🚨 ALTERNATIVA: Si DROP CONSTRAINT no funciona

Si `DROP CONSTRAINT` no funciona en tu versión de MariaDB, puedes intentar:

### Opción A: Usar ALTER TABLE con MODIFY

```sql
-- Primero, modificar la restricción para que no referencie descuentos
-- (Esto puede requerir recrear la tabla, así que mejor usar la opción B)
```

### Opción B: Eliminar y recrear la restricción manualmente

1. Anota la definición completa de la restricción desde `SHOW CREATE TABLE`
2. Elimínala manualmente editando la definición
3. Recrea la tabla o modifica la restricción

### Opción C: Usar un script más completo

Ejecuta el script `eliminar_columna_descuentos_ordenes_produccion_final.sql` que incluye todos los pasos.

---

## 📝 NOTAS IMPORTANTES

- **Backup:** Siempre haz backup antes de ejecutar estos comandos en producción
- **Horario:** Ejecuta en horario de bajo tráfico
- **Pruebas:** Verifica primero en un ambiente de pruebas si es posible
- **Rollback:** Ten un plan de rollback listo por si algo sale mal

---

## 🔍 VERIFICACIÓN FINAL

Después de ejecutar todos los pasos, verifica:

1. ✅ La columna `descuentos` ya no existe
2. ✅ La restricción CHECK fue recreada sin referencia a `descuentos`
3. ✅ Las órdenes existentes siguen funcionando correctamente
4. ✅ Los nuevos registros validan correctamente con la nueva restricción

---

**Última actualización:** 2025-01-XX

