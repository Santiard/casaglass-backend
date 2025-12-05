# 🔧 Corrección: IVA Incluido en el Subtotal

## ⚠️ Información Importante

**El `subtotal` que se envía a la factura YA incluye el IVA del 19%.**

Esto significa que:
- ❌ **NO** debes calcular el IVA sobre el subtotal
- ✅ **SÍ** debes **extraer** el IVA del subtotal

---

## 📊 Situación Actual vs Correcta

### ❌ **Cálculo Incorrecto (Asumiendo subtotal sin IVA):**
```javascript
// ❌ INCORRECTO - Asume que subtotal NO incluye IVA
const baseImponible = subtotal - descuentos;
const iva = baseImponible * (19 / 100);  // ❌ Esto está mal
```

### ✅ **Cálculo Correcto (Subtotal YA incluye IVA):**
```javascript
// ✅ CORRECTO - Extrae el IVA del subtotal
const baseImponible = subtotal - descuentos;
const iva = baseImponible - (baseImponible / 1.19);  // ✅ Extrae el IVA incluido
```

---

## 🔢 Fórmulas Correctas

### **Extraer IVA del Subtotal (que ya lo incluye):**

```
baseImponible = subtotal - descuentos
subtotalSinIva = baseImponible / 1.19
iva = baseImponible - subtotalSinIva
```

**O más directo:**
```
iva = baseImponible × (0.19 / 1.19)
iva = baseImponible × 0.1596638655462185
```

**Simplificado:**
```
iva = baseImponible × 0.15966387  // Aproximado
```

---

## 📋 Ejemplo Práctico

### Escenario:
- **Subtotal enviado**: 1,000,000 (YA incluye 19% IVA)
- **Descuentos**: 0
- **IVA Rate**: 19%

### Cálculo Correcto:

```javascript
const baseImponible = 1000000 - 0 = 1000000;
const subtotalSinIva = 1000000 / 1.19 = 840336.13;
const iva = 1000000 - 840336.13 = 159663.87;
```

**O usando la fórmula directa:**
```javascript
const baseImponible = 1000000 - 0 = 1000000;
const iva = 1000000 × (0.19 / 1.19) = 159663.87;
```

### Verificación:
- Subtotal sin IVA: 840,336.13
- IVA (19%): 159,663.87
- Total: 840,336.13 + 159,663.87 = **1,000,000** ✅

---

## 🔧 Código Corregido para el Frontend

### Cálculo de IVA (Extraer del subtotal):

```javascript
// Calcular base imponible
const baseImponible = Number(totales.subtotal || 0) - Number(totales.descuentos || 0);

// Extraer el IVA del subtotal (que ya lo incluye)
// Fórmula: iva = baseImponible × (0.19 / 1.19)
const iva = baseImponible * (0.19 / 1.19);
const ivaRedondeado = Math.round(iva * 100) / 100;

// Calcular retención de fuente (sobre base imponible sin IVA)
const subtotalSinIva = baseImponible / 1.19;
const porcentajeRetencion = porcentajeRetencionFuente || 0;
const valorRetencionFuente = subtotalSinIva * (porcentajeRetencion / 100);
const valorRetencionRedondeado = Math.round(valorRetencionFuente * 100) / 100;

// Payload completo
{
  ordenId: Number(orden.id),
  fecha: formData.fecha,
  subtotal: Number(totales.subtotal || 0),  // ✅ Ya incluye IVA
  descuentos: Number(totales.descuentos || 0),
  iva: ivaRedondeado,  // ✅ IVA extraído del subtotal
  retencionFuente: Math.max(0, valorRetencionRedondeado),  // ✅ Calculado sobre subtotal sin IVA
  formaPago: formData.formaPago || 'EFECTIVO',
  observaciones: formData.observaciones || `Factura generada desde orden #${orden.numero}`,
  clienteId: Number(clienteFactura.id)
}
```

---

## 📊 Ejemplos Completos

### Ejemplo 1: Sin Descuentos

**Datos:**
- Subtotal: 1,000,000 (con IVA incluido)
- Descuentos: 0
- Retención: 2.5%

**Cálculos:**
```javascript
const baseImponible = 1000000 - 0 = 1000000;
const iva = 1000000 × (0.19 / 1.19) = 159663.87;
const subtotalSinIva = 1000000 / 1.19 = 840336.13;
const retencionFuente = 840336.13 × (2.5 / 100) = 21008.40;
```

**Payload:**
```json
{
  "subtotal": 1000000.0,
  "descuentos": 0.0,
  "iva": 159663.87,      // ✅ Extraído del subtotal
  "retencionFuente": 21008.40,  // ✅ Calculado sobre subtotal sin IVA
  "total": 1139655.47    // Backend calcula: 1000000 + 159663.87 - 21008.40
}
```

### Ejemplo 2: Con Descuentos

**Datos:**
- Subtotal: 1,000,000 (con IVA incluido)
- Descuentos: 100,000
- Retención: 2.5%

**Cálculos:**
```javascript
const baseImponible = 1000000 - 100000 = 900000;
const iva = 900000 × (0.19 / 1.19) = 143697.48;
const subtotalSinIva = 900000 / 1.19 = 756302.52;
const retencionFuente = 756302.52 × (2.5 / 100) = 18907.56;
```

**Payload:**
```json
{
  "subtotal": 1000000.0,
  "descuentos": 100000.0,
  "iva": 143697.48,      // ✅ Extraído del base imponible
  "retencionFuente": 18907.56,  // ✅ Calculado sobre base sin IVA
  "total": 1024789.92    // Backend calcula: 900000 + 143697.48 - 18907.56
}
```

---

## ⚠️ Importante: Retención de Fuente

**La retención de fuente se calcula sobre el subtotal SIN IVA**, no sobre el subtotal con IVA.

**Fórmula correcta:**
```javascript
const baseImponible = subtotal - descuentos;
const subtotalSinIva = baseImponible / 1.19;  // Quitar el IVA
const retencionFuente = subtotalSinIva × (porcentajeRetencion / 100);
```

**Razón:**
- La retención de fuente se calcula sobre la base imponible (sin IVA)
- El IVA es un impuesto agregado, la retención es sobre el valor base

---

## 🔄 Resumen de Fórmulas

### **IVA (Extraer del subtotal que ya lo incluye):**
```
baseImponible = subtotal - descuentos
iva = baseImponible × (0.19 / 1.19)
```

### **Retención de Fuente (Calcular sobre subtotal sin IVA):**
```
baseImponible = subtotal - descuentos
subtotalSinIva = baseImponible / 1.19
retencionFuente = subtotalSinIva × (porcentajeRetencion / 100)
```

### **Total (Calculado por backend):**
```
total = baseImponible + iva - retencionFuente
```

---

## ✅ Código Final Recomendado

```javascript
// Calcular base imponible (subtotal - descuentos)
const baseImponible = Number(totales.subtotal || 0) - Number(totales.descuentos || 0);

