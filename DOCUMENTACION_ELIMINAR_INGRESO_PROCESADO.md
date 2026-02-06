# 🗑️ Documentación: Eliminación de Ingresos Procesados

## ✅ Problema Resuelto

**Situación:** Dos empleados registran y procesan el mismo ingreso, causando que el inventario se aumente dos veces (duplicación).

**Solución:** Ahora se puede eliminar un ingreso procesado. El sistema automáticamente revierte el inventario antes de eliminarlo.

---

## 🎯 Cambios Implementados

### 1. Eliminación Automática con Reversión de Inventario

El endpoint `DELETE /api/ingresos/{id}` ahora:
- ✅ **Permite eliminar ingresos procesados**
- ✅ **Revierte automáticamente el inventario** antes de eliminar
- ✅ **Resta las cantidades** que se sumaron al procesar
- ✅ **Mantiene la integridad del inventario**

### 2. Nuevo Endpoint: Desprocesar Sin Eliminar

Se agregó `PUT /api/ingresos/{id}/desprocesar` para:
- ✅ Revertir el inventario **sin eliminar** el ingreso
- ✅ Útil para corregir errores manteniendo el historial
- ✅ Permite reprocesar después si es necesario

---

## 📋 Endpoints Disponibles

### 1. Eliminar Ingreso (con reversión automática)

**Método:** `DELETE`  
**URL:** `/api/ingresos/{id}`

**Comportamiento:**
- Si el ingreso **NO está procesado**: Se elimina directamente
- Si el ingreso **SÍ está procesado**: 
  1. Primero revierte el inventario (resta las cantidades)
  2. Luego elimina el ingreso

**Ejemplo de uso:**
```javascript
// Frontend
await fetch(`/api/ingresos/${ingresoId}`, {
  method: 'DELETE'
});
```

**Respuestas:**

✅ **204 No Content** - Ingreso eliminado correctamente
```json
// Sin cuerpo de respuesta
```

❌ **404 Not Found** - Ingreso no encontrado
```json
{
  "error": "Ingreso no encontrado"
}
```

❌ **500 Internal Server Error** - Error al revertir inventario
```json
{
  "error": "Error al revertir inventario: ..."
}
```

---

### 2. Desprocesar Ingreso (sin eliminar)

**Método:** `PUT`  
**URL:** `/api/ingresos/{id}/desprocesar`

**Comportamiento:**
- Revierte el inventario (resta las cantidades)
- Marca el ingreso como `procesado = false`
- **NO elimina** el ingreso (mantiene el historial)

**Ejemplo de uso:**
```javascript
// Frontend
await fetch(`/api/ingresos/${ingresoId}/desprocesar`, {
  method: 'PUT'
});
```

**Respuestas:**

✅ **200 OK** - Ingreso desprocesado correctamente
```json
{
  "mensaje": "Ingreso desprocesado correctamente. El inventario ha sido revertido.",
  "ingreso": {
    "id": 123,
    "procesado": false,
    ...
  }
}
```

❌ **400 Bad Request** - El ingreso no está procesado
```json
{
  "error": "Intento de desprocesar un ingreso que no está procesado"
}
```

❌ **404 Not Found** - Ingreso no encontrado
```json
{
  "error": "Ingreso no encontrado"
}
```

---

## ⚠️ Consideraciones Importantes

### 1. Reversión de Inventario

✅ **Lo que SÍ se revierte:**
- Las **cantidades** del inventario se restan correctamente
- Si un producto tenía 100 unidades y se ingresaron 50, al eliminar vuelve a 100

⚠️ **Lo que NO se revierte:**
- El **costo del producto** NO se revierte automáticamente
- El costo es un promedio ponderado calculado desde múltiples ingresos
- Si necesitas recalcular el costo, debes hacerlo manualmente o mediante un proceso de recálculo global

### 2. Protección contra Cantidades Negativas

El sistema protege contra inventarios negativos:
- Si al revertir el inventario quedaría negativo, se establece en **0**
- Se registra un warning en los logs para auditoría

### 3. Logs y Auditoría

El sistema registra en los logs:
- ✅ Cuando se elimina un ingreso procesado
- ✅ Cuando se revierte el inventario
- ⚠️ Advertencias si el inventario quedaría negativo
- ⚠️ Advertencias si no se encuentra inventario para un producto

