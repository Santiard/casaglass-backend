# 💰 Cálculo de Retención de Fuente al Facturar una Orden

## 📋 Situación Actual

**El backend NO calcula automáticamente la retención de fuente.** El frontend debe calcularla y enviarla como valor monetario.

---

## 🔍 Código del Backend

### En `FacturaService.crearFactura()`:

```java
// Línea 80
factura.setRetencionFuente(facturaDTO.getRetencionFuente() != null ? facturaDTO.getRetencionFuente() : 0.0);
```

**Análisis:**
- ✅ El backend **asigna directamente** el valor que viene en el DTO
- ❌ **NO hay cálculo automático** en el backend
- ❌ **NO consulta** `BusinessSettings` para obtener el porcentaje
- ✅ Si no viene valor, usa `0.0` por defecto

---

## 📊 Fórmula Actual (Según Documentación)

### **Retención de Fuente:**

```javascript
const baseImponible = subtotal - descuentos;
const valorRetencionFuente = baseImponible * (porcentajeRetencion / 100);
```

**Donde:**
- `subtotal` = Subtotal de la orden (que **YA incluye IVA**)
- `descuentos` = Descuentos aplicados
- `porcentajeRetencion` = Porcentaje de retención (ej: 2.5 para 2.5%)

---

## 🔢 Ejemplo Práctico

### Escenario:
- **Subtotal de orden**: 1,000,000 (con IVA incluido)
- **Descuentos**: 0
- **Porcentaje retención**: 2.5%

### Cálculo:
```javascript
const baseImponible = 1000000 - 0 = 1000000;
const valorRetencionFuente = 1000000 * (2.5 / 100);
const valorRetencionFuente = 1000000 * 0.025;
const valorRetencionFuente = 25000.0;
```

### Payload:
```json
{
  "ordenId": 100,
  "subtotal": 1000000.0,
  "descuentos": 0.0,
  "iva": 190000.0,  // 19% de 1000000
  "retencionFuente": 25000.0,  // ✅ 2.5% de 1000000
  "total": 1165000.0  // Backend: 1000000 + 190000 - 25000
}
```

---

## ⚠️ Importante: Base de Cálculo

**La retención de fuente se calcula sobre el subtotal CON IVA incluido.**

**Razón:**
- El subtotal de la orden ya incluye el IVA del 19%
- La retención se calcula sobre ese valor total
- **NO se resta el IVA antes de calcular la retención**

---

## 🔧 Código Completo para el Frontend

```javascript
// Calcular base imponible (subtotal - descuentos)
// NOTA: El subtotal de la orden YA incluye IVA del 19%
const baseImponible = Number(totales.subtotal || 0) - Number(totales.descuentos || 0);

// Calcular IVA: 19% del subtotal (que ya incluye IVA)
const iva = baseImponible * 0.19;
const ivaRedondeado = Math.round(iva * 100) / 100;

// Calcular retención de fuente sobre el base imponible
// Obtener porcentaje de retención (puede venir de BusinessSettings o del cliente)
const porcentajeRetencion = porcentajeRetencionFuente || 0; // Ej: 2.5 para 2.5%
const valorRetencionFuente = baseImponible * (porcentajeRetencion / 100);
const valorRetencionRedondeado = Math.round(valorRetencionFuente * 100) / 100;

// Payload para crear factura
{
  ordenId: Number(orden.id),
  fecha: formData.fecha,
  subtotal: Number(totales.subtotal || 0),  // ✅ Ya incluye IVA
  descuentos: Number(totales.descuentos || 0),
  iva: ivaRedondeado,  // ✅ 19% del subtotal
  retencionFuente: Math.max(0, valorRetencionRedondeado),  // ✅ Porcentaje del subtotal
  formaPago: formData.formaPago || 'EFECTIVO',
  observaciones: formData.observaciones || `Factura generada desde orden #${orden.numero}`,
  clienteId: Number(clienteFactura.id)
}
```

---

## 📊 Ejemplos con Diferentes Escenarios

### Ejemplo 1: Sin Descuentos

**Datos:**
- Subtotal: 1,000,000 (con IVA)
- Descuentos: 0
- Retención: 2.5%

**Cálculo:**
```javascript
baseImponible = 1000000 - 0 = 1000000
retencionFuente = 1000000 × 0.025 = 25000.0
```

### Ejemplo 2: Con Descuentos

**Datos:**
- Subtotal: 1,000,000 (con IVA)
- Descuentos: 100,000
- Retención: 2.5%

**Cálculo:**
```javascript
baseImponible = 1000000 - 100000 = 900000
retencionFuente = 900000 × 0.025 = 22500.0
```

### Ejemplo 3: Sin Retención

**Datos:**
- Subtotal: 500,000 (con IVA)
- Descuentos: 0
- Retención: 0%

**Cálculo:**
```javascript
baseImponible = 500000 - 0 = 500000
retencionFuente = 500000 × 0 = 0.0
```

---

## 🎯 Resumen

| Concepto | Valor | Fórmula |
|----------|-------|---------|
| **Base Imponible** | `subtotal - descuentos` | Valor sobre el que se calcula |
| **Retención de Fuente** | `baseImponible × (porcentaje / 100)` | Valor monetario calculado |
| **Backend** | Asigna directamente | No calcula automáticamente |

---

## ⚠️ Notas Importantes

1. **El backend NO calcula automáticamente** la retención de fuente
2. **El frontend debe calcular** el valor monetario antes de enviarlo
3. **La retención se calcula sobre el subtotal CON IVA incluido**
4. **El porcentaje de retención** debe obtenerse del cliente o de `BusinessSettings`
5. **El valor debe enviarse en dinero**, no como porcentaje

---

## 🔄 Flujo Completo

1. **Frontend obtiene orden:**
   - Subtotal: 1,000,000 (con IVA)
   - Descuentos: 0

2. **Frontend calcula:**
   - Base imponible: 1,000,000
   - IVA: 1,000,000 × 0.19 = 190,000
   - Retención: 1,000,000 × 0.025 = 25,000

3. **Frontend envía a backend:**
   ```json
   {
     "subtotal": 1000000.0,
     "descuentos": 0.0,
     "iva": 190000.0,
     "retencionFuente": 25000.0
   }
   ```

4. **Backend calcula total:**
   ```java
   total = baseImponible + iva - retencionFuente
   total = 1000000 + 190000 - 25000 = 1165000
   ```

---

## ✅ Conclusión

**La retención de fuente se calcula en el frontend usando la fórmula:**
```
retencionFuente = (subtotal - descuentos) × (porcentajeRetencion / 100)
```

**Donde:**
- `subtotal` = Subtotal de la orden (con IVA incluido)
- `descuentos` = Descuentos aplicados
- `porcentajeRetencion` = Porcentaje de retención (ej: 2.5 para 2.5%)

**El backend solo recibe y almacena el valor calculado, no lo calcula automáticamente.**

