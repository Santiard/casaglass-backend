# 📋 CAMBIOS EN ORDEN: ELIMINACIÓN DEL CAMPO DESCUENTOS

**Fecha:** 2025-01-XX  
**Versión:** 1.0  
**Tipo:** Breaking Change

---

## 🎯 RESUMEN DE CAMBIOS

Se ha eliminado el campo `descuentos` de la entidad `Orden` y todos sus DTOs relacionados. Este campo no se estaba utilizando en la práctica y su eliminación simplifica el modelo de datos y los cálculos financieros.

---

## 📝 CAMBIOS EN EL BACKEND

### 1. Entidad Orden (`Orden.java`)
- ❌ **Eliminado:** Campo `descuentos` (Double)
- ✅ **Mantenido:** Todos los demás campos (subtotal, iva, retencionFuente, total, etc.)
- ✅ **Actualizado:** Comentarios de cálculo para reflejar que ya no se usan descuentos

### 2. DTOs Actualizados

#### OrdenTablaDTO
- ❌ **Eliminado:** Campo `descuentos` (Double)
- ✅ **Mantenido:** Todos los demás campos

#### OrdenDetalleDTO
- ❌ **Eliminado:** Campo `descuentos` (Double)
- ✅ **Mantenido:** Todos los demás campos

#### OrdenVentaDTO
- ❌ **Eliminado:** Campo `descuentos` (Double, default: 0.0)
- ✅ **Mantenido:** Todos los demás campos

#### OrdenActualizarDTO
- ❌ **Eliminado:** Campo `descuentos` (Double, default: 0.0)
- ✅ **Mantenido:** Todos los demás campos

#### OrdenResponseDTO
- ❌ **Eliminado:** Campo `descuentos` (Double)
- ✅ **Mantenido:** Todos los demás campos

#### CreditoPendienteDTO
- ❌ **Eliminado:** Campo `descuentos` (Double)
- ✅ **Mantenido:** Todos los demás campos

### 3. Servicio (`OrdenService.java`)
- ❌ **Eliminado:** Todas las asignaciones y cálculos relacionados con `descuentos`
- ✅ **Actualizado:** Método `calcularValoresMonetariosOrden()` ahora no recibe parámetro `descuentos`
- ✅ **Actualizado:** Método `calcularRetencionFuente()` ahora no usa descuentos
- ✅ **Actualizado:** Todos los cálculos financieros ahora usan directamente el subtotal sin restar descuentos

### 4. Controladores
- ✅ **Sin cambios necesarios:** Los controladores no tenían lógica específica de descuentos

---

## 🔧 CAMBIOS EN EL FRONTEND

### ⚠️ ACCIÓN REQUERIDA: Actualizar payloads y componentes

#### 1. **Eliminar campo `descuentos` del payload de creación de órdenes**

**ANTES:**
```javascript
{
  fecha: String,
  obra: String,
  descripcion: String,
  venta: Boolean,
  credito: Boolean,
  tieneRetencionFuente: Boolean,
  descuentos: Number,        // ❌ ELIMINAR ESTE CAMPO
  clienteId: Number,
  sedeId: Number,
  trabajadorId: Number,
  items: Array,
  cortes: Array
}
```

**DESPUÉS:**
```javascript
{
  fecha: String,
  obra: String,
  descripcion: String,
  venta: Boolean,
  credito: Boolean,
  tieneRetencionFuente: Boolean,
  // descuentos: Number,    // ❌ YA NO SE ENVÍA
  clienteId: Number,
  sedeId: Number,
  trabajadorId: Number,
  items: Array,
  cortes: Array
}
```

#### 2. **Archivos a actualizar:**

##### A) Crear Orden de Venta
**Archivo:** `src/pages/VentasPage.jsx` o donde se cree la orden
**Endpoint:** `POST /api/ordenes/venta`

**Cambios necesarios:**
```javascript
// ❌ ELIMINAR esta línea del payload:
descuentos: 0.0,  // o cualquier valor que estés enviando

// ✅ El payload ahora NO debe incluir descuentos
```

