# 🔄 Flujo Completo: IVA desde Orden hasta Factura

## 📋 Situación Actual

### **Los precios de los productos YA incluyen el IVA del 19%**

Esto significa que:
- ✅ Los `precioUnitario` que envía el frontend ya incluyen IVA
- ✅ El backend calcula el subtotal **SIN IVA** en la orden
- ⚠️ **IMPORTANTE**: Al crear la factura, debes decidir qué subtotal enviar

---

## 🔍 Flujo Completo

### **1. Creación de Orden**

**Frontend envía:**
```javascript
{
  items: [
    { productoId: 10, cantidad: 2, precioUnitario: 119.0 },  // ✅ Precio con IVA incluido
    { productoId: 20, cantidad: 1, precioUnitario: 238.0 }   // ✅ Precio con IVA incluido
  ]
}
```

**Backend calcula:**
```java
// 1. Subtotal bruto (con IVA incluido)
subtotalBruto = (2 × 119.0) + (1 × 238.0) = 476.0

// 2. Subtotal SIN IVA (restando el IVA)
subtotal = 476.0 / 1.19 = 400.0

// 3. Total
total = 400.0 - descuentos
```

**Orden guardada:**
```json
{
  "id": 100,
  "subtotal": 400.0,  // ✅ SIN IVA (calculado por backend)
  "descuentos": 0.0,
  "total": 400.0
}
```

---

## ⚠️ Pregunta Clave: ¿Qué subtotal envías a la factura?

### **Opción A: Enviar el subtotal de la orden (SIN IVA)**

Si envías `orden.subtotal` (que es 400.0):

```javascript
{
  ordenId: 100,
  subtotal: orden.subtotal,  // 400.0 (SIN IVA)
  descuentos: 0.0,
  iva: 400.0 × 0.19 = 76.0,  // ✅ Calcular IVA sobre subtotal sin IVA
  retencionFuente: 400.0 × (2.5 / 100) = 10.0
}
```

**Resultado en factura:**
- Subtotal: 400.0
- IVA: 76.0
- Retención: 10.0
- Total: 400.0 + 76.0 - 10.0 = **466.0**

### **Opción B: Enviar el subtotal bruto (CON IVA)**

Si calculas el subtotal bruto desde los items (que es 476.0):

```javascript
// Calcular subtotal bruto desde los items
const subtotalBruto = orden.items.reduce((sum, item) => 
  sum + (item.cantidad * item.precioUnitario), 0
);  // 476.0 (CON IVA)

{
  ordenId: 100,
  subtotal: subtotalBruto,  // 476.0 (CON IVA)
  descuentos: 0.0,
  iva: 476.0 × (0.19 / 1.19) = 76.0,  // ✅ Extraer IVA del subtotal
  retencionFuente: (476.0 / 1.19) × (2.5 / 100) = 10.0
}
```

**Resultado en factura:**
- Subtotal: 476.0
- IVA: 76.0
- Retención: 10.0
- Total: 476.0 + 76.0 - 10.0 = **542.0** ❌ (Incorrecto, duplica IVA)

---

## ✅ Solución Correcta

### **Si el subtotal de la orden es SIN IVA:**

```javascript
// Usar el subtotal de la orden (SIN IVA)
const baseImponible = orden.subtotal - orden.descuentos;  // 400.0

// Calcular IVA sobre el subtotal sin IVA
const iva = baseImponible * (19 / 100);  // 400.0 × 0.19 = 76.0

// Calcular retención sobre el subtotal sin IVA
const retencionFuente = baseImponible * (porcentajeRetencion / 100);

{
  ordenId: orden.id,
  subtotal: orden.subtotal,  // ✅ 400.0 (SIN IVA)
  descuentos: orden.descuentos,
  iva: iva,  // ✅ 76.0 (calculado sobre subtotal sin IVA)
  retencionFuente: retencionFuente
}
```

### **Si el subtotal que envías YA incluye IVA:**

```javascript
// Calcular subtotal bruto desde los items (CON IVA)
const subtotalBruto = orden.items.reduce((sum, item) => 
  sum + (item.cantidad * item.precioUnitario), 0
);  // 476.0 (CON IVA)

const baseImponible = subtotalBruto - orden.descuentos;  // 476.0

// Extraer IVA del subtotal que ya lo incluye
const iva = baseImponible * (0.19 / 1.19);  // 476.0 × 0.15966387 = 76.0

// Calcular retención sobre el subtotal sin IVA
const subtotalSinIva = baseImponible / 1.19;  // 400.0
const retencionFuente = subtotalSinIva * (porcentajeRetencion / 100);

{
  ordenId: orden.id,
  subtotal: subtotalBruto,  // ✅ 476.0 (CON IVA)
  descuentos: orden.descuentos,
  iva: iva,  // ✅ 76.0 (extraído del subtotal)
  retencionFuente: retencionFuente
}
```

---

## 🎯 Recomendación

### **Usar el subtotal de la orden (SIN IVA) - Opción A**

