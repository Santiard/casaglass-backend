# 📦 CÓMO FUNCIONA LA EDICIÓN DE TRASLADOS

## 🎯 Resumen General

La edición de traslados se divide en **DOS PARTES INDEPENDIENTES**:

1. **Edición de la CABECERA del traslado** (sede origen, sede destino, fecha, etc.)
2. **Edición de los DETALLES del traslado** (productos y cantidades)

---

## 1️⃣ EDICIÓN DE LA CABECERA DEL TRASLADO

### Endpoint
```
PUT /api/traslados/{id}
```

### Controlador
**Archivo:** `TrasladoController.java` (línea 67-76)

```java
@PutMapping("/{id}")
public ResponseEntity<?> actualizarCabecera(@PathVariable Long id, @RequestBody Traslado traslado)
```

### Servicio
**Archivo:** `TrasladoService.java` (línea 158-179)

**Método:** `actualizarCabecera(Long id, Traslado cambios)`

### Campos que se pueden editar:
- ✅ `sedeOrigen.id` - Sede de origen
- ✅ `sedeDestino.id` - Sede de destino
- ✅ `fecha` - Fecha del traslado
- ✅ `trabajadorConfirmacion.id` - Trabajador que confirma
- ✅ `fechaConfirmacion` - Fecha de confirmación

### Validaciones:
- ❌ **NO permite** que `sedeOrigen` y `sedeDestino` sean la misma
- ✅ Los campos son **opcionales** (solo se actualizan si vienen en el payload)

### ⚠️ IMPORTANTE - Cambio de Sedes:
**Si cambias la sede origen o destino, el inventario NO se ajusta automáticamente.**

Esto significa que:
- Si cambias `sedeOrigen` de `1` a `2`, el inventario que ya se restó de la sede `1` **NO se revierte**
- Si cambias `sedeDestino` de `2` a `3`, el inventario que ya se sumó a la sede `2` **NO se revierte**

**Recomendación:** Evitar cambiar las sedes después de crear el traslado, o implementar lógica adicional para revertir y reaplicar los movimientos de inventario.

---

## 2️⃣ EDICIÓN DE LOS DETALLES DEL TRASLADO

### Endpoint
```
PUT /api/traslados/{trasladoId}/detalles/{detalleId}
```

### Controlador
**Archivo:** `TrasladoDetalleController.java` (línea 51-62)

```java
@PutMapping("/{detalleId}")
public ResponseEntity<?> actualizar(@PathVariable Long trasladoId,
                                   @PathVariable Long detalleId,
                                   @Valid @RequestBody TrasladoDetalle detalle)
```

### Servicio
**Archivo:** `TrasladoDetalleService.java` (línea 65-82)

**Método:** `actualizar(Long trasladoId, Long detalleId, TrasladoDetalle cambios)`

### Campos que se pueden editar:
- ✅ `producto.id` - Producto del detalle
- ✅ `cantidad` - Cantidad a trasladar

### Validaciones:
- ✅ `cantidad` debe ser `>= 1`
- ✅ El detalle debe pertenecer al traslado indicado
- ✅ `producto.id` es obligatorio si se envía

### ⚠️ PROBLEMA CRÍTICO - Inventario NO se actualiza:

**El servicio `TrasladoDetalleService.actualizar()` NO actualiza el inventario cuando editas un detalle.**

Esto significa que:
- Si cambias la `cantidad` de `10` a `20`, el inventario **NO se ajusta** (debería restar 10 más de origen y sumar 10 más a destino)
- Si cambias el `producto` de `A` a `B`, el inventario **NO se ajusta** (debería revertir el movimiento de `A` y aplicar el movimiento de `B`)

### 🔧 Solución Alternativa:

Existe otro método en `TrasladoService.actualizarDetalle()` (línea 228-279) que **SÍ maneja el inventario correctamente**, pero **NO está siendo usado por el controller**.

Este método:
- ✅ Revierte el inventario del producto anterior si cambias el producto
- ✅ Ajusta el inventario por la diferencia si cambias la cantidad
- ✅ Valida stock disponible antes de hacer cambios

**Recomendación:** El `TrasladoDetalleController` debería usar `TrasladoService.actualizarDetalle()` en lugar de `TrasladoDetalleService.actualizar()`.

---

## 📊 FLUJO COMPLETO DE EDICIÓN

