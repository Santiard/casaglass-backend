# 📋 CAMBIOS EN FACTURA: ELIMINACIÓN DEL CAMPO DESCUENTOS

**Fecha:** 2025-01-XX  
**Versión:** 1.0  
**Tipo:** Breaking Change

---

## 🎯 RESUMEN DE CAMBIOS

Se ha eliminado el campo `descuentos` de la entidad `Factura` y todos sus DTOs relacionados. Este campo no se estaba utilizando en la práctica y su eliminación simplifica el modelo de datos.

---

## 📝 CAMBIOS EN EL BACKEND

### 1. Entidad Factura (`Factura.java`)
- ❌ **Eliminado:** Campo `descuentos` (Double)
- ✅ **Mantenido:** Todos los demás campos (subtotal, iva, retencionFuente, total, etc.)

### 2. DTOs

#### FacturaCreateDTO
- ❌ **Eliminado:** Campo `descuentos` (Double, default: 0.0)
- ✅ **Mantenido:** Todos los demás campos

#### FacturaTablaDTO
- ❌ **Eliminado:** Campo `descuentos` (Double)
- ✅ **Mantenido:** Todos los demás campos

### 3. Servicio (`FacturaService.java`)
- ❌ **Eliminado:** Todas las asignaciones y cálculos relacionados con `descuentos`
- ✅ **Actualizado:** Los cálculos financieros ahora usan directamente el `total` de la orden sin restar descuentos

### 4. Controlador (`OrdenController.java`)
- ❌ **Eliminado:** Asignación de `descuentos` al crear factura automática

---

## 🔧 CAMBIOS EN EL FRONTEND

### ⚠️ ACCIÓN REQUERIDA: Actualizar payloads y componentes

#### 1. **Eliminar campo `descuentos` del payload de creación**

**ANTES:**
```javascript
{
  ordenId: Number,
  fecha: String,
  subtotal: Number,
  descuentos: Number,        // ❌ ELIMINAR ESTE CAMPO
  iva: Number,
  retencionFuente: Number,
  formaPago: String,
  observaciones: String,
  clienteId: Number
}
```

**DESPUÉS:**
```javascript
{
  ordenId: Number,
  fecha: String,
  subtotal: Number,
  // descuentos: Number,    // ❌ YA NO SE ENVÍA
  iva: Number,
  retencionFuente: Number,
  formaPago: String,
  observaciones: String,
  clienteId: Number,
  numeroFactura: String     // ✅ NUEVO: Opcional, si no se envía el backend lo genera
}
```

#### 2. **Archivos a actualizar:**

##### A) Facturación Simple
**Archivo:** `src/pages/OrdenesPage.jsx` (línea ~207)
**Función:** `crearFactura(facturaPayload)`

**Cambios necesarios:**
```javascript
// ❌ ELIMINAR esta línea del payload:
descuentos: 0.0,  // o cualquier valor que estés enviando

// ✅ El payload ahora NO debe incluir descuentos
```

##### B) Facturación Múltiple
**Archivo:** `src/modals/FacturarMultiplesOrdenesModal.jsx` (línea ~633)

**Cambios necesarios:**
```javascript
// ❌ ELIMINAR esta línea del payload:
descuentos: 0.0,  // o cualquier valor que estés enviando

// ✅ El payload ahora NO debe incluir descuentos
```

#### 3. **Actualizar componentes de visualización**

Si tienes componentes que muestran facturas en tablas o formularios, elimina las referencias al campo `descuentos`:

**Ejemplo de tabla:**
```jsx
// ❌ ELIMINAR esta columna:
<TableCell>Descuentos</TableCell>
<TableCell>{factura.descuentos}</TableCell>

// ✅ La tabla ya no mostrará descuentos
```

**Ejemplo de formulario:**
```jsx
// ❌ ELIMINAR este campo:
<TextField
  label="Descuentos"
  value={formData.descuentos}
  onChange={(e) => setFormData({...formData, descuentos: e.target.value})}
/>

// ✅ El formulario ya no incluirá descuentos
```

#### 4. **Actualizar servicios**

**Archivo:** `src/services/FacturasService.js`

**Cambios necesarios:**
```javascript
// ❌ ELIMINAR descuentos del payload en las funciones:
// - crearFactura()
// - actualizarFactura() (si existe)

// Ejemplo ANTES:
const facturaPayload = {
  ordenId: orden.id,
  subtotal: subtotal,
  descuentos: 0.0,  // ❌ ELIMINAR
  iva: iva,
  // ...
};

// Ejemplo DESPUÉS:
const facturaPayload = {
  ordenId: orden.id,
  subtotal: subtotal,
  // descuentos ya no se envía
  iva: iva,
  // ...
};
```

#### 5. **Actualizar tipos TypeScript (si aplica)**

Si usas TypeScript, actualiza las interfaces:

```typescript
// ❌ ANTES:
interface FacturaCreateDTO {
  ordenId: number;
  subtotal: number;
  descuentos: number;  // ❌ ELIMINAR
  iva: number;
  // ...
}

// ✅ DESPUÉS:
interface FacturaCreateDTO {
  ordenId: number;
  subtotal: number;
  // descuentos eliminado
  iva: number;
  // ...
}
```