**Razones:**
1. ✅ El backend ya calculó el subtotal correctamente
2. ✅ Es más simple: solo multiplicas por el porcentaje de IVA
3. ✅ Evita duplicar cálculos
4. ✅ Consistente con cómo el backend maneja las órdenes

**Código recomendado:**
```javascript
// Usar el subtotal de la orden (que es SIN IVA)
const baseImponible = orden.subtotal - orden.descuentos;

// Calcular IVA sobre el subtotal sin IVA
const iva = baseImponible * (19 / 100);
const ivaRedondeado = Math.round(iva * 100) / 100;

// Calcular retención sobre el subtotal sin IVA
const porcentajeRetencion = porcentajeRetencionFuente || 0;
const retencionFuente = baseImponible * (porcentajeRetencion / 100);
const retencionRedondeado = Math.round(retencionFuente * 100) / 100;

// Payload
{
  ordenId: orden.id,
  subtotal: orden.subtotal,  // ✅ Subtotal SIN IVA de la orden
  descuentos: orden.descuentos,
  iva: ivaRedondeado,  // ✅ Calculado sobre subtotal sin IVA
  retencionFuente: Math.max(0, retencionRedondeado),
  formaPago: formData.formaPago || 'EFECTIVO',
  observaciones: formData.observaciones || `Factura generada desde orden #${orden.numero}`,
  clienteId: Number(clienteFactura.id)
}
```

---

## 📊 Ejemplo Completo

### Escenario:
- **Items**: 2 × 119.0 + 1 × 238.0 = 476.0 (con IVA)
- **Subtotal de orden**: 400.0 (sin IVA, calculado por backend)
- **Descuentos**: 0
- **IVA Rate**: 19%
- **Retención Rate**: 2.5%

### Cálculo Correcto (Opción A):

```javascript
const baseImponible = 400.0 - 0 = 400.0;
const iva = 400.0 × (19 / 100) = 76.0;
const retencionFuente = 400.0 × (2.5 / 100) = 10.0;
```

### Payload:
```json
{
  "ordenId": 100,
  "subtotal": 400.0,  // ✅ De la orden (SIN IVA)
  "descuentos": 0.0,
  "iva": 76.0,  // ✅ Calculado sobre 400.0
  "retencionFuente": 10.0,  // ✅ Calculado sobre 400.0
  "total": 466.0  // Backend: 400.0 + 76.0 - 10.0
}
```

### Verificación:
- Subtotal sin IVA: 400.0
- IVA (19%): 76.0
- Subtotal con IVA: 476.0 ✅
- Retención (2.5% sobre 400.0): 10.0
- Total: 400.0 + 76.0 - 10.0 = 466.0 ✅

---

## 🔍 Si Envías Subtotal CON IVA (Opción B)

Si por alguna razón envías el subtotal bruto (476.0 con IVA):

```javascript
// Calcular subtotal bruto desde items
const subtotalBruto = orden.items.reduce((sum, item) => 
  sum + (item.cantidad * item.precioUnitario), 0
);  // 476.0 (CON IVA)

const baseImponible = subtotalBruto - orden.descuentos;  // 476.0

// Extraer IVA del subtotal
const iva = baseImponible * (0.19 / 1.19);  // 76.0

// Calcular retención sobre subtotal sin IVA
const subtotalSinIva = baseImponible / 1.19;  // 400.0
const retencionFuente = subtotalSinIva * (2.5 / 100);  // 10.0

{
  ordenId: orden.id,
  subtotal: subtotalBruto,  // 476.0 (CON IVA)
  descuentos: orden.descuentos,
  iva: iva,  // 76.0 (extraído)
  retencionFuente: retencionFuente  // 10.0
}
```

**⚠️ Problema:** El backend suma el IVA al total:
```
total = subtotal + iva - retencionFuente
total = 476.0 + 76.0 - 10.0 = 542.0  // ❌ Incorrecto (duplica IVA)
```

**Solución:** El backend debería usar el subtotal sin IVA para calcular el total, pero actualmente usa el subtotal que envías.

---

## ✅ Conclusión y Recomendación Final

### **Usa el subtotal de la orden (SIN IVA):**

1. ✅ El backend ya calculó el subtotal correctamente (sin IVA)
2. ✅ Calcula el IVA sobre ese subtotal: `iva = subtotal × 0.19`
3. ✅ Calcula la retención sobre ese subtotal: `retencionFuente = subtotal × (porcentaje / 100)`
4. ✅ Envía el subtotal de la orden tal cual

**Código final recomendado:**
```javascript
const baseImponible = orden.subtotal - orden.descuentos;
const iva = baseImponible * (19 / 100);
const retencionFuente = baseImponible * (porcentajeRetencionFuente / 100);

{
  ordenId: orden.id,
  subtotal: orden.subtotal,  // ✅ SIN IVA (de la orden)
  descuentos: orden.descuentos,
  iva: Math.round(iva * 100) / 100,
  retencionFuente: Math.round(retencionFuente * 100) / 100
}
```

**NO necesitas calcular el subtotal bruto desde los items, usa directamente `orden.subtotal`.**

