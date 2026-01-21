# 🔄 ENDPOINT BATCH PARA ACTUALIZAR DETALLES DE TRASLADOS

## 🎯 Problema Resuelto

Cuando el frontend hace múltiples cambios simultáneos (agregar producto + cambiar cantidad + eliminar producto), solo se efectuaba uno de los cambios debido a problemas de concurrencia.

**Solución:** Nuevo endpoint batch que procesa todos los cambios en una **sola transacción atómica**.

---

## 📍 Nuevo Endpoint

```
PUT /api/traslados/{trasladoId}/detalles/batch
```

### Características:
- ✅ **Transacción atómica**: Todos los cambios se aplican juntos o ninguno
- ✅ **Sin conflictos de concurrencia**: Procesa todo en una sola operación
- ✅ **Inventario actualizado correctamente**: Maneja todos los ajustes de inventario
- ✅ **Orden de procesamiento**: Elimina → Actualiza → Crea (para evitar conflictos)

---

## 📝 Estructura del Body

```json
{
  "crear": [
    {
      "productoId": 1,
      "cantidad": 10
    },
    {
      "productoId": 2,
      "cantidad": 5
    }
  ],
  "actualizar": [
    {
      "detalleId": 5,
      "cantidad": 15
    },
    {
      "detalleId": 7,
      "productoId": 3,
      "cantidad": 20
    }
  ],
  "eliminar": [3, 4]
}
```

### Campos:

#### `crear` (opcional)
Array de objetos con detalles nuevos a crear:
- `productoId` (obligatorio): ID del producto
- `cantidad` (obligatorio): Cantidad a trasladar (debe ser >= 1)

#### `actualizar` (opcional)
Array de objetos con detalles existentes a actualizar:
- `detalleId` (obligatorio): ID del detalle a actualizar
- `productoId` (opcional): Si se envía, cambia el producto del detalle
- `cantidad` (opcional): Si se envía, cambia la cantidad del detalle

**Nota:** Si cambias el producto, la cantidad se actualiza automáticamente si no se especifica.

#### `eliminar` (opcional)
Array de IDs de detalles a eliminar.

---

## 🔄 Cómo Funciona

### Orden de Procesamiento:

1. **ELIMINAR** detalles primero
   - Revierte el inventario de los detalles eliminados
   - Devuelve stock a sede origen
   - Resta stock de sede destino

2. **ACTUALIZAR** detalles existentes
   - Si cambias el producto: revierte inventario del anterior y aplica el nuevo
   - Si cambias la cantidad: ajusta inventario por la diferencia

3. **CREAR** nuevos detalles
   - Aplica inventario para los nuevos detalles
   - Resta de sede origen
   - Suma a sede destino

### Validaciones:

- ✅ El traslado debe existir
- ✅ Los detalles a actualizar/eliminar deben pertenecer al traslado
- ✅ Las cantidades deben ser >= 1
- ✅ El stock debe ser suficiente en sede origen
- ✅ Los productos deben existir

---

## 📋 Ejemplos de Uso

### Ejemplo 1: Agregar producto y cambiar cantidad de otro

```json
PUT /api/traslados/5/detalles/batch

{
  "crear": [
    { "productoId": 10, "cantidad": 5 }
  ],
  "actualizar": [
    { "detalleId": 3, "cantidad": 15 }
  ]
}
```

**Resultado:**
- Se crea un nuevo detalle con producto 10, cantidad 5
- Se actualiza el detalle 3 cambiando su cantidad a 15
- El inventario se ajusta correctamente para ambos cambios

---

### Ejemplo 2: Eliminar un producto y agregar otro

```json
PUT /api/traslados/5/detalles/batch

{
  "eliminar": [2],
  "crear": [
    { "productoId": 20, "cantidad": 8 }
  ]
}
```

**Resultado:**
- Se elimina el detalle 2 (se revierte su inventario)
- Se crea un nuevo detalle con producto 20, cantidad 8
- El inventario se ajusta correctamente para ambos cambios

---

### Ejemplo 3: Cambiar producto y cantidad de un detalle

```json
PUT /api/traslados/5/detalles/batch

{
  "actualizar": [
    {
      "detalleId": 4,
      "productoId": 30,
      "cantidad": 12
    }
  ]
}
```

**Resultado:**
- Se revierte el inventario del producto anterior del detalle 4
- Se aplica el inventario del nuevo producto 30 con cantidad 12
- El inventario se ajusta correctamente

---

### Ejemplo 4: Solo cambiar cantidad (sin cambiar producto)

```json
PUT /api/traslados/5/detalles/batch

{
  "actualizar": [
    { "detalleId": 6, "cantidad": 20 }
  ]
}
```

