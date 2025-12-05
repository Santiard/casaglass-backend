# 💰 Especificación: Campo `iva` en POST /api/facturas

## ✅ Respuesta: El backend espera el VALOR CALCULADO EN DINERO, NO el porcentaje

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
- El `iva` se **suma directamente** al total
- Si fuera porcentaje, habría que multiplicarlo: `baseImponible * (iva / 100)`
- Como se suma directamente, **debe ser un valor monetario**

### 2. Asignación Directa en el Servicio

En `FacturaService.crearFactura()` (línea 79):

```java
factura.setIva(facturaDTO.getIva() != null ? facturaDTO.getIva() : 0.0);
```

**Análisis:**
- El valor se asigna **directamente** sin ningún cálculo
- No hay conversión de porcentaje a valor
- Confirma que espera el valor monetario

### 3. Comentario en la Entidad (Confuso)

En `Factura.java` (línea 75-78):

```java
/**
 * IVA (Impuesto sobre el Valor Agregado)
 * Porcentaje de IVA aplicado sobre (subtotal - descuentos)
 */
```

**⚠️ NOTA:** Este comentario es **confuso**. Dice "Porcentaje" pero el campo almacena el **valor calculado**, no el porcentaje.

---

## 🔢 Ejemplo de Cálculo Correcto

### Escenario:
- **Subtotal**: 1,000,000.0
- **Descuentos**: 0.0
- **Porcentaje de IVA**: 19%
- **Base para IVA**: 1,000,000.0 (subtotal - descuentos)

### Cálculo de IVA:
```
iva = baseImponible × (porcentajeIva / 100)
iva = 1,000,000 × (19 / 100)
iva = 1,000,000 × 0.19
iva = 190,000.0
```

### Payload Correcto:
```json
{
  "ordenId": 100,
  "fecha": "2025-01-15",
  "subtotal": 1000000.0,
  "descuentos": 0.0,
  "iva": 190000.0,  // ✅ Valor calculado, NO 19
  "retencionFuente": 25000.0,
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

## 🔧 Código Correcto en el Frontend

### ⚠️ IMPORTANTE: El subtotal de la orden YA incluye IVA

**El subtotal que envías a la factura es el de la orden, que YA incluye el IVA del 19%.**

### Cálculo del IVA:

```javascript
// El subtotal de la orden YA incluye IVA, así que el IVA es simplemente:
const baseImponible = Number(totales.subtotal || 0) - Number(totales.descuentos || 0);
const valorIva = baseImponible * 0.19; // ✅ 19% del subtotal (que ya incluye IVA)
const valorIvaRedondeado = Math.round(valorIva * 100) / 100; // Redondear a 2 decimales

// Calcular el valor de retención en dinero
const porcentajeRetencion = porcentajeRetencionFuente || 0;
const valorRetencionFuente = baseImponible * (porcentajeRetencion / 100);
const valorRetencionRedondeado = Math.round(valorRetencionFuente * 100) / 100;