### Escenario 1: Editar solo la cabecera (fecha, sedes)

```
1. Frontend envía: PUT /api/traslados/5
   {
     "fecha": "2026-01-21",
     "sedeOrigen": {"id": 1},
     "sedeDestino": {"id": 2}
   }

2. Backend actualiza solo los campos enviados
3. ❌ NO se ajusta inventario (incluso si cambias las sedes)
```

### Escenario 2: Editar un detalle (producto o cantidad)

```
1. Frontend envía: PUT /api/traslados/5/detalles/10
   {
     "producto": {"id": 100},
     "cantidad": 15
   }

2. Backend actualiza el detalle
3. ❌ NO se ajusta inventario (PROBLEMA)
```

---

## 🔍 CÓDIGO RELEVANTE

### TrasladoService.actualizarCabecera() (CORRECTO)
```java
@Transactional
public Traslado actualizarCabecera(Long id, Traslado cambios) {
    return repo.findById(id).map(t -> {
        // Actualiza campos opcionales
        if (cambios.getSedeOrigen() != null && cambios.getSedeOrigen().getId() != null) {
            t.setSedeOrigen(em.getReference(Sede.class, cambios.getSedeOrigen().getId()));
        }
        // ... más campos ...
        return repo.save(t);
    }).orElseThrow(() -> new RuntimeException("Traslado no encontrado con id " + id));
}
```

### TrasladoDetalleService.actualizar() (PROBLEMA - NO actualiza inventario)
```java
@Transactional
public TrasladoDetalle actualizar(Long trasladoId, Long detalleId, TrasladoDetalle cambios) {
    TrasladoDetalle detalle = detalleRepo.findById(detalleId)
            .orElseThrow(() -> new RuntimeException("Detalle no encontrado con id " + detalleId));

    // Valida que pertenezca al traslado
    if (!Objects.equals(detalle.getTraslado().getId(), trasladoId)) {
        throw new IllegalArgumentException("El detalle no pertenece al traslado indicado");
    }

    // Actualiza campos
    if (cambios.getProducto() != null && cambios.getProducto().getId() != null) {
        detalle.setProducto(em.getReference(Producto.class, cambios.getProducto().getId()));
    }
    if (cambios.getCantidad() != null) {
        if (cambios.getCantidad() < 1) throw new IllegalArgumentException("La cantidad debe ser >= 1");
        detalle.setCantidad(cambios.getCantidad());
    }

    // ❌ PROBLEMA: Guarda sin ajustar inventario
    return detalleRepo.save(detalle);
}
```

### TrasladoService.actualizarDetalle() (CORRECTO - SÍ actualiza inventario)
```java
@Transactional
public TrasladoDetalle actualizarDetalle(Long trasladoId, Long detalleId, TrasladoDetalle payload) {
    TrasladoDetalle d = detalleRepo.findById(detalleId)
            .orElseThrow(() -> new RuntimeException("Detalle no encontrado"));
    
    // Valida que pertenezca al traslado
    if (!Objects.equals(d.getTraslado().getId(), trasladoId))
        throw new IllegalArgumentException("El detalle no pertenece al traslado indicado");

    Traslado traslado = d.getTraslado();
    Long sedeOrigenId = traslado.getSedeOrigen().getId();
    Long sedeDestinoId = traslado.getSedeDestino().getId();
    
    // ✅ CAMBIO DE PRODUCTO: Revertir inventario del producto anterior y aplicar el nuevo
    if (payload.getProducto() != null && payload.getProducto().getId() != null 
        && !Objects.equals(d.getProducto().getId(), payload.getProducto().getId())) {
        
        Long productoAnteriorId = d.getProducto().getId();
        Double cantidadAnterior = d.getCantidad();
        
        // Revertir movimiento del producto anterior
        ajustarInventario(productoAnteriorId, sedeOrigenId, cantidadAnterior, "origen");
        ajustarInventario(productoAnteriorId, sedeDestinoId, -cantidadAnterior, "destino");
        
        // Aplicar movimiento del nuevo producto
        Long productoNuevoId = payload.getProducto().getId();
        Double cantidadNueva = (payload.getCantidad() != null) ? payload.getCantidad() : cantidadAnterior;
        
        ajustarInventario(productoNuevoId, sedeOrigenId, -cantidadNueva, "origen");
        ajustarInventario(productoNuevoId, sedeDestinoId, cantidadNueva, "destino");
        
        d.setProducto(em.getReference(Producto.class, productoNuevoId));
        if (payload.getCantidad() != null) {
            if (payload.getCantidad() < 1) throw new IllegalArgumentException("cantidad debe ser >= 1");
            d.setCantidad(payload.getCantidad());
        }
    }
    // ✅ CAMBIO DE CANTIDAD: Ajustar solo la diferencia
    else if (payload.getCantidad() != null && !Objects.equals(d.getCantidad(), payload.getCantidad())) {
        if (payload.getCantidad() < 1) throw new IllegalArgumentException("cantidad debe ser >= 1");
        
        Long productoId = d.getProducto().getId();
        Double cantidadAnterior = d.getCantidad();
        Double cantidadNueva = payload.getCantidad();
        Double diferencia = cantidadNueva - cantidadAnterior;
        
        // Ajustar inventario por la diferencia
        ajustarInventario(productoId, sedeOrigenId, -diferencia, "origen");
        ajustarInventario(productoId, sedeDestinoId, diferencia, "destino");
        
        d.setCantidad(cantidadNueva);
    }
    
    return detalleRepo.save(d);
}
```

