# 🔄 Endpoints de Procesamiento de Ingresos - CORREGIDOS

## ✅ **Corrección Aplicada**

**Problema identificado:** Los endpoints de procesamiento usaban `POST` cuando deberían usar `PUT` según las convenciones REST.

**Solución:** Cambiados a `PUT` porque actualizan el estado de recursos existentes.

---

## 🎯 **Endpoints de Procesamiento**

### 1. **Procesar Ingreso Completo**

**Endpoint:** `PUT /api/ingresos/{id}/procesar`

**Descripción:** Procesa completamente un ingreso:
- ✅ Actualiza el inventario en la sede principal
- ✅ Actualiza el costo de los productos
- ✅ Marca el ingreso como `procesado = true`

**¿Qué hace internamente?**
```java
public void procesarInventario(Ingreso ingreso) {
    // 1. Verificar que no esté ya procesado
    if (ingreso.getProcesado()) {
        throw new RuntimeException("El ingreso ya ha sido procesado");
    }

    // 2. Obtener sede principal (ID = 1)
    Sede sedePrincipal = sedeRepository.findById(1L);

    // 3. Para cada detalle del ingreso:
    for (IngresoDetalle detalle : ingreso.getDetalles()) {
        // 3.1 Buscar inventario existente o crear nuevo
        Optional<Inventario> inventarioExistente = inventarioService
            .obtenerPorProductoYSede(producto.getId(), sedePrincipal.getId());

        if (inventarioExistente.isPresent()) {
            // Actualizar cantidad existente
            inventario.setCantidad(inventario.getCantidad() + cantidadIngresada);
        } else {
            // Crear nuevo registro de inventario
            Inventario nuevoInventario = new Inventario();
            nuevoInventario.setProducto(producto);
            nuevoInventario.setSede(sedePrincipal);
            nuevoInventario.setCantidad(cantidadIngresada);
        }

        // 3.2 Actualizar costo del producto si es diferente
        if (!costoActual.equals(nuevoCosto)) {
            producto.setCosto(nuevoCosto);
        }
    }

    // 4. Marcar ingreso como procesado
    ingreso.setProcesado(true);
    ingresoRepository.save(ingreso);
}
```

**Uso en Frontend:**
```javascript
// Procesar un ingreso completo
const procesarIngreso = async (ingresoId) => {
  try {
    const response = await fetch(`/api/ingresos/${ingresoId}/procesar`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    if (response.ok) {
      const message = await response.text();
      console.log('✅', message); // "Inventario procesado correctamente"
      // Actualizar la UI para mostrar que está procesado
    } else {
      const error = await response.text();
      console.error('❌', error);
    }
  } catch (error) {
    console.error('Error de red:', error);
  }
};
```

### 2. **Reprocesar Ingreso**

**Endpoint:** `PUT /api/ingresos/{id}/reprocesar`

**Descripción:** Reprocesa un ingreso que ya fue procesado anteriormente (útil para correcciones).

**¿Qué hace internamente?**
```java
public void reprocesarInventario(Long ingresoId) {
    Ingreso ingreso = ingresoRepository.findByIdWithDetalles(ingresoId);
    
    // Marcar como no procesado temporalmente
    ingreso.setProcesado(false);
    
    // Procesar nuevamente
    procesarInventario(ingreso);
}
```

**Uso en Frontend:**
```javascript
// Reprocesar un ingreso (para correcciones)
const reprocesarIngreso = async (ingresoId) => {
  try {
    const response = await fetch(`/api/ingresos/${ingresoId}/reprocesar`, {
      method: 'PUT'
    });
    
    if (response.ok) {
      const message = await response.text();
      console.log('✅', message); // "Inventario reprocesado correctamente"
    }
  } catch (error) {
    console.error('Error:', error);
  }
};
```

### 3. **Marcar como Procesado (Solo Estado)**

**Endpoint:** `PUT /api/ingresos/{id}/marcar-procesado`

**Descripción:** Solo cambia el estado del ingreso a `procesado = true` **SIN** actualizar el inventario.

**¿Cuándo usar?**
- Cuando el inventario ya fue actualizado manualmente
- Cuando necesitas marcar como procesado sin afectar el stock
- Para casos especiales de procesamiento manual

**¿Qué hace internamente?**
```java
public Ingreso marcarComoProcesado(Long ingresoId) {
    Ingreso ingreso = ingresoRepository.findById(ingresoId)
        .orElseThrow(() -> new RuntimeException("Ingreso no encontrado"));

    if (ingreso.getProcesado()) {
        throw new RuntimeException("El ingreso ya está marcado como procesado");
    }

    // Solo cambiar el estado, NO tocar inventario
    ingreso.setProcesado(true);
    return ingresoRepository.save(ingreso);
}
```

