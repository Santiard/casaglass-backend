# 📤 Qué Envía el Frontend al Crear una Factura

## 📋 Estructura del Payload (POST /api/facturas)

Según `FacturaCreateDTO`, el frontend debe enviar:

```json
{
  "ordenId": 100,                    // ✅ OBLIGATORIO - ID de la orden
  "clienteId": 5,                    // ⚠️ OPCIONAL - Si no se envía, usa el cliente de la orden
  "fecha": "2025-01-15",             // ⚠️ OPCIONAL - Si no se envía, usa fecha actual
  "subtotal": 1000000.0,             // ✅ OBLIGATORIO - Subtotal de la orden (con IVA incluido)
  "descuentos": 0.0,                 // ⚠️ OPCIONAL - Por defecto 0.0
  "iva": 190000.0,                   // ⚠️ OPCIONAL - Por defecto 0.0 (valor monetario)
  "retencionFuente": 12500.0,        // ⚠️ OPCIONAL - Por defecto 0.0 (valor monetario)
  "total": 1165000.0,                // ⚠️ OPCIONAL - Si no se envía, se calcula automáticamente
  "formaPago": "EFECTIVO",           // ⚠️ OPCIONAL
  "observaciones": "Factura...",      // ⚠️ OPCIONAL
  "numeroFactura": "FAC-001"         // ⚠️ OPCIONAL - Si no se envía, se genera automáticamente
}
```

---

## 🔍 Análisis del Código Actual

### Backend NO calcula automáticamente la retención

**Código en `FacturaService.crearFactura()` (línea 80):**
```java
factura.setRetencionFuente(facturaDTO.getRetencionFuente() != null ? facturaDTO.getRetencionFuente() : 0.0);
```

**Análisis:**
- ❌ El backend **NO calcula** la retención automáticamente
- ✅ El backend **asigna directamente** el valor que envía el frontend
- ✅ Si no se envía, usa `0.0` por defecto

---

## 💰 Retención de Fuente: 1.25%

### Fórmula de Cálculo:

```
retencionFuente = (subtotal - descuentos) × (1.25 / 100)
retencionFuente = baseImponible × 0.0125
```

### Ejemplo:

**Datos:**
- Subtotal: 1,000,000 (con IVA incluido)
- Descuentos: 0
- Retención: 1.25%

**Cálculo:**
```javascript
const baseImponible = 1000000 - 0 = 1000000;
const retencionFuente = 1000000 × 0.0125 = 12500.0;
```

---

## 📤 Qué Debe Enviar el Frontend

### Opción A: Frontend Calcula (Situación Actual)