**Resultado:**
- Se ajusta el inventario solo por la diferencia de cantidad
- Si la cantidad anterior era 10 y la nueva es 20, se ajusta +10

---

## ✅ Respuesta Exitosa

```json
HTTP 200 OK

[
  {
    "id": 1,
    "producto": { "id": 1, ... },
    "cantidad": 10
  },
  {
    "id": 2,
    "producto": { "id": 2, ... },
    "cantidad": 5
  },
  ...
]
```

Retorna la lista completa de detalles del traslado después de aplicar todos los cambios.

---

## ❌ Errores Posibles

### Error 400: Validación fallida

```json
{
  "error": "La cantidad debe ser >= 1"
}
```

### Error 400: Detalle no pertenece al traslado

```json
{
  "error": "El detalle 5 no pertenece al traslado 3"
}
```

### Error 400: Stock insuficiente

```json
{
  "error": "Stock insuficiente en sede origen. Disponible: 5, ajuste solicitado: -10"
}
```

---

## 🔄 Migración desde Endpoints Individuales

### Antes (múltiples llamadas - problemático):

```javascript
// ❌ Problema: Solo se efectúa uno de los cambios
await Promise.all([
  agregarDetalle(trasladoId, { productoId: 1, cantidad: 10 }),
  actualizarDetalle(trasladoId, detalleId, { cantidad: 15 }),
  eliminarDetalle(trasladoId, otroDetalleId)
]);
```

### Ahora (una sola llamada - correcto):

```javascript
// ✅ Solución: Todos los cambios se aplican juntos
await actualizarDetallesBatch(trasladoId, {
  crear: [
    { productoId: 1, cantidad: 10 }
  ],
  actualizar: [
    { detalleId: detalleId, cantidad: 15 }
  ],
  eliminar: [otroDetalleId]
});
```

---

## 📌 Recomendaciones para el Frontend

1. **Usar el endpoint batch cuando haya múltiples cambios**
   - Agregar producto + cambiar cantidad
   - Eliminar producto + agregar otro
   - Cualquier combinación de cambios

2. **Usar endpoints individuales solo para cambios únicos**
   - Si solo agregas un producto → `POST /detalles`
   - Si solo cambias cantidad de uno → `PUT /detalles/{id}`
   - Si solo eliminas uno → `DELETE /detalles/{id}`

3. **Recopilar todos los cambios antes de enviar**
   - Cuando el usuario hace múltiples cambios en la UI
   - Agruparlos en un solo objeto batch
   - Enviar una sola petición

---

## 🔍 Ejemplo de Implementación Frontend

```javascript
// Función helper para actualizar detalles en batch
async function actualizarDetallesBatch(trasladoId, cambios) {
  const response = await fetch(`/api/traslados/${trasladoId}/detalles/batch`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      crear: cambios.crear || [],
      actualizar: cambios.actualizar || [],
      eliminar: cambios.eliminar || []
    })
  });
  
  if (!response.ok) {
    const error = await response.text();
    throw new Error(error);
  }
  
  return await response.json();
}

// Uso en el componente
const handleGuardarCambios = async () => {
  const cambios = {
    crear: productosNuevos.map(p => ({
      productoId: p.id,
      cantidad: p.cantidad
    })),
    actualizar: productosModificados.map(p => ({
      detalleId: p.detalleId,
      cantidad: p.cantidad,
      productoId: p.productoId !== p.productoIdAnterior ? p.productoId : undefined
    })),
    eliminar: productosEliminados.map(p => p.detalleId)
  };
  
  try {
    const detallesActualizados = await actualizarDetallesBatch(trasladoId, cambios);
    // Actualizar estado con los detalles retornados
    setDetalles(detallesActualizados);
  } catch (error) {
    console.error('Error al guardar cambios:', error);
    alert('Error al guardar cambios: ' + error.message);
  }
};
```

---

## ✅ Ventajas del Endpoint Batch

1. **Atomicidad**: Todos los cambios se aplican juntos o ninguno
2. **Sin conflictos**: No hay problemas de concurrencia
3. **Inventario correcto**: Todos los ajustes se hacen en el orden correcto
4. **Mejor rendimiento**: Una sola transacción en lugar de múltiples
5. **Más simple**: El frontend solo necesita hacer una llamada

---

## 📝 Notas Técnicas

- El endpoint usa `@Transactional` para garantizar atomicidad
- El orden de procesamiento (eliminar → actualizar → crear) evita conflictos
- Todos los ajustes de inventario se validan antes de aplicar cambios
- Si cualquier cambio falla, toda la transacción se revierte

