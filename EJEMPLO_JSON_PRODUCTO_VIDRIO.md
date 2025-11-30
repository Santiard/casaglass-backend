# 📦 EJEMPLO: JSON que se envía al frontend para ProductoVidrio

## 🔍 Formato del JSON retornado

Cuando el backend retorna un `ProductoVidrio` desde el endpoint `/api/inventario-completo/vidrios` o `/api/inventario-completo`, se serializa como `ProductoInventarioCompletoDTO` con el siguiente formato:

```json
{
  "id": 117,
  "codigo": "VID007",
  "nombre": "VIDRIO JAMES BON",
  "categoria": "VIDRIO",              // ⚠️ IMPORTANTE: Es el NOMBRE de la categoría (String), NO un objeto
  "tipo": "UNID",                     // Enum TipoProducto como String
  "color": "NA",                      // Enum ColorProducto como String
  "esVidrio": true,                   // ✅ Boolean que indica que es vidrio
  "mm": 3.0,                          // ✅ Campo específico de vidrio
  "m1": 3.0,                          // ✅ Campo específico de vidrio
  "m2": 4.0,                          // ✅ Campo específico de vidrio
  "cantidadInsula": 0,
  "cantidadCentro": 0,
  "cantidadPatios": 0,
  "cantidadTotal": 0,                 // Suma de las 3 sedes
  "costo": 12.0,
  "precio1": 133.0,
  "precio2": 133.0,
  "precio3": 133.0
}
```

## 🔍 Comparación: Producto Normal vs Producto Vidrio

### Producto Normal:
```json
{
  "id": 1,
  "codigo": "PROD001",
  "nombre": "Producto Normal",
  "categoria": "OTRA_CATEGORIA",
  "tipo": "UNID",
  "color": "BLANCO",
  "esVidrio": false,                  // ✅ false para productos normales
  "mm": null,                         // ✅ null para productos normales
  "m1": null,                         // ✅ null para productos normales
  "m2": null,                         // ✅ null para productos normales
  "cantidadInsula": 100,
  "cantidadCentro": 81,
  "cantidadPatios": 200,
  "cantidadTotal": 381,
  "costo": 50.0,
  "precio1": 100.0,
  "precio2": 90.0,
  "precio3": 80.0
}
```

### Producto Vidrio:
```json
{
  "id": 117,
  "codigo": "VID007",
  "nombre": "VIDRIO JAMES BON",
  "categoria": "VIDRIO",              // ⚠️ NOMBRE de la categoría (String)
  "tipo": "UNID",
  "color": "NA",
  "esVidrio": true,                   // ✅ true para productos vidrio
  "mm": 3.0,                          // ✅ Tiene valores
  "m1": 3.0,                          // ✅ Tiene valores
  "m2": 4.0,                          // ✅ Tiene valores
  "cantidadInsula": 0,
  "cantidadCentro": 0,
  "cantidadPatios": 0,
  "cantidadTotal": 0,
  "costo": 12.0,
  "precio1": 133.0,
  "precio2": 133.0,
  "precio3": 133.0
}
```

## ⚠️ PUNTOS IMPORTANTES PARA EL FRONTEND

### 1. Campo `categoria`
- **Tipo**: `String` (nombre de la categoría)
- **NO es un objeto**: No tiene `{ id: 26, nombre: "VIDRIO" }`
- **Es solo el nombre**: `"VIDRIO"`

### 2. Campo `esVidrio`
- **Tipo**: `Boolean`
- **Valor**: `true` para productos vidrio, `false` para productos normales
- **Uso**: Para filtrar o identificar productos vidrio

### 3. Campos de vidrio (`mm`, `m1`, `m2`)
- **Tipo**: `Double` o `null`
- **Valor**: 
  - Si `esVidrio === true` → tienen valores numéricos
  - Si `esVidrio === false` → son `null`

## 🔍 CÓDIGO DEL BACKEND QUE GENERA ESTO

El método `convertirADTO` en `InventarioCompletoService.java` (líneas 240-287):

```java
private ProductoInventarioCompletoDTO convertirADTO(Producto producto, Map<Long, Integer> inventariosPorSede) {
    // ... código para obtener inventarios ...
    
    // Verificar si es vidrio
    Boolean esVidrio = producto instanceof ProductoVidrio;
    Double mm = null;
    Double m1 = null;
    Double m2 = null;

    if (esVidrio) {
        ProductoVidrio vidrio = (ProductoVidrio) producto;
        mm = vidrio.getMm();
        m1 = vidrio.getM1();
        m2 = vidrio.getM2();
    }

    // Obtener nombre de la categoría (String, NO objeto)
    String categoriaNombre = producto.getCategoria() != null 
        ? producto.getCategoria().getNombre() 
        : null;
    
    String tipoProducto = producto.getTipo() != null ? producto.getTipo().name() : null;
    String colorProducto = producto.getColor() != null ? producto.getColor().name() : null;

    return new ProductoInventarioCompletoDTO(
        producto.getId(),
        producto.getCodigo(),
        producto.getNombre(),
        categoriaNombre,        // ⚠️ String, NO objeto Categoria
        tipoProducto,           // String del enum
        colorProducto,          // String del enum
        esVidrio,               // Boolean
        mm,                     // Double o null
        m1,                     // Double o null
        m2,                     // Double o null
        cantidadInsula,
        cantidadCentro,
        cantidadPatios,
        producto.getCosto(),
        producto.getPrecio1(),
        producto.getPrecio2(),
        producto.getPrecio3()
    );
}
```

## 🐛 POSIBLE PROBLEMA EN EL FRONTEND

Si el filtro de categoría no funciona, verifica:

1. **¿Cómo estás comparando la categoría?**
   ```javascript
   // ❌ INCORRECTO (si esperas un objeto):
   producto.categoria.id === categoriaId
   producto.categoria?.id === categoriaId
   
   // ✅ CORRECTO (es un String):
   producto.categoria === "VIDRIO"
   producto.categoria?.toLowerCase() === "vidrio"
   ```

2. **¿Cómo estás filtrando?**
   ```javascript
   // ✅ CORRECTO:
   const productosFiltrados = productos.filter(p => 
     p.categoria === categoriaSeleccionada
   );
   
   // O si necesitas comparar por ID:
   // Necesitarías mapear nombres de categoría a IDs en el frontend
   ```

3. **¿El campo `categoria` está llegando correctamente?**
   - Verifica en la consola del navegador el objeto completo
   - Debería ser: `categoria: "VIDRIO"` (String)
   - NO debería ser: `categoria: { id: 26, nombre: "VIDRIO" }` (objeto)

## 📝 RESUMEN

- **`categoria`**: String con el nombre de la categoría (ej: `"VIDRIO"`)
- **`esVidrio`**: Boolean (`true` para vidrios, `false` para normales)
- **`mm`, `m1`, `m2`**: Double o `null` (solo tienen valores si `esVidrio === true`)