##### B) Actualizar Orden
**Archivo:** `src/pages/OrdenesPage.jsx` o donde se actualice la orden
**Endpoint:** `PUT /api/ordenes/tabla/{id}` o `PUT /api/ordenes/venta/{id}`

**Cambios necesarios:**
```javascript
// ❌ ELIMINAR esta línea del payload:
descuentos: 0.0,  // o cualquier valor que estés enviando

// ✅ El payload ahora NO debe incluir descuentos
```

#### 3. **Actualizar componentes de visualización**

Si tienes componentes que muestran órdenes en tablas o formularios, elimina las referencias al campo `descuentos`:

**Ejemplo de tabla:**
```jsx
// ❌ ELIMINAR esta columna:
<TableCell>Descuentos</TableCell>
<TableCell>{orden.descuentos}</TableCell>

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

**Ejemplo de detalle de orden:**
```jsx
// ❌ ELIMINAR estas líneas:
<Grid item>
  <Typography>Descuentos:</Typography>
  <Typography>{orden.descuentos}</Typography>
</Grid>

// ✅ El detalle ya no mostrará descuentos
```

#### 4. **Actualizar servicios**

**Archivo:** `src/services/OrdenesService.js`

**Cambios necesarios:**
```javascript
// ❌ ELIMINAR descuentos del payload en las funciones:
// - crearOrdenVenta()
// - actualizarOrden()
// - actualizarOrdenVenta()

// Ejemplo ANTES:
const ordenPayload = {
  fecha: fecha,
  obra: obra,
  descuentos: 0.0,  // ❌ ELIMINAR
  clienteId: clienteId,
  items: items,
  // ...
};

// Ejemplo DESPUÉS:
const ordenPayload = {
  fecha: fecha,
  obra: obra,
  // descuentos ya no se envía
  clienteId: clienteId,
  items: items,
  // ...
};
```

#### 5. **Actualizar tipos TypeScript (si aplica)**

Si usas TypeScript, actualiza las interfaces:

```typescript
// ❌ ANTES:
interface OrdenVentaDTO {
  fecha: string;
  obra: string;
  descuentos: number;  // ❌ ELIMINAR
  clienteId: number;
  items: OrdenItem[];
  // ...
}

// ✅ DESPUÉS:
interface OrdenVentaDTO {
  fecha: string;
  obra: string;
  // descuentos eliminado
  clienteId: number;
  items: OrdenItem[];
  // ...
}
```

```typescript
// ❌ ANTES:
interface Orden {
  id: number;
  subtotal: number;
  descuentos: number;  // ❌ ELIMINAR
  iva: number;
  total: number;
  // ...
}