// Payload completo
{
  ordenId: Number(orden.id),
  fecha: formData.fecha,
  subtotal: Number(totales.subtotal || 0),
  descuentos: Number(totales.descuentos || 0),
  iva: valorIvaRedondeado,  // ✅ Valor calculado (ej: 190000.0)
  retencionFuente: Math.max(0, valorRetencionRedondeado),  // ✅ Valor calculado (ej: 25000.0)
  formaPago: formData.formaPago || 'EFECTIVO',
  observaciones: formData.observaciones || `Factura generada desde orden #${orden.numero}`,
  clienteId: Number(clienteFactura.id)
}
```

---

## 📋 Comparación: IVA vs Retención de Fuente

| Campo | Tipo Esperado | Fórmula de Cálculo | Ejemplo |
|-------|---------------|-------------------|---------|
| `iva` | **Double (valor monetario)** | `baseImponible × (porcentajeIva / 100)` | `190000.0` |
| `retencionFuente` | **Double (valor monetario)** | `baseImponible × (porcentajeRetencion / 100)` | `25000.0` |

**Ambos campos esperan VALOR CALCULADO, NO porcentaje.**

---

## 🔢 Ejemplos Prácticos

### Ejemplo 1: Con IVA 19% y Retención 2.5%

**Datos:**
- Subtotal: 1,000,000
- Descuentos: 0
- IVA Rate: 19%
- Retención Rate: 2.5%

**Cálculos:**
```javascript
const baseImponible = 1000000 - 0 = 1000000;
const iva = 1000000 × (19 / 100) = 190000;
const retencionFuente = 1000000 × (2.5 / 100) = 25000;
```

**Payload:**
```json
{
  "subtotal": 1000000.0,
  "descuentos": 0.0,
  "iva": 190000.0,           // ✅ Valor calculado
  "retencionFuente": 25000.0, // ✅ Valor calculado
  "total": 1165000.0          // Calculado por backend: 1000000 + 190000 - 25000
}
```

### Ejemplo 2: Con Descuentos

**Datos:**
- Subtotal: 1,000,000
- Descuentos: 100,000
- IVA Rate: 19%
- Retención Rate: 2.5%

**Cálculos:**
```javascript
const baseImponible = 1000000 - 100000 = 900000;
const iva = 900000 × (19 / 100) = 171000;
const retencionFuente = 900000 × (2.5 / 100) = 22500;
```

**Payload:**
```json
{
  "subtotal": 1000000.0,
  "descuentos": 100000.0,
  "iva": 171000.0,            // ✅ Calculado sobre base imponible (900000)
  "retencionFuente": 22500.0, // ✅ Calculado sobre base imponible (900000)
  "total": 1048500.0          // Calculado por backend: 900000 + 171000 - 22500
}
```

### Ejemplo 3: Sin IVA ni Retención

**Datos:**
- Subtotal: 500,000
- Descuentos: 0
- IVA Rate: 0%
- Retención Rate: 0%

**Payload:**
```json
{
  "subtotal": 500000.0,
  "descuentos": 0.0,
  "iva": 0.0,                 // ✅ Sin IVA
  "retencionFuente": 0.0,     // ✅ Sin retención
  "total": 500000.0           // Calculado por backend: 500000 + 0 - 0
}
```

---

## 📊 Resumen de Campos del Body

| Campo | Tipo | Formato | Ejemplo | Notas |
|-------|------|---------|---------|-------|
| `subtotal` | Double | Valor monetario | `1000000.0` | Subtotal sin impuestos |
| `descuentos` | Double | Valor monetario | `100000.0` | Descuentos aplicados |
| `iva` | Double | **Valor monetario** | `190000.0` | ✅ **NO porcentaje** |
| `retencionFuente` | Double | **Valor monetario** | `25000.0` | ✅ **NO porcentaje** |
| `total` | Double | Valor monetario | `1165000.0` | Opcional (se calcula si no se envía) |

---

## ⚠️ Errores Comunes

### ❌ Error 1: Enviar IVA como porcentaje
```javascript
// ❌ INCORRECTO
{
  "iva": 19  // Porcentaje
}
```

### ✅ Correcto: Enviar IVA como valor calculado
```javascript
// ✅ CORRECTO
{
  "iva": 190000.0  // Valor calculado
}
```

### ❌ Error 2: Calcular IVA sobre subtotal en lugar de base imponible
```javascript
// ❌ INCORRECTO
const iva = subtotal * (porcentajeIva / 100);
```

### ✅ Correcto: Calcular IVA sobre base imponible
```javascript
// ✅ CORRECTO
const baseImponible = subtotal - descuentos;
const iva = baseImponible * (porcentajeIva / 100);
```

---

## 🎯 Fórmulas Completas

### Base Imponible:
```
baseImponible = subtotal - descuentos
```

### IVA:
```
iva = baseImponible × (porcentajeIva / 100)
```

### Retención de Fuente:
```
retencionFuente = baseImponible × (porcentajeRetencion / 100)
```

### Total (calculado por backend):
```
total = baseImponible + iva - retencionFuente
```

O expandido:
```
total = (subtotal - descuentos) + iva - retencionFuente
```

---

## ✅ Checklist de Validación

Antes de enviar el payload, verifica:

- [ ] `iva` es un valor monetario (ej: `190000.0`), NO un porcentaje (ej: `19`)
- [ ] `retencionFuente` es un valor monetario (ej: `25000.0`), NO un porcentaje (ej: `2.5`)
- [ ] `iva` se calcula sobre `baseImponible` (subtotal - descuentos), no sobre subtotal
- [ ] `retencionFuente` se calcula sobre `baseImponible` (subtotal - descuentos), no sobre subtotal
- [ ] Ambos valores están redondeados a 2 decimales
- [ ] No envías el campo `total` (déjalo que el backend lo calcule)

---

## 🔗 Relación con la Orden

**Nota importante:** El `subtotal` que envías en la factura puede ser diferente al `subtotal` de la orden si:
- La orden tiene subtotal SIN IVA (porque se calcula restando el IVA)
- La factura necesita subtotal CON IVA o SIN IVA según tu lógica de negocio

**Recomendación:** Usa el `subtotal` de la orden directamente, ya que el backend lo calcula correctamente.

---

## 📝 Conclusión

**El backend espera el VALOR CALCULADO EN DINERO para `iva`, no el porcentaje.**

**Fórmula:**
```
iva = (subtotal - descuentos) × (porcentajeIva / 100)
```

**Igual que `retencionFuente`, el `iva` debe ser un valor monetario calculado, no un porcentaje.**

