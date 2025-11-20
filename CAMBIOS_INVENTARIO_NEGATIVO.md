# 📦 Cambios en el Manejo de Inventario - Valores Negativos Permitidos

## 🎯 Resumen

El sistema ahora **permite valores negativos en el inventario** para soportar **ventas anticipadas** (productos vendidos antes de tenerlos físicamente en tienda).

## ✅ ¿Qué cambió?

### Antes:
- ❌ El inventario **no podía quedar negativo**
- ❌ Si intentabas vender más de lo disponible, el sistema rechazaba la venta con error: `"Stock insuficiente"`
- ❌ La validación `@Min(0)` impedía guardar valores negativos

### Ahora:
- ✅ El inventario **puede quedar negativo**
- ✅ Puedes vender productos aunque no tengas stock físico
- ✅ El sistema registra cuántas unidades faltan por recibir
- ✅ Cuando ingreses las unidades faltantes, el inventario se normaliza automáticamente

## 🔄 Comportamiento Actual

### Flujo de Venta Anticipada:

1. **Inventario inicial**: 5 unidades
2. **Venta realizada**: 8 unidades
3. **Resultado**: Inventario queda en **-3** (indica que faltan 3 unidades)
4. **Cuando lleguen las 3 unidades**: Al hacer un ingreso de 3 unidades, el inventario pasa a **0**

### Ejemplo Práctico:

```
Estado Inicial:
- Producto: Vidrio 6mm 100x50
- Inventario Sede Centro: 5 unidades

Venta:
- Cliente compra: 8 unidades
- Sistema procesa la venta ✅
- Inventario queda en: -3 unidades ⚠️

Ingreso posterior:
- Llegan 10 unidades del proveedor
- Se registra ingreso de 10 unidades
- Inventario actualizado: -3 + 10 = 7 unidades ✅
```

## 📡 Impacto en el Frontend

### 1. **Visualización de Inventario**

Ahora debes mostrar valores negativos en las tablas/listados de inventario:

```javascript
// ✅ CORRECTO - Mostrar valores negativos
const cantidad = inventario.cantidad; // Puede ser -3, -5, etc.

// Mostrar con indicador visual
{cantidad < 0 ? (
  <span className="text-warning">
    {cantidad} ⚠️ (Faltan {Math.abs(cantidad)} unidades)
  </span>
) : (
  <span>{cantidad}</span>
)}
```

### 2. **Validaciones en el Frontend**

**Ya NO debes validar** que el inventario sea >= 0 antes de enviar la venta:

```javascript
// ❌ ANTES (ya no necesario)
if (cantidadDisponible < cantidadAVender) {
  alert("Stock insuficiente");
  return;
}

// ✅ AHORA - El backend permite la venta
// Puedes mostrar una advertencia pero no bloquear
if (cantidadDisponible < cantidadAVender) {
  const faltantes = cantidadAVender - cantidadDisponible;
  const confirmar = confirm(
    `⚠️ Advertencia: Faltan ${faltantes} unidades. ` +
    `¿Desea continuar con la venta anticipada?`
  );
  if (!confirmar) return;
}
```

### 3. **Mensajes de Error**

El backend **ya no devolverá** el error `"Stock insuficiente"` por valores negativos. Solo devolverá errores si:
- El producto no existe
- La sede no existe
- Hay problemas de concurrencia (muy raro)

### 4. **Indicadores Visuales Recomendados**

Sugerencia para mostrar inventario negativo:

```jsx
// Componente de Inventario
function InventarioDisplay({ cantidad }) {
  const esNegativo = cantidad < 0;
  const faltantes = esNegativo ? Math.abs(cantidad) : 0;
  
  return (
    <div className={esNegativo ? "inventario-negativo" : "inventario-normal"}>
      <span className="cantidad">{cantidad}</span>
      {esNegativo && (
        <span className="badge badge-warning">
          ⚠️ Faltan {faltantes} unidades
        </span>
      )}
    </div>
  );
}
```

### 5. **Filtros y Búsquedas**

Si tienes filtros de "productos con stock bajo", considera incluir productos negativos:

```javascript
// Filtrar productos que necesitan atención
const productosConProblemas = productos.filter(p => 
  p.inventarioTotal < 0 || // Faltantes
  p.inventarioTotal < p.stockMinimo // Stock bajo
);
```