---

## 🔄 Flujo de Corrección de Duplicación

### Escenario: Ingreso Duplicado

1. **Situación inicial:**
   - Empleado A registra y procesa ingreso ID: 100
   - Empleado B registra y procesa el mismo ingreso (duplicado) ID: 101
   - Resultado: Inventario aumentado **dos veces** ❌

2. **Corrección:**
   ```javascript
   // Opción 1: Eliminar el ingreso duplicado (recomendado)
   DELETE /api/ingresos/101
   // → Automáticamente revierte el inventario y elimina el registro
   
   // Opción 2: Desprocesar sin eliminar (si quieres mantener historial)
   PUT /api/ingresos/101/desprocesar
   // → Revierte el inventario pero mantiene el registro
   ```

3. **Resultado:**
   - ✅ Inventario corregido (cantidades revertidas)
   - ✅ Ingreso duplicado eliminado o desprocesado
   - ✅ Sistema consistente

---

## 📝 Código de Ejemplo para el Frontend

### Función para Eliminar Ingreso

```typescript
// IngresosService.ts
export const eliminarIngreso = async (id: number): Promise<void> => {
  const response = await fetch(`${API_URL}/ingresos/${id}`, {
    method: 'DELETE',
  });

  if (!response.ok) {
    if (response.status === 404) {
      throw new Error('Ingreso no encontrado');
    }
    const error = await response.json();
    throw new Error(error.error || 'Error al eliminar el ingreso');
  }
};
```

### Función para Desprocesar Ingreso

```typescript
// IngresosService.ts
export const desprocesarIngreso = async (id: number): Promise<Ingreso> => {
  const response = await fetch(`${API_URL}/ingresos/${id}/desprocesar`, {
    method: 'PUT',
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al desprocesar el ingreso');
  }

  const result = await response.json();
  return result.ingreso;
};
```

### Uso en Componente React

```typescript
// Ejemplo: Eliminar ingreso duplicado
const handleEliminarIngresoDuplicado = async (ingresoId: number) => {
  try {
    await eliminarIngreso(ingresoId);
    toast.success('Ingreso eliminado correctamente. El inventario ha sido revertido.');
    // Recargar lista de ingresos
    cargarIngresos();
  } catch (error) {
    toast.error(error.message);
  }
};

// Ejemplo: Desprocesar sin eliminar
const handleDesprocesarIngreso = async (ingresoId: number) => {
  try {
    await desprocesarIngreso(ingresoId);
    toast.success('Ingreso desprocesado. El inventario ha sido revertido.');
    // Recargar lista de ingresos
    cargarIngresos();
  } catch (error) {
    toast.error(error.message);
  }
};
```

---

## ✅ Resumen de Cambios

### Antes
- ❌ No se podía eliminar un ingreso procesado
- ❌ Error: "No se puede eliminar un ingreso ya procesado"
- ❌ No había forma de corregir duplicaciones

### Ahora
- ✅ Se puede eliminar un ingreso procesado
- ✅ El inventario se revierte automáticamente
- ✅ Nuevo endpoint para desprocesar sin eliminar
- ✅ Logs y auditoría mejorados
- ✅ Protección contra inventarios negativos

---

## 🧪 Casos de Prueba Recomendados

1. ✅ Eliminar un ingreso **no procesado** → debe funcionar normalmente
2. ✅ Eliminar un ingreso **procesado** → debe revertir inventario y eliminar
3. ✅ Desprocesar un ingreso **procesado** → debe revertir inventario y marcar como no procesado
4. ✅ Intentar desprocesar un ingreso **no procesado** → debe retornar error
5. ✅ Eliminar un ingreso procesado con productos que **no tienen inventario** → debe manejar el error correctamente
6. ✅ Eliminar un ingreso procesado donde el inventario **quedaría negativo** → debe establecer en 0 y registrar warning

---

## 📞 Soporte

Si encuentras algún problema o comportamiento inesperado, contacta al equipo de backend con:
- El ID del ingreso
- El estado del ingreso (procesado/no procesado)
- El error recibido (si aplica)
- Los logs del servidor (si están disponibles)