**Uso en Frontend:**
```javascript
// Solo marcar como procesado (sin tocar inventario)
const marcarComoProcesado = async (ingresoId) => {
  try {
    const response = await fetch(`/api/ingresos/${ingresoId}/marcar-procesado`, {
      method: 'PUT'
    });
    
    if (response.ok) {
      const ingresoActualizado = await response.json();
      console.log('✅ Ingreso marcado como procesado:', ingresoActualizado);
      // Actualizar UI con el ingreso actualizado
    }
  } catch (error) {
    console.error('Error:', error);
  }
};
```

---

## 📊 **Comparación de Endpoints**

| Endpoint | Método | Actualiza Inventario | Marca como Procesado | Uso Recomendado |
|----------|--------|---------------------|---------------------|-----------------|
| `/procesar` | `PUT` | ✅ SÍ | ✅ SÍ | Procesamiento automático completo |
| `/reprocesar` | `PUT` | ✅ SÍ | ✅ SÍ | Corrección de procesamiento previo |
| `/marcar-procesado` | `PUT` | ❌ NO | ✅ SÍ | Solo cambio de estado manual |

---

## 🎯 **Flujo Recomendado en Frontend**

### **Escenario 1: Procesamiento Normal**
```javascript
const procesarIngresoNormal = async (ingresoId) => {
  // 1. Mostrar loading
  setLoading(true);
  
  try {
    // 2. Procesar completamente
    await fetch(`/api/ingresos/${ingresoId}/procesar`, { method: 'PUT' });
    
    // 3. Actualizar UI
    setIngresoProcesado(true);
    mostrarMensaje('✅ Ingreso procesado correctamente');
    
  } catch (error) {
    mostrarError('❌ Error al procesar ingreso: ' + error.message);
  } finally {
    setLoading(false);
  }
};
```

### **Escenario 2: Procesamiento Manual**
```javascript
const procesarIngresoManual = async (ingresoId) => {
  // 1. Confirmar con el usuario
  const confirmar = window.confirm(
    '¿Estás seguro? Esto marcará el ingreso como procesado SIN actualizar el inventario.'
  );
  
  if (!confirmar) return;
  
  try {
    // 2. Solo marcar como procesado
    await fetch(`/api/ingresos/${ingresoId}/marcar-procesado`, { method: 'PUT' });
    
    // 3. Actualizar UI
    setIngresoProcesado(true);
    mostrarMensaje('✅ Ingreso marcado como procesado');
    
  } catch (error) {
    mostrarError('❌ Error: ' + error.message);
  }
};
```

### **Escenario 3: Corrección de Procesamiento**
```javascript
const corregirProcesamiento = async (ingresoId) => {
  // 1. Confirmar corrección
  const confirmar = window.confirm(
    '¿Reprocesar este ingreso? Esto actualizará nuevamente el inventario.'
  );
  
  if (!confirmar) return;
  
  try {
    // 2. Reprocesar
    await fetch(`/api/ingresos/${ingresoId}/reprocesar`, { method: 'PUT' });
    
    // 3. Actualizar UI
    mostrarMensaje('✅ Ingreso reprocesado correctamente');
    
  } catch (error) {
    mostrarError('❌ Error al reprocesar: ' + error.message);
  }
};
```

---

## 🔧 **Validaciones Importantes**

### **Antes de Procesar:**
- ✅ El ingreso debe existir
- ✅ El ingreso NO debe estar ya procesado (para `/procesar`)
- ✅ El ingreso debe tener detalles válidos
- ✅ Los productos deben existir en la base de datos
- ✅ La sede principal (ID=1) debe existir

### **Errores Comunes:**
- `"El ingreso ya ha sido procesado"` - Intentar procesar un ingreso ya procesado
- `"Proveedor no encontrado"` - ID de proveedor inválido
- `"Producto no encontrado"` - ID de producto inválido
- `"Sede principal no encontrada"` - No existe sede con ID=1

---

## 🎉 **Resumen de Cambios**

✅ **Corregido:** `POST` → `PUT` para todos los endpoints de procesamiento
✅ **Mantenido:** Toda la funcionalidad existente
✅ **Mejorado:** Cumplimiento con convenciones REST
✅ **Documentado:** Uso claro para frontend

**Los endpoints ahora siguen las mejores prácticas REST!** 🚀