```typescript
// ❌ ANTES:
interface Factura {
  id: number;
  subtotal: number;
  descuentos: number;  // ❌ ELIMINAR
  iva: number;
  // ...
}

// ✅ DESPUÉS:
interface Factura {
  id: number;
  subtotal: number;
  // descuentos eliminado
  iva: number;
  // ...
}
```

---

## 🗄️ CAMBIOS EN LA BASE DE DATOS

### Script SQL para eliminar la columna

Ejecuta el siguiente script SQL en tu base de datos MariaDB (usando DBeaver o tu cliente SQL preferido):

```sql
-- Eliminar la columna descuentos de la tabla facturas
ALTER TABLE facturas
DROP COLUMN descuentos;
```

**⚠️ IMPORTANTE:**
- Haz un backup de la base de datos antes de ejecutar el script
- Verifica que no haya datos importantes en la columna `descuentos` antes de eliminarla
- El script está disponible en: `scripts/eliminar_columna_descuentos_facturas.sql`

---

## ✅ NUEVO CAMPO: numeroFactura

### Soporte para número de factura personalizado

El backend ahora acepta el campo `numeroFactura` como opcional en el payload de creación:

```javascript
{
  ordenId: Number,
  fecha: String,
  subtotal: Number,
  iva: Number,
  retencionFuente: Number,
  formaPago: String,
  observaciones: String,
  clienteId: Number,
  numeroFactura: String  // ✅ NUEVO: Opcional (acepta cualquier String)
}
```

**Tipo de dato:** `String` (texto libre)

**Comportamiento:**
- Si se envía `numeroFactura`, el backend lo usa directamente (acepta cualquier formato de texto)
- Si NO se envía `numeroFactura` (o viene `null`/`undefined`/`""`), el backend genera un número secuencial automáticamente

**Ejemplos de uso:**
```javascript
// Factura con número personalizado (formato factura electrónica)
const facturaPayload = {
  ordenId: 125,
  subtotal: 1827731.09,
  iva: 347268.91,
  numeroFactura: "FE-2025-001"  // ✅ String personalizado
};

// Factura con número personalizado (formato numérico simple)
const facturaPayload = {
  ordenId: 125,
  subtotal: 1827731.09,
  iva: 347268.91,
  numeroFactura: "12345"  // ✅ String numérico
};

// Factura con número personalizado (formato con prefijo)
const facturaPayload = {
  ordenId: 125,
  subtotal: 1827731.09,
  iva: 347268.91,
  numeroFactura: "FAC-2025-0001"  // ✅ String con formato personalizado
};

// Factura con número automático (no se envía numeroFactura)
const facturaPayload = {
  ordenId: 125,
  subtotal: 1827731.09,
  iva: 347268.91,
  // numeroFactura no se envía, el backend genera automáticamente (ej: "1", "2", "3", ...)
};
```

**Nota importante:** El campo acepta cualquier `String`, por lo que puedes usar el formato que necesites (factura electrónica, manual, con prefijos, etc.).

---

## 🧪 TESTING

### Checklist de pruebas

- [ ] Crear factura simple sin `descuentos` en el payload
- [ ] Crear factura múltiple sin `descuentos` en el payload
- [ ] Verificar que las tablas de facturas no muestren columna `descuentos`
- [ ] Verificar que los formularios no incluyan campo `descuentos`
- [ ] Probar creación de factura con `numeroFactura` personalizado
- [ ] Probar creación de factura sin `numeroFactura` (debe generarse automáticamente)
- [ ] Verificar que las facturas existentes sigan funcionando correctamente

---

## 📊 IMPACTO

### Endpoints afectados

1. **POST /api/facturas** - Crear factura
   - ✅ Acepta payload sin `descuentos`
   - ✅ Acepta `numeroFactura` opcional

2. **PUT /api/facturas/{id}** - Actualizar factura
   - ✅ Ya no acepta `descuentos` en el payload

3. **GET /api/facturas** - Listar facturas
   - ✅ Respuesta ya no incluye `descuentos`

4. **GET /api/facturas/tabla** - Listar facturas para tabla
   - ✅ Respuesta ya no incluye `descuentos`

---

## 🔄 MIGRACIÓN

### Pasos para migrar

1. **Backend:**
   - ✅ Código actualizado (ya completado)
   - ⏳ Ejecutar script SQL para eliminar columna

2. **Frontend:**
   - ⏳ Eliminar `descuentos` de todos los payloads
   - ⏳ Eliminar `descuentos` de componentes de visualización
   - ⏳ Actualizar tipos/interfaces TypeScript
   - ⏳ Probar creación y visualización de facturas

3. **Base de datos:**
   - ⏳ Ejecutar script SQL (ver sección anterior)

---

## 📞 SOPORTE

Si encuentras algún problema durante la migración:

1. Verifica que el backend esté actualizado
2. Verifica que el script SQL se haya ejecutado correctamente
3. Revisa los logs del backend para errores relacionados con `descuentos`
4. Asegúrate de que todos los payloads del frontend no incluyan `descuentos`

---

## 📝 NOTAS ADICIONALES

- El campo `descuentos` sigue existiendo en la entidad `Orden` (no se eliminó de ahí)
- Los cálculos financieros de facturas ahora usan directamente el `total` de la orden
- La eliminación de `descuentos` simplifica el modelo y reduce la complejidad de los cálculos

---

**Última actualización:** 2025-01-XX  
**Versión del documento:** 1.0

