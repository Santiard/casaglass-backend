# 📊 Contabilidad: Retención de Fuente en Órdenes vs Facturas

## 🎯 Pregunta Clave

**¿El total de una orden debe tener la retención de fuente restada ya, o se da el total con todo y la retención se señala como monto separado?**

---

## 📋 Situación Actual en el Backend

### 1. **ÓRDENES** (Documento Interno/Preliminar)

**Fórmula actual:**
```java
total = subtotal - descuentos
```

**Campos disponibles:**
- `subtotal` - Subtotal sin IVA
- `descuentos` - Descuentos aplicados
- `total` - Total final (subtotal - descuentos)
- `tieneRetencionFuente` - **Boolean** que indica si aplica retención (NO el monto)

**Características:**
- ❌ **NO incluye IVA** en el total
- ❌ **NO incluye retención de fuente** en el total
- ✅ Solo tiene un **flag booleano** `tieneRetencionFuente`
- ✅ El total es el **valor base** sin impuestos

**Ejemplo:**
```json
{
  "id": 100,
  "subtotal": 1000000.0,
  "descuentos": 0.0,
  "total": 1000000.0,  // ✅ Solo subtotal - descuentos
  "tieneRetencionFuente": true  // ✅ Flag, NO el monto
}
```

### 2. **FACTURAS** (Documento Oficial)

**Fórmula actual:**
```java
total = (subtotal - descuentos) + iva - retencionFuente
```

**Campos disponibles:**
- `subtotal` - Subtotal sin IVA
- `descuentos` - Descuentos aplicados
- `iva` - IVA calculado (valor monetario)
- `retencionFuente` - Retención de fuente calculada (valor monetario)
- `total` - Total final con impuestos

**Características:**
- ✅ **SÍ incluye IVA** en el total
- ✅ **SÍ resta la retención de fuente** del total
- ✅ Tiene el **monto exacto** de retención de fuente
- ✅ El total es el **valor neto a pagar** después de impuestos

**Ejemplo:**
```json
{
  "id": 50,
  "subtotal": 1000000.0,
  "descuentos": 0.0,
  "iva": 190000.0,
  "retencionFuente": 25000.0,  // ✅ Monto exacto
  "total": 1165000.0  // ✅ Con IVA y sin retención: 1000000 + 190000 - 25000
}
```

---

## 💼 Según Contabilidad Colombiana

### **Enfoque Correcto (Actual):**

#### **ÓRDENES** (Documento Interno)
- ✅ **Total SIN impuestos**: `total = subtotal - descuentos`
- ✅ **Flag de retención**: `tieneRetencionFuente = true/false`
- ✅ **Propósito**: Documento preliminar, valor base de la venta

**Razón:**
- La orden es un documento **interno/preliminar**
- Los impuestos se calculan y aplican en la **factura oficial**
- El total de la orden representa el **valor de los productos** sin impuestos

#### **FACTURAS** (Documento Oficial)
- ✅ **Total CON impuestos**: `total = baseImponible + iva - retencionFuente`
- ✅ **Monto de retención**: `retencionFuente = valor calculado`
- ✅ **Propósito**: Documento oficial, valor neto a pagar

**Razón:**
- La factura es el **documento oficial** para efectos fiscales
- Debe mostrar **todos los impuestos** (IVA y retención)
- El total representa el **valor neto que el cliente debe pagar**

---

## 📊 Comparación: Orden vs Factura

| Concepto | Orden | Factura |
|----------|-------|---------|
| **Subtotal** | ✅ 1,000,000 | ✅ 1,000,000 |
| **Descuentos** | ✅ 0 | ✅ 0 |
| **IVA** | ❌ No incluido | ✅ 190,000 (incluido) |
| **Retención Fuente** | ⚠️ Solo flag (true/false) | ✅ 25,000 (monto exacto) |
| **Total** | ✅ 1,000,000 (sin impuestos) | ✅ 1,165,000 (con impuestos, sin retención) |

---

## 🔍 Análisis de la Situación Actual

### ✅ **Ventajas del Enfoque Actual:**

1. **Separación de responsabilidades:**
   - Orden = Valor de productos (sin impuestos)
   - Factura = Valor oficial con impuestos

2. **Flexibilidad:**
   - Puedes crear órdenes sin calcular impuestos
   - Los impuestos se calculan solo al facturar

3. **Claridad contable:**
   - La orden muestra el valor base
   - La factura muestra el valor neto a pagar

### ⚠️ **Consideraciones:**

1. **El total de la orden NO incluye retención:**
   - Si el cliente ve la orden, ve un total que **NO es el final**
   - El total final (con retención restada) solo aparece en la factura

2. **El flag `tieneRetencionFuente` es informativo:**
   - Solo indica si aplica retención
   - NO indica el monto ni cómo afecta el total

---

