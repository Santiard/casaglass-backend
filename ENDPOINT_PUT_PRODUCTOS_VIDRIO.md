# 📋 ENDPOINT: PUT /api/productos-vidrio/{id}

## 🔍 Información del Endpoint

### URL
```
PUT /api/productos-vidrio/{id}
```

### Ubicación en el código
- **Controller**: `src/main/java/com/casaglass/casaglass_backend/controller/ProductoVidrioController.java` (línea 84-91)
- **Service**: `src/main/java/com/casaglass/casaglass_backend/service/ProductoVidrioService.java` (línea 180-211)

---

## ✅ Campos que ACEPTA el endpoint

El endpoint acepta un objeto `ProductoVidrio` completo en el body. Los campos que se actualizan son:

### Campos heredados de `Producto`:
- ✅ `posicion` (String)
- ✅ `codigo` (String)
- ✅ `nombre` (String)
- ✅ `color` (Enum: ColorProducto)
- ✅ `cantidad` (Integer) - **⚠️ NOTA: Este es el campo `cantidad` del producto, NO el inventario por sede**
- ✅ `costo` (Double)
- ✅ `precio1` (Double)
- ✅ `precio2` (Double)
- ✅ `precio3` (Double)
- ✅ `descripcion` (String)
- ✅ `categoria` (Object con `id`)

### Campos específicos de `ProductoVidrio`:
- ✅ `mm` (Double) - Espesor en milímetros
- ✅ `m1` (Double) - Medida 1
- ✅ `m2` (Double) - Medida 2
- ✅ `m1m2` (Double) - Se calcula automáticamente (`m1 * m2`) mediante `@PreUpdate`

---

## ❌ Campos que NO acepta

El endpoint **NO acepta** estos campos relacionados con inventario por sede:
- ❌ `cantidadInsula` (Integer)
- ❌ `cantidadCentro` (Integer)
- ❌ `cantidadPatios` (Integer)

**Razón**: El método `actualizar` en `ProductoVidrioService` solo actualiza los campos del modelo `ProductoVidrio`, no el inventario por sede.

---

## 🔑 Sobre el ID en la URL

### ¿Qué ID es?
El `{id}` en la URL es el **ID del producto vidrio**, que es el **mismo ID del producto normal**.

**Explicación técnica**:
- `ProductoVidrio` hereda de `Producto` usando estrategia `JOINED` inheritance
- Ambos comparten el mismo `id` en la tabla `productos`
- `ProductoVidrio` tiene un registro adicional en la tabla `productos_vidrio` con el mismo `id`
- Por lo tanto, el ID es único y funciona para ambos

### Ejemplo:
```json
// Producto normal en tabla productos
{
  "id": 117,
  "codigo": "VID007",
  "nombre": "VIDRIO JAMES BON",
  ...
}

// Producto vidrio en tabla productos_vidrio (mismo ID)
{
  "id": 117,  // ← Mismo ID
  "mm": 3.0,
  "m1": 3.0,
  "m2": 4.0,
  "m1m2": 12.0
}
```

---

## 📦 Actualización de Inventario

### ❌ NO actualiza inventario automáticamente

El endpoint `PUT /api/productos-vidrio/{id}` **NO actualiza el inventario** por sede (`cantidadInsula`, `cantidadCentro`, `cantidadPatios`).

### Comparación con Producto normal:

| Endpoint | Actualiza Inventario | DTO usado |
|----------|---------------------|-----------|
| `PUT /api/productos/{id}` | ✅ **SÍ** (si se envían `cantidadInsula`, `cantidadCentro`, `cantidadPatios`) | `ProductoActualizarDTO` |
| `PUT /api/productos-vidrio/{id}` | ❌ **NO** | `ProductoVidrio` (directo) |

### Código actual de `ProductoVidrioService.actualizar`:

```java
public ProductoVidrio actualizar(Long id, ProductoVidrio p) {
    return repo.findById(id).map(actual -> {
        // Solo actualiza campos del producto
        actual.setPosicion(p.getPosicion());
        actual.setCodigo(p.getCodigo());
        actual.setNombre(p.getNombre());
        // ... otros campos ...
        
        // ❌ NO actualiza inventario
        return repo.save(actual);
    }).orElseThrow(...);
}
```

---

## 🔧 Cómo actualizar el inventario

### Opción 1: Usar el endpoint de inventario (recomendado)

Para actualizar el inventario de un producto vidrio, usa el endpoint de inventario:

**✅ NUEVO ENDPOINT CREADO:**
```
PUT /api/inventario/producto/{productoId}
```

**Body esperado:**
```json
{
  "cantidadInsula": 10,
  "cantidadCentro": 0,
  "cantidadPatios": 0
}
```

**Funcionalidad**: Actualiza el inventario del producto para las 3 sedes en una sola llamada.

**Alternativa (menos eficiente):**
```
PUT /api/inventario/{id}
```
Donde `{id}` es el ID del registro de inventario (no del producto). Requiere hacer 3 llamadas separadas.

### Opción 2: Modificar el backend (si necesitas actualizar inventario desde el mismo endpoint)