## 🔍 Endpoints Afectados

### ✅ Endpoints que ahora aceptan valores negativos:

1. **POST /api/inventario** - Crear inventario
2. **PUT /api/inventario/{id}** - Actualizar inventario
3. **POST /api/ordenes/venta** - Crear orden de venta
4. **PUT /api/ordenes/venta/{id}** - Actualizar orden de venta
5. **PUT /api/productos/{id}** - Actualizar producto (si incluye inventario)

### 📊 Respuestas del Backend

El backend ahora puede devolver:

```json
{
  "id": 123,
  "producto": { "id": 1, "nombre": "Vidrio 6mm" },
  "sede": { "id": 2, "nombre": "Centro" },
  "cantidad": -3  // ✅ Ahora puede ser negativo
}
```

## ⚠️ Consideraciones Importantes

### 1. **No hay validación de stock mínimo**
- El sistema no bloquea ventas por falta de stock
- Es responsabilidad del usuario/frontend mostrar advertencias si lo desea

### 2. **Concurrencia**
- El sistema mantiene locks pesimistas para evitar race conditions
- Si dos usuarios venden simultáneamente, el sistema maneja la concurrencia correctamente

### 3. **Reportes y Dashboard**
- Los reportes de inventario ahora pueden mostrar valores negativos
- Considera agregar indicadores visuales para identificar productos con faltantes

### 4. **Integración con otros módulos**
- Los traslados entre sedes funcionan normalmente (pueden trasladar desde inventario negativo)
- Los ingresos de productos normalizan automáticamente el inventario negativo

## 🧪 Casos de Prueba Sugeridos

1. **Venta con stock suficiente**: Debe funcionar normalmente
2. **Venta con stock insuficiente**: Debe permitir la venta y dejar inventario negativo
3. **Venta sin stock (0 unidades)**: Debe permitir la venta
4. **Ingreso después de venta anticipada**: Debe normalizar el inventario
5. **Visualización de inventario negativo**: Debe mostrarse claramente

## 📝 Ejemplo de Request/Response

### Request: Crear Orden de Venta
```json
POST /api/ordenes/venta
{
  "clienteId": 1,
  "sedeId": 2,
  "items": [
    {
      "productoId": 10,
      "cantidad": 8,  // Solo hay 5 en inventario
      "precioUnitario": 15000
    }
  ]
}
```

### Response: Orden Creada
```json
{
  "mensaje": "Orden de venta creada exitosamente",
  "orden": {
    "id": 456,
    "numero": 1001,
    "total": 120000
  }
}
```

### Estado del Inventario Después:
```json
GET /api/inventario?productoId=10&sedeId=2
{
  "id": 123,
  "producto": { "id": 10 },
  "sede": { "id": 2 },
  "cantidad": -3  // ✅ Negativo permitido
}
```

## 🎨 Sugerencias de UI/UX

1. **Color coding**:
   - Verde: Stock positivo normal
   - Amarillo/Naranja: Stock bajo (0-5 unidades)
   - Rojo: Stock negativo (faltantes)

2. **Tooltips informativos**:
   - "Este producto tiene faltantes. Se vendieron X unidades antes de recibirlas."

3. **Alertas opcionales**:
   - Mostrar advertencia (no error) cuando se intenta vender más de lo disponible
   - Permitir confirmar la venta anticipada

4. **Reportes**:
   - Agregar filtro "Productos con faltantes" (cantidad < 0)
   - Mostrar total de unidades faltantes en dashboard

## ❓ Preguntas Frecuentes

**P: ¿Puedo seguir validando stock en el frontend?**
R: Sí, pero solo como advertencia, no como bloqueo. El backend permitirá la venta.

**P: ¿Qué pasa si tengo -10 unidades y hago un ingreso de 5?**
R: El inventario quedará en -5. Los ingresos se suman normalmente.

**P: ¿Los reportes de inventario muestran valores negativos?**
R: Sí, todos los endpoints de consulta devuelven el valor real (puede ser negativo).

**P: ¿Hay un límite de cuánto puede ser negativo?**
R: No, técnicamente no hay límite, pero es recomendable monitorear valores muy negativos.

---

**Fecha de implementación**: 2025-01-XX  
**Versión del backend**: Compatible con todas las versiones actuales  
**Breaking changes**: Ninguno (solo se removieron restricciones)


