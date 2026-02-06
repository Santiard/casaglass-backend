# 📋 Documentación: Actualización Parcial de Facturas

## ✅ Endpoint Actualizado

**Método**: `PUT`  
**URL**: `/api/facturas/{id}`  
**Controller**: `FacturaController.actualizarFactura`  
**Service**: `FacturaService.actualizarFactura`

---

## 🎯 Características Implementadas

### ✅ 1. Actualización Parcial
El endpoint ahora soporta **actualización parcial de campos**. Solo se actualizan los campos que vienen en el JSON. Los campos que no se envían se mantienen sin cambios.

### ✅ 2. Soporte Explícito para `numeroFactura`
- El campo `numeroFactura` ahora **SÍ se puede actualizar**.
- Se valida la **unicidad** del número (no puede duplicarse).
- Si intentas usar un número que ya existe en otra factura, retorna error.

### ✅ 3. Payloads Combinados
Puedes enviar múltiples campos en un solo request:
```json
{
  "numeroFactura": "F-000123",
  "clienteId": 5
}
```

---

## 📦 DTO Esperado (`FacturaCreateDTO`)

Todos los campos son **opcionales** (excepto validaciones específicas):

```typescript
interface FacturaCreateDTO {
  // ✅ Número de factura (ahora SÍ se puede actualizar)
  numeroFactura?: string;

  // ✅ Cliente al que se factura
  clienteId?: number;

  // ✅ Fecha de la factura
  fecha?: string; // Formato: "YYYY-MM-DD"

  // ✅ Campos monetarios (solo si quieres actualizarlos)
  subtotal?: number;        // Debe ser > 0 si se envía
  iva?: number;             // Debe ser >= 0 si se envía
  retencionFuente?: number; // Debe ser >= 0 si se envía
  retencionIca?: number;    // Debe ser >= 0 si se envía

  // ✅ Forma de pago
  formaPago?: string;

  // ✅ Observaciones
  observaciones?: string;

  // ⚠️ NO SE USA EN ACTUALIZACIÓN (solo en creación)
  ordenId?: number;
  total?: number;
}
```

---

## 📝 Ejemplos de Uso

### Ejemplo 1: Actualizar solo el número de factura

```javascript
// Frontend
await actualizarFactura(factura.id, {
  numeroFactura: "F-000123"
});
```

**Request:**
```json
PUT /api/facturas/42
{
  "numeroFactura": "F-000123"
}
```

**Respuesta exitosa:**
```json
{
  "mensaje": "Factura actualizada exitosamente",
  "factura": { ... }
}
```

**Respuesta si el número ya existe:**
```json
{
  "error": "Ya existe una factura con el número: F-000123"
}
```

---

### Ejemplo 2: Actualizar solo el cliente

```javascript
// Frontend
await actualizarFactura(factura.id, {
  clienteId: 5
});
```

**Request:**
```json
PUT /api/facturas/42
{
  "clienteId": 5
}
```

**Respuesta si el cliente no existe:**
```json
{
  "error": "Cliente no encontrado con ID: 5"
}
```

---

### Ejemplo 3: Actualizar número y cliente (payload combinado)

```javascript
// Frontend
await actualizarFactura(factura.id, {
  numeroFactura: "F-000123",
  clienteId: 5
});
```

**Request:**
```json
PUT /api/facturas/42
{
  "numeroFactura": "F-000123",
  "clienteId": 5
}
```

---

### Ejemplo 4: Actualizar campos monetarios

```javascript
// Frontend
await actualizarFactura(factura.id, {
  subtotal: 100000,
  iva: 19000,
  retencionFuente: 3500,
  retencionIca: 1000
});
```

**Nota:** Si actualizas `subtotal`, el backend recalcula automáticamente el `iva` si no lo envías. Si envías `iva`, se usa el valor que enviaste.

---

### Ejemplo 5: Actualizar observaciones

```javascript
// Frontend
await actualizarFactura(factura.id, {
  observaciones: "Factura corregida por error administrativo"
});
```

---

## ⚠️ Validaciones y Restricciones

### 1. Estado de la Factura
- ❌ **No se puede actualizar** si la factura está **PAGADA**.
- ❌ **No se puede actualizar** si la factura está **ANULADA**.

**Error esperado:**
```json
{
  "error": "No se puede actualizar una factura pagada"
}
```

### 2. Unicidad de `numeroFactura`
- ✅ El número de factura debe ser **único** en el sistema.
- ✅ Si intentas usar un número que ya existe en otra factura, retorna error.
- ✅ Si envías el mismo número que ya tiene la factura, no hace nada (no error).

**Error esperado:**
```json
{
  "error": "Ya existe una factura con el número: F-000123"
}
```