Si necesitas que `PUT /api/productos-vidrio/{id}` también actualice el inventario, necesitarías:

1. **Crear un DTO** similar a `ProductoActualizarDTO` pero para `ProductoVidrio`:
   ```java
   public class ProductoVidrioActualizarDTO {
       // Campos del producto
       private String codigo;
       private String nombre;
       // ... otros campos ...
       
       // Campos de vidrio
       private Double mm;
       private Double m1;
       private Double m2;
       
       // ✅ Campos de inventario (NUEVOS)
       private Integer cantidadInsula;
       private Integer cantidadCentro;
       private Integer cantidadPatios;
   }
   ```

2. **Modificar el método `actualizar`** en `ProductoVidrioService` para:
   - Aceptar el nuevo DTO
   - Llamar a `actualizarInventarioConValores()` si se envían las cantidades (similar a `ProductoService`)

---

## 📝 Ejemplo de Payload

### Payload actual (lo que acepta ahora):

```json
PUT /api/productos-vidrio/117
{
  "codigo": "VID007",
  "nombre": "VIDRIO JAMES BON",
  "categoria": {
    "id": 26
  },
  "tipo": "UNID",
  "color": "NA",
  "cantidad": 0,           // ⚠️ Este es el campo del producto, NO el inventario
  "costo": 12.0,
  "precio1": 133.0,
  "precio2": 133.0,
  "precio3": 133.0,
  "descripcion": "",
  "posicion": "",
  "mm": 3.0,
  "m1": 3.0,
  "m2": 4.0
}
```

### Payload que NO funciona (campos de inventario):

```json
PUT /api/productos-vidrio/117
{
  "codigo": "VID007",
  "nombre": "VIDRIO JAMES BON",
  // ... otros campos ...
  "cantidadInsula": 10,     // ❌ Este campo se IGNORA
  "cantidadCentro": 20,     // ❌ Este campo se IGNORA
  "cantidadPatios": 30      // ❌ Este campo se IGNORA
}
```

---

## ✅ Resumen de Respuestas

| Pregunta | Respuesta |
|----------|-----------|
| **¿Qué campos espera?** | Campos de `ProductoVidrio`: `codigo`, `nombre`, `categoria`, `tipo`, `color`, `cantidad`, `costo`, `precio1-3`, `descripcion`, `posicion`, `mm`, `m1`, `m2` |
| **¿Acepta cantidad, cantidadInsula, cantidadCentro, cantidadPatios?** | ✅ `cantidad` (sí, pero es del producto, no inventario)<br>❌ `cantidadInsula`, `cantidadCentro`, `cantidadPatios` (NO) |
| **¿Cómo actualiza las cantidades del inventario?** | ❌ **NO las actualiza**. Requiere usar otro endpoint:<br>✅ `PUT /api/inventario/producto/{productoId}` (recomendado - actualiza las 3 sedes)<br>O `PUT /api/inventario/{id}` (por registro individual) |
| **¿El id en la URL es el ID del producto vidrio o del producto normal?** | ✅ **Ambos son el mismo**. El ID es compartido entre `productos` y `productos_vidrio` |

---

## 🚀 Recomendación

Si necesitas actualizar el inventario desde el frontend al actualizar un producto vidrio:

1. **Opción A (Recomendada)**: Hacer dos llamadas:
   - `PUT /api/productos-vidrio/{id}` para actualizar el producto
   - `PUT /api/inventario/producto/{id}` para actualizar el inventario (las 3 sedes en una sola llamada)

2. **Opción B (Mejor a largo plazo)**: Modificar el backend para que `PUT /api/productos-vidrio/{id}` también acepte y actualice el inventario (similar a como lo hace `PUT /api/productos/{id}`)

---

## ✅ NUEVO ENDPOINT CREADO

### `PUT /api/inventario/producto/{productoId}`

**Descripción**: Actualiza el inventario de un producto en las 3 sedes (Insula, Centro, Patios) en una sola llamada.

**Parámetros**:
- `productoId` (Path): ID del producto

**Body**:
```json
{
  "cantidadInsula": 10,
  "cantidadCentro": 0,
  "cantidadPatios": 0
}
```

**Respuesta exitosa** (200 OK):
```json
[
  {
    "id": 1,
    "producto": { "id": 117, ... },
    "sede": { "id": 1, "nombre": "Insula", ... },
    "cantidad": 10
  },
  {
    "id": 2,
    "producto": { "id": 117, ... },
    "sede": { "id": 2, "nombre": "Centro", ... },
    "cantidad": 0
  },
  {
    "id": 3,
    "producto": { "id": 117, ... },
    "sede": { "id": 3, "nombre": "Patios", ... },
    "cantidad": 0
  }
]
```

**Errores**:
- `400 Bad Request`: Si no se encuentran las 3 sedes o hay un error en los datos
- `404 Not Found`: Si el producto no existe

**Notas**:
- Permite valores negativos (para ventas anticipadas)
- Si no existe inventario para una sede, lo crea automáticamente
- Si ya existe inventario, lo actualiza con el nuevo valor

