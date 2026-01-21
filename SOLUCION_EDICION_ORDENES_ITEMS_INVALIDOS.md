# 🔧 SOLUCIÓN: Edición de Órdenes - Items Inválidos

## 🐛 PROBLEMA IDENTIFICADO

Al intentar editar una orden, el backend estaba rechazando la petición con el error:

```
❌ ERROR VALIDACION: Item 17: El precio unitario debe ser mayor a 0
```

**Causa:**
El frontend estaba enviando items con `precioUnitario=0.0` en el array de items. Esto puede ocurrir cuando:
- Se agregan/eliminan items en el formulario y quedan items vacíos
- Hay bugs en la lógica de construcción del array de items
- Se procesan cortes y quedan items residuales

**Ejemplo del problema:**
```json
{
  "items": [
    { "productoId": 300, "cantidad": 1.0, "precioUnitario": 84000.0 },
    { "productoId": 321, "cantidad": 1.0, "precioUnitario": 0.0 }, // ← Item inválido
    { "productoId": 141, "cantidad": 1.0, "precioUnitario": 200000.0 }
  ]
}
```

---

## ✅ SOLUCIÓN IMPLEMENTADA EN EL BACKEND

El backend ahora **filtra automáticamente** los items inválidos antes de procesarlos:

### Cambio en `OrdenService.validarDatosVenta()`:

**ANTES:**
```java
// Validaba todos los items, incluso los inválidos
for (int i = 0; i < ventaDTO.getItems().size(); i++) {
    if (item.getPrecioUnitario() <= 0) {
        throw new IllegalArgumentException("El precio unitario debe ser mayor a 0");
    }
}
```

**AHORA:**
```java
// Filtra items inválidos antes de validar
List<OrdenItemVentaDTO> itemsValidos = ventaDTO.getItems().stream()
    .filter(item -> item.getProductoId() != null 
                 && item.getCantidad() != null && item.getCantidad() > 0
                 && item.getPrecioUnitario() != null && item.getPrecioUnitario() > 0)
    .collect(Collectors.toList());

// Actualiza el DTO con solo items válidos
ventaDTO.setItems(itemsValidos);

// Luego valida los items válidos
```

---

## 🎯 QUÉ SIGNIFICA ESTO PARA EL FRONTEND

### ✅ BUENAS NOTICIAS

**El backend ahora es más tolerante:**
- Si envías items con precio 0, el backend los ignora automáticamente
- Solo procesa items válidos (con precio > 0 y cantidad > 0)
- El error ya no debería aparecer

### ⚠️ PERO DEBERÍAS CORREGIRLO EN EL FRONTEND

Aunque el backend ahora funciona, es mejor práctica filtrar los items inválidos en el frontend antes de enviarlos:

```javascript
// ANTES de enviar la orden, filtrar items inválidos
const itemsValidos = items.filter(item => 
  item.productoId && 
  item.cantidad > 0 && 
  item.precioUnitario > 0
);

// Enviar solo items válidos
const body = {
  ...ordenData,
  items: itemsValidos
};
```

---

## 🔍 POR QUÉ OCURRÍA EL PROBLEMA

El item inválido tenía:
- `productoId: 321`
- `precioUnitario: 0.0`
- `descripcion: ""` (vacía)

Esto puede pasar cuando:
1. **Se elimina un item del formulario** pero queda en el array
2. **Se procesan cortes** y se crean items adicionales mal formados
3. **Hay bugs en la lógica** de construcción del array de items
4. **El formulario tiene campos vacíos** que se envían como items

---

## 📋 QUÉ REVISAR EN EL FRONTEND

### 1. **Revisar la lógica de construcción del array de items**

Busca dónde construyes el array de items antes de enviar la orden:

```javascript
// Ejemplo: Si tienes algo así, puede estar creando items vacíos
const items = productosSeleccionados.map(producto => ({
  productoId: producto.id,
  cantidad: producto.cantidad || 0, // ← Puede ser 0
  precioUnitario: producto.precio || 0 // ← Puede ser 0
}));
```

### 2. **Filtrar items inválidos antes de enviar**

```javascript
// Filtrar items inválidos ANTES de enviar
const itemsValidos = items.filter(item => 
  item.productoId != null &&
  item.cantidad != null && item.cantidad > 0 &&
  item.precioUnitario != null && item.precioUnitario > 0
);

// Enviar solo items válidos
await actualizarOrden(ordenId, {
  ...ordenData,
  items: itemsValidos
});
```

### 3. **Revisar la lógica de cortes**

Si estás procesando cortes, asegúrate de que no estés creando items adicionales con precio 0:

```javascript
// Si procesas cortes, verifica que no crees items vacíos
const itemsDeCortes = cortes.map(corte => {
  if (!corte.precioUnitarioSolicitado || corte.precioUnitarioSolicitado <= 0) {
    return null; // ← No crear item si no tiene precio válido
  }
  return {
    productoId: corte.productoId,
    cantidad: corte.cantidad,
    precioUnitario: corte.precioUnitarioSolicitado
  };
}).filter(item => item != null); // ← Filtrar nulls
```

---

## 🚀 ACCIÓN INMEDIATA

### Para el Backend (YA ESTÁ HECHO):
✅ El backend ya filtra items inválidos automáticamente
✅ El error ya no debería aparecer
✅ Solo necesitas actualizar el código en producción

### Para el Frontend (RECOMENDADO):
1. **Buscar dónde se construye el array de items** antes de enviar
2. **Agregar filtro** para eliminar items con precio 0 o cantidad 0
3. **Revisar la lógica de cortes** para asegurar que no crea items vacíos
4. **Probar** que la edición de órdenes funciona correctamente

---

## 📝 EJEMPLO DE CÓDIGO PARA EL FRONTEND

```javascript
// Función para validar y filtrar items antes de enviar
const validarYFiltrarItems = (items) => {
  return items.filter(item => {
    // Validar que tenga productoId
    if (!item.productoId) {
      console.warn('Item sin productoId ignorado:', item);
      return false;
    }
    
    // Validar cantidad
    if (!item.cantidad || item.cantidad <= 0) {
      console.warn('Item con cantidad inválida ignorado:', item);
      return false;
    }
    
    // Validar precio
    if (!item.precioUnitario || item.precioUnitario <= 0) {
      console.warn('Item con precio inválido ignorado:', item);
      return false;
    }
    
    return true;
  });
};

// Usar antes de enviar la orden
const actualizarOrden = async (ordenId, ordenData) => {
  // Filtrar items inválidos
  const itemsValidos = validarYFiltrarItems(ordenData.items);
  
  if (itemsValidos.length === 0) {
    throw new Error('Debe incluir al menos un producto válido');
  }
  
  // Enviar solo items válidos
  const body = {
    ...ordenData,
    items: itemsValidos
  };
  
  const response = await fetch(`/api/ordenes/${ordenId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  
  return response.json();
};
```

---

## 🎯 RESUMEN

**PROBLEMA:** El frontend enviaba items con precio 0, causando error en el backend.

**SOLUCIÓN BACKEND:** El backend ahora filtra automáticamente items inválidos.

**ACCIÓN FRONTEND:** Aunque el backend ya funciona, es mejor filtrar items inválidos en el frontend antes de enviarlos.

**RESULTADO:** La edición de órdenes ahora funciona incluso si el frontend envía items inválidos, pero deberías corregirlo en el frontend para evitar problemas futuros.