---

## ⚠️ PROBLEMAS IDENTIFICADOS

### Problema 1: Inventario no se actualiza al editar detalles
**Ubicación:** `TrasladoDetalleController` usa `TrasladoDetalleService.actualizar()`

**Impacto:** 
- Si editas la cantidad de un detalle, el inventario queda desincronizado
- Si cambias el producto de un detalle, el inventario queda desincronizado

**Solución:** Cambiar `TrasladoDetalleController` para usar `TrasladoService.actualizarDetalle()` en lugar de `TrasladoDetalleService.actualizar()`.

### Problema 2: Inventario no se actualiza al cambiar sedes en la cabecera
**Ubicación:** `TrasladoService.actualizarCabecera()`

**Impacto:**
- Si cambias `sedeOrigen` o `sedeDestino`, los movimientos de inventario anteriores no se revierten
- El inventario queda desincronizado

**Solución:** Implementar lógica para revertir y reaplicar movimientos de inventario cuando se cambian las sedes.

---

## 📝 RECOMENDACIONES PARA EL FRONTEND

1. **Evitar editar sedes después de crear el traslado** - El inventario no se ajusta automáticamente.

2. **Evitar editar detalles después de crear el traslado** - Actualmente el inventario no se ajusta (a menos que se corrija el backend).

3. **Si necesitas editar**, considera:
   - Eliminar el traslado completo y crear uno nuevo
   - O implementar la corrección en el backend primero

---

## 🔧 CORRECCIÓN SUGERIDA

### Cambiar TrasladoDetalleController para usar TrasladoService:

```java
@RestController
@RequestMapping("/api/traslados/{trasladoId}/detalles")
public class TrasladoDetalleController {

    private final TrasladoService trasladoService; // ✅ Usar TrasladoService

    public TrasladoDetalleController(TrasladoService trasladoService) {
        this.trasladoService = trasladoService;
    }

    @PutMapping("/{detalleId}")
    public ResponseEntity<?> actualizar(@PathVariable Long trasladoId,
                                       @PathVariable Long detalleId,
                                       @Valid @RequestBody TrasladoDetalle detalle) {
        try {
            // ✅ Usar el método que SÍ actualiza el inventario
            return ResponseEntity.ok(trasladoService.actualizarDetalle(trasladoId, detalleId, detalle));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

---

## 📌 RESUMEN EJECUTIVO

| Operación | Endpoint | Inventario se actualiza? | Estado |
|-----------|----------|-------------------------|--------|
| Editar cabecera (fecha, sedes) | `PUT /api/traslados/{id}` | ❌ NO | ⚠️ Problema menor |
| Editar detalle (producto, cantidad) | `PUT /api/traslados/{trasladoId}/detalles/{detalleId}` | ❌ NO | 🔴 Problema crítico |
| Crear traslado | `POST /api/traslados` | ✅ SÍ | ✅ Correcto |
| Agregar detalle | `POST /api/traslados/{trasladoId}/detalles` | ✅ SÍ | ✅ Correcto |
| Eliminar detalle | `DELETE /api/traslados/{trasladoId}/detalles/{detalleId}` | ✅ SÍ | ✅ Correcto |