## 💡 Recomendaciones según Contabilidad

### **Opción 1: Mantener el Enfoque Actual (Recomendado)**

**Órdenes:**
- Total = subtotal - descuentos (sin impuestos)
- Flag `tieneRetencionFuente` para indicar si aplica
- **Propósito**: Documento interno, valor base

**Facturas:**
- Total = baseImponible + iva - retencionFuente (con impuestos)
- Monto exacto de retención de fuente
- **Propósito**: Documento oficial, valor neto a pagar

**Ventajas:**
- ✅ Separación clara entre documento interno y oficial
- ✅ Los impuestos solo se calculan al facturar
- ✅ Cumple con normativa contable colombiana

### **Opción 2: Agregar Campos a la Orden (Alternativa)**

Si necesitas mostrar el total con retención en la orden, podrías agregar:

```java
// Campos adicionales en Orden
private Double iva = 0.0;  // IVA calculado
private Double retencionFuente = 0.0;  // Retención calculada
private Double totalConImpuestos = 0.0;  // Total con impuestos
```

**Fórmulas:**
```java
total = subtotal - descuentos;  // Total base (actual)
totalConImpuestos = (subtotal - descuentos) + iva - retencionFuente;  // Total neto
```

**Ventajas:**
- ✅ Muestra el total final en la orden
- ✅ El cliente ve el monto exacto a pagar

**Desventajas:**
- ⚠️ Duplica lógica entre orden y factura
- ⚠️ Los impuestos se calculan antes de facturar

---

## 📋 Ejemplo Práctico Completo

### Escenario:
- Subtotal: 1,000,000
- Descuentos: 0
- IVA: 19%
- Retención: 2.5%

### **ORDEN** (Documento Interno):
```json
{
  "id": 100,
  "numero": 1001,
  "subtotal": 1000000.0,
  "descuentos": 0.0,
  "total": 1000000.0,  // ✅ Solo productos (sin impuestos)
  "tieneRetencionFuente": true,  // ✅ Flag informativo
  "venta": true,
  "credito": false
}
```

**Interpretación:**
- El cliente debe pagar **aproximadamente** 1,000,000
- Pero el total **real** (con impuestos) se calcula en la factura

### **FACTURA** (Documento Oficial):
```json
{
  "id": 50,
  "numeroFactura": "FAC-2025-001",
  "subtotal": 1000000.0,
  "descuentos": 0.0,
  "iva": 190000.0,  // ✅ IVA calculado
  "retencionFuente": 25000.0,  // ✅ Retención calculada
  "total": 1165000.0  // ✅ Total neto: 1000000 + 190000 - 25000
}
```

**Interpretación:**
- El cliente debe pagar **exactamente** 1,165,000
- Este es el **valor neto** después de impuestos

---

## 🎯 Respuesta Directa

### **¿El total de la orden debe tener la retención restada?**

**Respuesta: NO, según contabilidad colombiana y el enfoque actual:**

1. **Órdenes** (documento interno):
   - Total = subtotal - descuentos (sin impuestos)
   - Flag `tieneRetencionFuente` indica si aplica
   - **NO se resta la retención** del total

2. **Facturas** (documento oficial):
   - Total = baseImponible + iva - retencionFuente
   - **SÍ se resta la retención** del total
   - Muestra el valor neto a pagar

### **¿Se señala la retención como monto separado?**

**Respuesta: SÍ, pero solo en la factura:**

- En la **orden**: Solo un flag booleano (`tieneRetencionFuente`)
- En la **factura**: Monto exacto (`retencionFuente = 25000.0`)

---

## ✅ Conclusión

**El enfoque actual es correcto según contabilidad:**

1. **Órdenes** = Valor base sin impuestos
2. **Facturas** = Valor neto con impuestos (retención restada)

**El total de la orden NO debe tener la retención restada** porque:
- Es un documento preliminar
- Los impuestos se calculan al facturar
- El total final (con retención restada) aparece en la factura

**La retención se señala como monto separado** solo en la factura, donde se muestra:
- El monto exacto de retención
- Cómo afecta el total final

---

## 🔧 Si Necesitas Mostrar Total con Retención en la Orden

Si tu negocio requiere mostrar el total con retención en la orden (para que el cliente vea el monto exacto), puedes:

1. **Calcularlo en el frontend:**
   ```javascript
   const baseImponible = orden.subtotal - orden.descuentos;
   const iva = baseImponible * (ivaRate / 100);
   const retencionFuente = baseImponible * (retencionRate / 100);
   const totalConRetencion = baseImponible + iva - retencionFuente;
   ```

2. **O agregar campos a la orden** (requiere cambios en backend):
   - `iva` (Double)
   - `retencionFuente` (Double)
   - `totalConImpuestos` (Double)

Pero **el enfoque actual es el estándar contable** y funciona correctamente.