// ✅ DESPUÉS:
interface Orden {
  id: number;
  subtotal: number;
  // descuentos eliminado
  iva: number;
  total: number;
  // ...
}
```

#### 6. **Actualizar cálculos financieros**

Si tienes cálculos en el frontend que usan descuentos, actualízalos:

**ANTES:**
```javascript
// ❌ Cálculo con descuentos
const total = subtotalFacturado - descuentos;
const baseImponible = subtotalFacturado - descuentos;
```

**DESPUÉS:**
```javascript
// ✅ Cálculo sin descuentos
const total = subtotalFacturado;
const baseImponible = subtotalFacturado;
```

---

## 🗄️ CAMBIOS EN LA BASE DE DATOS

### Script SQL para eliminar la columna

Ejecuta el siguiente script SQL en tu base de datos MariaDB (usando DBeaver o tu cliente SQL preferido):

```sql
-- Eliminar la columna descuentos de la tabla ordenes
ALTER TABLE ordenes
DROP COLUMN descuentos;
```

**⚠️ IMPORTANTE:**
- Haz un backup de la base de datos antes de ejecutar el script
- Verifica que no haya datos importantes en la columna `descuentos` antes de eliminarla
- El script está disponible en: `scripts/eliminar_columna_descuentos_ordenes.sql`

---

## 📊 IMPACTO EN ENDPOINTS

### Endpoints afectados

1. **POST /api/ordenes/venta** - Crear orden de venta
   - ✅ Ya no acepta `descuentos` en el payload

2. **PUT /api/ordenes/venta/{id}** - Actualizar orden de venta
   - ✅ Ya no acepta `descuentos` en el payload

3. **PUT /api/ordenes/tabla/{id}** - Actualizar orden desde tabla
   - ✅ Ya no acepta `descuentos` en el payload

4. **GET /api/ordenes/tabla** - Listar órdenes para tabla
   - ✅ Respuesta ya no incluye `descuentos`

5. **GET /api/ordenes/{id}** - Obtener orden por ID
   - ✅ Respuesta ya no incluye `descuentos`

6. **GET /api/ordenes/{id}/detalle** - Obtener detalle de orden
   - ✅ Respuesta ya no incluye `descuentos`

7. **GET /api/creditos/cliente/{clienteId}/pendientes** - Créditos pendientes
   - ✅ Respuesta ya no incluye `descuentos` en la información de la orden

---

## 🔄 CAMBIOS EN CÁLCULOS FINANCIEROS

### Fórmulas actualizadas

#### Antes (con descuentos):
```
Base imponible = Subtotal facturado - Descuentos
Subtotal sin IVA = Base imponible / 1.19
IVA = Base imponible - Subtotal sin IVA
Total = Subtotal facturado - Descuentos
```

#### Después (sin descuentos):
```
Base imponible = Subtotal facturado
Subtotal sin IVA = Base imponible / 1.19
IVA = Base imponible - Subtotal sin IVA
Total = Subtotal facturado
```

### Ejemplo práctico

**Antes:**
- Subtotal facturado: $1,000,000
- Descuentos: $50,000
- Base imponible: $950,000
- Subtotal sin IVA: $797,479.83
- IVA: $152,520.17
- Total: $950,000

**Después:**
- Subtotal facturado: $1,000,000
- Base imponible: $1,000,000
- Subtotal sin IVA: $840,336.13
- IVA: $159,663.87
- Total: $1,000,000

---

## 🧪 TESTING

### Checklist de pruebas

- [ ] Crear orden de venta sin `descuentos` en el payload
- [ ] Actualizar orden sin `descuentos` en el payload
- [ ] Verificar que las tablas de órdenes no muestren columna `descuentos`
- [ ] Verificar que los formularios no incluyan campo `descuentos`
- [ ] Verificar que los detalles de orden no muestren `descuentos`
- [ ] Verificar que los cálculos financieros funcionen correctamente sin descuentos
- [ ] Verificar que las órdenes existentes sigan funcionando correctamente
- [ ] Verificar que los créditos pendientes no muestren `descuentos`

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
   - ⏳ Actualizar cálculos financieros si los hay
   - ⏳ Probar creación y actualización de órdenes

3. **Base de datos:**
   - ⏳ Ejecutar script SQL (ver sección anterior)

---

## 📞 SOPORTE

Si encuentras algún problema durante la migración:

1. Verifica que el backend esté actualizado
2. Verifica que el script SQL se haya ejecutado correctamente
3. Revisa los logs del backend para errores relacionados con `descuentos`
4. Asegúrate de que todos los payloads del frontend no incluyan `descuentos`
5. Verifica que los cálculos financieros no dependan de descuentos

---

## 📝 NOTAS ADICIONALES

- El campo `descuentos` sigue existiendo en la entidad `ReembolsoVenta` (no se eliminó de ahí, ya que es específico del reembolso)
- Los cálculos financieros de órdenes ahora son más simples y directos
- La eliminación de `descuentos` simplifica el modelo y reduce la complejidad de los cálculos
- Los totales ahora coinciden directamente con la suma de los items

---

## 🔗 RELACIÓN CON CAMBIOS EN FACTURAS

Este cambio está relacionado con la eliminación de `descuentos` en `Factura`. Ambos cambios simplifican el modelo de datos y hacen que los cálculos sean más consistentes entre órdenes y facturas.

**Documentación relacionada:**
- `CAMBIOS_FACTURA_ELIMINAR_DESCUENTOS.md`

---

**Última actualización:** 2025-01-XX  
**Versión del documento:** 1.0