```javascript
// Calcular base imponible
const baseImponible = Number(totales.subtotal || 0) - Number(totales.descuentos || 0);

// Calcular IVA: 19% del subtotal (que ya incluye IVA)
const iva = baseImponible * 0.19;
const ivaRedondeado = Math.round(iva * 100) / 100;

// Calcular retención de fuente: 1.25% del base imponible
const porcentajeRetencion = 1.25; // ✅ 1.25% (no 2.5%)
const valorRetencionFuente = baseImponible * (porcentajeRetencion / 100);
const valorRetencionRedondeado = Math.round(valorRetencionFuente * 100) / 100;

// Payload
{
  ordenId: Number(orden.id),
  fecha: formData.fecha,
  subtotal: Number(totales.subtotal || 0),  // ✅ Ya incluye IVA
  descuentos: Number(totales.descuentos || 0),
  iva: ivaRedondeado,  // ✅ 190000.0 (19% de 1000000)
  retencionFuente: Math.max(0, valorRetencionRedondeado),  // ✅ 12500.0 (1.25% de 1000000)
  formaPago: formData.formaPago || 'EFECTIVO',
  observaciones: formData.observaciones || `Factura generada desde orden #${orden.numero}`,
  clienteId: Number(clienteFactura.id)
}
```

### Opción B: Backend Calcula (Si se implementa)

Si quieres que el backend calcule automáticamente, el frontend solo enviaría:

```javascript
// Payload simplificado (sin calcular retención)
{
  ordenId: Number(orden.id),
  fecha: formData.fecha,
  subtotal: Number(totales.subtotal || 0),  // ✅ Ya incluye IVA
  descuentos: Number(totales.descuentos || 0),
  iva: ivaRedondeado,  // ✅ 190000.0 (19% de 1000000)
  // retencionFuente: NO se envía, el backend lo calcula
  formaPago: formData.formaPago || 'EFECTIVO',
  observaciones: formData.observaciones || `Factura generada desde orden #${orden.numero}`,
  clienteId: Number(clienteFactura.id)
}
```

Y el backend calcularía:
```java
// En FacturaService.crearFactura()
Double baseImponible = facturaDTO.getSubtotal() - facturaDTO.getDescuentos();
Double reteRate = obtenerReteRate(); // 1.25 desde BusinessSettings
Double retencionFuente = baseImponible * (reteRate / 100.0);
factura.setRetencionFuente(retencionFuente);
```

---

## 📊 Ejemplo Completo

### Escenario:
- **Subtotal**: 1,000,000 (con IVA incluido)
- **Descuentos**: 0
- **IVA**: 19% = 190,000
- **Retención**: 1.25% = 12,500

### Payload que Envía el Frontend (Opción A - Actual):

```json
{
  "ordenId": 100,
  "fecha": "2025-01-15",
  "subtotal": 1000000.0,
  "descuentos": 0.0,
  "iva": 190000.0,           // ✅ Calculado en frontend: 1000000 × 0.19
  "retencionFuente": 12500.0, // ✅ Calculado en frontend: 1000000 × 0.0125
  "formaPago": "EFECTIVO",
  "observaciones": "Factura generada desde orden #1001",
  "clienteId": 5
}
```

### Cálculo del Total (Backend):

```java
// Factura.calcularTotal()
baseImponible = 1000000 - 0 = 1000000
total = baseImponible + iva - retencionFuente
total = 1000000 + 190000 - 12500 = 1167500.0
```

---

## 🎯 Resumen

| Campo | Tipo | Obligatorio | Valor Esperado | Ejemplo |
|-------|------|-------------|----------------|---------|
| `ordenId` | Long | ✅ SÍ | ID de la orden | `100` |
| `clienteId` | Long | ⚠️ NO | ID del cliente | `5` |
| `fecha` | LocalDate | ⚠️ NO | Fecha de factura | `"2025-01-15"` |
| `subtotal` | Double | ✅ SÍ | Subtotal con IVA | `1000000.0` |
| `descuentos` | Double | ⚠️ NO | Descuentos | `0.0` |
| `iva` | Double | ⚠️ NO | IVA en dinero | `190000.0` |
| `retencionFuente` | Double | ⚠️ NO | Retención en dinero | `12500.0` (1.25%) |
| `total` | Double | ⚠️ NO | Total (se calcula si no se envía) | `1167500.0` |
| `formaPago` | String | ⚠️ NO | Forma de pago | `"EFECTIVO"` |
| `observaciones` | String | ⚠️ NO | Observaciones | `"Factura..."` |
| `numeroFactura` | String | ⚠️ NO | Número de factura | `"FAC-001"` |

---

## ⚠️ Notas Importantes

1. **Retención de fuente: 1.25%** (no 2.5%)
2. **El backend NO calcula automáticamente** la retención (actualmente)
3. **El frontend debe calcular** y enviar el valor monetario
4. **El subtotal ya incluye IVA** del 19%
5. **La retención se calcula sobre el subtotal con IVA incluido**

---

## 🔧 Si Quieres que el Backend Calcule Automáticamente

Si quieres que el backend calcule la retención automáticamente, necesitarías modificar `FacturaService.crearFactura()` para:

1. Obtener el `reteRate` desde `BusinessSettings` (1.25%)
2. Calcular la retención sobre `baseImponible`
3. Asignar el valor calculado

¿Quieres que implemente esto?