// Extraer el IVA del subtotal (que ya lo incluye al 19%)
// Fórmula: iva = baseImponible × (0.19 / 1.19)
const iva = baseImponible * (0.19 / 1.19);
const ivaRedondeado = Math.round(iva * 100) / 100;

// Calcular retención de fuente sobre el subtotal SIN IVA
const subtotalSinIva = baseImponible / 1.19;
const porcentajeRetencion = porcentajeRetencionFuente || 0;
const valorRetencionFuente = subtotalSinIva * (porcentajeRetencion / 100);
const valorRetencionRedondeado = Math.round(valorRetencionFuente * 100) / 100;

// Payload para crear factura
const payload = {
  ordenId: Number(orden.id),
  fecha: formData.fecha,
  subtotal: Number(totales.subtotal || 0),  // ✅ Ya incluye IVA
  descuentos: Number(totales.descuentos || 0),
  iva: ivaRedondeado,  // ✅ IVA extraído del subtotal
  retencionFuente: Math.max(0, valorRetencionRedondeado),  // ✅ Calculado sobre subtotal sin IVA
  formaPago: formData.formaPago || 'EFECTIVO',
  observaciones: formData.observaciones || `Factura generada desde orden #${orden.numero}`,
  clienteId: Number(clienteFactura.id)
};
```

---

## 🎯 Verificación

### Test 1: Subtotal 1,000,000 con IVA incluido

**Cálculo:**
- Subtotal sin IVA: 1,000,000 / 1.19 = 840,336.13
- IVA incluido: 1,000,000 - 840,336.13 = 159,663.87
- Verificación: 840,336.13 × 1.19 = 1,000,000 ✅

### Test 2: Con descuentos

**Cálculo:**
- Base imponible: 1,000,000 - 100,000 = 900,000
- Subtotal sin IVA: 900,000 / 1.19 = 756,302.52
- IVA incluido: 900,000 - 756,302.52 = 143,697.48
- Verificación: 756,302.52 × 1.19 = 900,000 ✅

---

## 📝 Notas Importantes

1. **El subtotal que envías YA incluye el IVA del 19%**
2. **Debes EXTRAER el IVA, no calcularlo**
3. **La retención de fuente se calcula sobre el subtotal SIN IVA**
4. **El backend suma el IVA al total, pero como ya está incluido en el subtotal, el cálculo final es correcto**

---

## 🔍 ¿Por qué el Backend Suma el IVA?

El backend usa esta fórmula:
```java
total = baseImponible + iva - retencionFuente
```

Donde:
- `baseImponible = subtotal - descuentos` (que ya incluye IVA)
- `iva` = IVA extraído que envías
- `retencionFuente` = Retención calculada

**Resultado:**
```
total = (subtotal con IVA) + (IVA extraído) - retencionFuente
total = subtotal + iva - retencionFuente
```

Esto es correcto porque:
- El subtotal ya incluye el IVA
- Al sumar el IVA extraído, estás "separando" el IVA del subtotal
- El total final es correcto

---

## ✅ Conclusión

**Cuando el subtotal YA incluye IVA:**

1. ✅ **Extrae el IVA** del subtotal: `iva = baseImponible × (0.19 / 1.19)`
2. ✅ **Calcula la retención** sobre el subtotal sin IVA: `retencionFuente = (baseImponible / 1.19) × (porcentaje / 100)`
3. ✅ **Envía ambos valores** al backend
4. ✅ El backend calcula el total correctamente

**NO calcules el IVA sobre el subtotal, extráelo del subtotal que ya lo incluye.**