### 3. Validaciones de Campos Monetarios
- ✅ `subtotal` debe ser **> 0** si se envía.
- ✅ `iva` debe ser **>= 0** si se envía.
- ✅ `retencionFuente` debe ser **>= 0** si se envía.
- ✅ `retencionIca` debe ser **>= 0** si se envía.

**Error esperado:**
```json
{
  "error": "El subtotal debe ser mayor a 0"
}
```

### 4. Cliente
- ✅ Si envías `clienteId`, el cliente debe **existir** en la base de datos.

**Error esperado:**
```json
{
  "error": "Cliente no encontrado con ID: 999"
}
```

---

## 🔄 Comportamiento del Backend

### Actualización Parcial
- ✅ Solo actualiza los campos que vienen en el JSON.
- ✅ Los campos que **no** se envían se mantienen **sin cambios**.
- ✅ No es necesario enviar todos los campos.

### Cálculo Automático de IVA
- Si actualizas `subtotal` y **NO** envías `iva`, el backend **recalcula automáticamente** el IVA desde el subtotal.
- Si envías `iva`, se usa el valor que enviaste.

### Recalculación de Total
- El total se recalcula automáticamente **solo si** se actualizaron campos monetarios (`subtotal`, `iva`, `retencionFuente`, `retencionIca`).
- Si solo actualizas campos no monetarios (`numeroFactura`, `clienteId`, `observaciones`, etc.), el total **no se recalcula**.

---

## 📋 Código de Ejemplo para el Frontend

### Función de Actualización (TypeScript/JavaScript)

```typescript
// FacturasService.ts
export const actualizarFactura = async (
  id: number,
  datos: Partial<FacturaCreateDTO>
): Promise<Factura> => {
  const response = await fetch(`${API_URL}/facturas/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(datos),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al actualizar la factura');
  }

  const result = await response.json();
  return result.factura;
};
```

### Uso en Componente React

```typescript
// Ejemplo: Actualizar solo número de factura
const handleActualizarNumero = async (facturaId: number, nuevoNumero: string) => {
  try {
    await actualizarFactura(facturaId, {
      numeroFactura: nuevoNumero.trim()
    });
    // Mostrar mensaje de éxito
    toast.success('Número de factura actualizado correctamente');
  } catch (error) {
    // Manejar error
    toast.error(error.message);
  }
};

// Ejemplo: Actualizar cliente
const handleActualizarCliente = async (facturaId: number, clienteId: number) => {
  try {
    await actualizarFactura(facturaId, {
      clienteId: clienteId
    });
    // Mostrar mensaje de éxito
    toast.success('Cliente actualizado correctamente');
  } catch (error) {
    // Manejar error
    toast.error(error.message);
  }
};

// Ejemplo: Actualizar ambos
const handleActualizarAmbos = async (
  facturaId: number,
  nuevoNumero: string,
  clienteId: number
) => {
  try {
    await actualizarFactura(facturaId, {
      numeroFactura: nuevoNumero.trim(),
      clienteId: clienteId
    });
    // Mostrar mensaje de éxito
    toast.success('Factura actualizada correctamente');
  } catch (error) {
    // Manejar error
    toast.error(error.message);
  }
};
```

---

## ✅ Resumen de Cambios

### Antes (Comportamiento Anterior)
- ❌ No se podía actualizar `numeroFactura`.
- ❌ Requería enviar todos los campos (o causaba errores).
- ❌ No soportaba actualización parcial real.

### Ahora (Comportamiento Nuevo)
- ✅ Se puede actualizar `numeroFactura` con validación de unicidad.
- ✅ Soporta actualización parcial (solo los campos que envías).
- ✅ Permite payloads combinados.
- ✅ Validaciones claras y mensajes de error descriptivos.

---

## 🧪 Casos de Prueba Recomendados

1. ✅ Actualizar solo `numeroFactura` con un número único.
2. ✅ Intentar actualizar `numeroFactura` con un número que ya existe → debe fallar.
3. ✅ Actualizar solo `clienteId` con un cliente válido.
4. ✅ Intentar actualizar `clienteId` con un cliente inexistente → debe fallar.
5. ✅ Actualizar `numeroFactura` y `clienteId` juntos.
6. ✅ Intentar actualizar una factura PAGADA → debe fallar.
7. ✅ Intentar actualizar una factura ANULADA → debe fallar.
8. ✅ Actualizar solo `observaciones` → debe funcionar sin afectar otros campos.
9. ✅ Actualizar `subtotal` sin enviar `iva` → debe recalcular IVA automáticamente.
10. ✅ Actualizar `subtotal` y `iva` juntos → debe usar el IVA enviado.

---

## 📞 Soporte

Si encuentras algún problema o comportamiento inesperado, contacta al equipo de backend con:
- El ID de la factura.
- El payload que enviaste.
- El error recibido (si aplica).


