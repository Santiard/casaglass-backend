# 💰 Especificación: Campo `retencionFuente` en POST /api/facturas

## ❌ Problema Identificado

El frontend está enviando `retencionFuente` como **porcentaje** (ej: `2.5`), pero el backend espera el **valor calculado en dinero** (ej: `25000`).

---

## ✅ Respuesta: ¿Qué espera el backend?

### **El backend espera el VALOR CALCULADO EN DINERO, NO el porcentaje**

**Ejemplo:**
- ❌ **Incorrecto**: `retencionFuente: 2.5` (porcentaje)
- ✅ **Correcto**: `retencionFuente: 25000` (valor en dinero)

---

## 📊 Evidencia del Código

### 1. Fórmula de Cálculo del Total

En `Factura.calcularTotal()` (línea 132-136):

```java
public void calcularTotal() {
    double baseImponible = subtotal - descuentos;
    double totalCalculado = baseImponible + iva - retencionFuente;
    // Redondear a 2 decimales
    this.total = Math.round(totalCalculado * 100.0) / 100.0;
}
```

**Análisis:**
- El `retencionFuente` se **resta directamente** del total
- Si fuera porcentaje, habría que multiplicarlo: `baseImponible * (retencionFuente / 100)`
- Como se resta directamente, **debe ser un valor monetario**

### 2. Asignación Directa en el Servicio

En `FacturaService.crearFactura()` (línea 80):

```java
factura.setRetencionFuente(facturaDTO.getRetencionFuente() != null ? facturaDTO.getRetencionFuente() : 0.0);
```

**Análisis:**
- El valor se asigna **directamente** sin ningún cálculo
- No hay conversión de porcentaje a valor
- Confirma que espera el valor monetario

### 3. Comentario en la Entidad

En `Factura.java` (línea 82-84):

```java
/**
 * Retención en la fuente
 * Impuesto retenido del cliente
 */
```

**Análisis:**
- Dice "Impuesto retenido", lo que indica un **valor monetario**, no un porcentaje

---

## 🔢 Ejemplo de Cálculo Correcto

### Escenario:
- **Subtotal**: 1,000,000.0
- **Descuentos**: 0.0
- **IVA**: 190,000.0 (19% de 1,000,000)
- **Porcentaje de retención**: 2.5%
- **Base para retención**: 1,000,000.0

### Cálculo de Retención:
```
retencionFuente = baseImponible × (porcentaje / 100)
retencionFuente = 1,000,000 × (2.5 / 100)
retencionFuente = 1,000,000 × 0.025
retencionFuente = 25,000.0
```

### Payload Correcto:
```json
{
  "ordenId": 100,
  "fecha": "2025-01-15",
  "subtotal": 1000000.0,
  "descuentos": 0.0,
  "iva": 190000.0,
  "retencionFuente": 25000.0,  // ✅ Valor calculado, NO 2.5
  "formaPago": "EFECTIVO",
  "observaciones": "Factura generada desde orden #1001",
  "clienteId": 5
}
```

### Cálculo del Total:
```
baseImponible = subtotal - descuentos = 1,000,000 - 0 = 1,000,000
total = baseImponible + iva - retencionFuente
total = 1,000,000 + 190,000 - 25,000
total = 1,165,000.0
```

---

## 🔧 Corrección en el Frontend

### Código Actual (Incorrecto):
```javascript
{
  ordenId: Number(orden.id),
  fecha: formData.fecha,
  subtotal: Number(totales.subtotal || 0),
  descuentos: Number(totales.descuentos || 0),
  iva: Number(ivaRate || 0),
  retencionFuente: Math.max(0, porcentajeRetencionFuente), // ❌ Porcentaje
  formaPago: formData.formaPago || 'EFECTIVO',
  observaciones: formData.observaciones || `Factura generada desde orden #${orden.numero}`,
  clienteId: Number(clienteFactura.id)
}
```

### Código Corregido:
```javascript
// Calcular el valor de retención en dinero
const baseImponible = Number(totales.subtotal || 0) - Number(totales.descuentos || 0);
const valorRetencionFuente = baseImponible * (porcentajeRetencionFuente / 100);

{
  ordenId: Number(orden.id),
  fecha: formData.fecha,
  subtotal: Number(totales.subtotal || 0),
  descuentos: Number(totales.descuentos || 0),
  iva: Number(ivaRate || 0),
  retencionFuente: Math.max(0, valorRetencionFuente), // ✅ Valor calculado
  formaPago: formData.formaPago || 'EFECTIVO',
  observaciones: formData.observaciones || `Factura generada desde orden #${orden.numero}`,
  clienteId: Number(clienteFactura.id)
}
```

### Versión Más Completa (con redondeo):
```javascript
// Calcular el valor de retención en dinero
const baseImponible = Number(totales.subtotal || 0) - Number(totales.descuentos || 0);
const porcentajeRetencion = porcentajeRetencionFuente || 0;
const valorRetencionFuente = baseImponible * (porcentajeRetencion / 100);
const valorRetencionRedondeado = Math.round(valorRetencionFuente * 100) / 100; // Redondear a 2 decimales

{
  ordenId: Number(orden.id),
  fecha: formData.fecha,
  subtotal: Number(totales.subtotal || 0),
  descuentos: Number(totales.descuentos || 0),
  iva: Number(ivaRate || 0),
  retencionFuente: Math.max(0, valorRetencionRedondeado), // ✅ Valor calculado y redondeado
  formaPago: formData.formaPago || 'EFECTIVO',
  observaciones: formData.observaciones || `Factura generada desde orden #${orden.numero}`,
  clienteId: Number(clienteFactura.id)
}
```

---

## 📋 Resumen

| Campo | Tipo Esperado | Ejemplo |
|-------|---------------|---------|
| `retencionFuente` | **Double (valor monetario)** | `25000.0` |
| **NO** | Porcentaje | `2.5` ❌ |

### Fórmula de Conversión:
```
valorRetencionFuente = baseImponible × (porcentajeRetencion / 100)
```

Donde:
- `baseImponible = subtotal - descuentos`
- `porcentajeRetencion` = porcentaje de retención (ej: 2.5 para 2.5%)

---

## ✅ Verificación

### Test 1: Con Retención
- **Subtotal**: 1,000,000
- **Descuentos**: 0
- **Porcentaje retención**: 2.5%
- **Valor esperado**: `retencionFuente = 25,000`

### Test 2: Sin Retención
- **Subtotal**: 500,000
- **Descuentos**: 0
- **Porcentaje retención**: 0%
- **Valor esperado**: `retencionFuente = 0`

### Test 3: Con Descuentos
- **Subtotal**: 1,000,000
- **Descuentos**: 100,000
- **Porcentaje retención**: 2.5%
- **Base imponible**: 900,000
- **Valor esperado**: `retencionFuente = 22,500` (900,000 × 0.025)

---

## 🎯 Conclusión

**El backend espera el VALOR CALCULADO EN DINERO para `retencionFuente`, no el porcentaje.**

**Corrección necesaria en el frontend:**
1. Calcular el valor: `baseImponible × (porcentaje / 100)`
2. Redondear a 2 decimales
3. Enviar el valor calculado, no el porcentaje

