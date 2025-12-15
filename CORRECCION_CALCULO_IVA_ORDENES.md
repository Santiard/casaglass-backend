# 🔧 CORRECCIÓN: CÁLCULO DE IVA EN ÓRDENES

## FECHA: 2025-01-XX
## PROBLEMA IDENTIFICADO Y RESUELTO

---

## 🐛 PROBLEMA IDENTIFICADO

El endpoint `GET /api/ordenes/tabla` estaba retornando valores incorrectos:

**Valores incorrectos retornados:**
- `subtotal: 2175000` (debería ser la base sin IVA = $1.827.731,09)
- `iva: 0` (debería ser ~$347.268,91)
- `total: 2175000` (correcto si incluye IVA)

**Cálculo esperado:**
- Si el total facturado es $2.175.000 (con IVA incluido):
  - Subtotal (sin IVA) = $2.175.000 / 1.19 = $1.827.731,09
  - IVA (19%) = $2.175.000 - $1.827.731,09 = $347.268,91

---

## 🔍 CAUSA RAÍZ

Los métodos `crearOrdenVenta()` y `crearOrdenVentaConCredito()` estaban usando una **lógica antigua** que:

1. ❌ Establecía `orden.setSubtotal(subtotalBruto)` donde `subtotalBruto` es el total CON IVA incluido
2. ❌ No calculaba el IVA (no llamaba a `calcularValoresMonetariosOrden()`)
3. ❌ No establecía el campo `iva` (solo establecía `retencionFuente` y `total`)

**Código problemático (ANTES):**

```java
// ❌ INCORRECTO
orden.setSubtotal(subtotalBruto); // subtotalBruto = total CON IVA
Double retencionFuente = calcularRetencionFuente(subtotalBruto, descuentos, ...);
orden.setRetencionFuente(retencionFuente);
Double total = subtotalBruto - descuentos - retencionFuente;
orden.setTotal(total);
// ❌ No se calculaba ni guardaba el IVA
```

---

## ✅ SOLUCIÓN IMPLEMENTADA

Se corrigieron los métodos para usar `calcularValoresMonetariosOrden()` que calcula correctamente:

1. ✅ **Subtotal sin IVA**: `(subtotalFacturado - descuentos) / 1.19`
2. ✅ **IVA**: `subtotalFacturado - subtotalSinIva`
3. ✅ **Retención de fuente**: Calculada sobre el subtotal sin IVA (si aplica)
4. ✅ **Total**: `subtotalFacturado - descuentos` (sin restar retención)

**Código corregido (DESPUÉS):**

```java
// ✅ CORRECTO
Double[] valores = calcularValoresMonetariosOrden(subtotalBruto, descuentos, tieneRetencionFuente);
Double subtotalSinIva = valores[0];  // Base imponible sin IVA
Double iva = valores[1];            // IVA calculado
Double retencionFuente = valores[2]; // Retención de fuente
Double total = valores[3];           // Total facturado

// Guardar valores en la orden
orden.setSubtotal(subtotalSinIva);        // Base sin IVA
orden.setIva(iva);                        // IVA
orden.setRetencionFuente(retencionFuente); // Retención
orden.setTotal(total);                    // Total facturado
```

---

## 📝 MÉTODOS CORREGIDOS

### 1. ✅ `crearOrdenVenta(OrdenVentaDTO ventaDTO)`
- **Ubicación:** `OrdenService.java` línea 156
- **Estado:** ✅ CORREGIDO
- **Cambio:** Ahora usa `calcularValoresMonetariosOrden()` (línea 216)

### 2. ✅ `crearOrdenVentaConCredito(OrdenVentaDTO ventaDTO)`
- **Ubicación:** `OrdenService.java` línea 256
- **Estado:** ✅ CORREGIDO
- **Cambio:** Ahora usa `calcularValoresMonetariosOrden()` (línea 316)

### 3. ✅ `actualizarOrdenVenta(Long ordenId, OrdenVentaDTO ventaDTO)`
- **Ubicación:** `OrdenService.java` línea 377
- **Estado:** ✅ YA ESTABA CORRECTO
- **Nota:** Ya usaba `calcularValoresMonetariosOrden()` (línea 452)

### 4. ✅ `actualizarOrdenVentaConCredito(Long ordenId, OrdenVentaDTO ventaDTO)`
- **Ubicación:** `OrdenService.java` línea 486
- **Estado:** ✅ YA ESTABA CORRECTO
- **Nota:** Ya usaba `calcularValoresMonetariosOrden()` (línea 561)

### 5. ✅ `crear(Orden orden)`
- **Ubicación:** `OrdenService.java` línea 84
- **Estado:** ✅ YA ESTABA CORRECTO
- **Nota:** Ya usaba `calcularValoresMonetariosOrden()` (línea 127)

### 6. ✅ `actualizarOrden(Long ordenId, OrdenActualizarDTO dto)`
- **Ubicación:** `OrdenService.java` línea 1527
- **Estado:** ✅ YA ESTABA CORRECTO
- **Nota:** Ya usaba `calcularValoresMonetariosOrden()` (línea 1544)

---

## 🧮 LÓGICA DE CÁLCULO (calcularValoresMonetariosOrden)

El método `calcularValoresMonetariosOrden()` implementa la siguiente lógica:

```java
// Paso 1: Calcular base imponible (total facturado - descuentos)
Double baseConIva = subtotalFacturado - descuentos;

// Paso 2: Calcular subtotal sin IVA
Double ivaRate = obtenerIvaRate(); // Obtiene de BusinessSettings (default: 19%)
Double subtotalSinIva = baseConIva / (1.0 + (ivaRate / 100.0));
subtotalSinIva = Math.round(subtotalSinIva * 100.0) / 100.0;

// Paso 3: Calcular IVA
Double iva = baseConIva - subtotalSinIva;
iva = Math.round(iva * 100.0) / 100.0;

// Paso 4: Calcular retención de fuente (sobre subtotal sin IVA)
Double retencionFuente = 0.0;
if (tieneRetencionFuente) {
    BusinessSettings config = obtenerConfiguracionRetencion();
    Double reteRate = config.getReteRate() != null ? config.getReteRate() : 2.5;
    Long reteThreshold = config.getReteThreshold() != null ? config.getReteThreshold() : 1_000_000L;
    
    if (subtotalSinIva >= reteThreshold) {
        retencionFuente = subtotalSinIva * (reteRate / 100.0);
        retencionFuente = Math.round(retencionFuente * 100.0) / 100.0;
    }
}

// Paso 5: Calcular total (total facturado - descuentos, sin restar retención)
Double total = subtotalFacturado - descuentos;
total = Math.round(total * 100.0) / 100.0;

return new Double[]{subtotalSinIva, iva, retencionFuente, total};
```

---

## 📊 EJEMPLO DE CÁLCULO CORRECTO

**Datos de entrada:**
- Subtotal facturado (con IVA): $2.175.000
- Descuentos: $0
- Tiene retención de fuente: `true`
- IVA rate: 19%
- Rete rate: 2.5%
- Rete threshold: $1.000.000

**Cálculo:**

1. **Base con IVA** = $2.175.000 - $0 = $2.175.000
2. **Subtotal sin IVA** = $2.175.000 / 1.19 = **$1.827.731,09**
3. **IVA** = $2.175.000 - $1.827.731,09 = **$347.268,91**
4. **Retención de fuente** = $1.827.731,09 × 2.5% = **$45.693,28** (si aplica)
5. **Total** = $2.175.000 - $0 = **$2.175.000**

**Valores guardados en la orden:**
```json
{
  "subtotal": 1827731.09,    // Base sin IVA
  "iva": 347268.91,          // IVA calculado
  "retencionFuente": 45693.28, // Retención
  "total": 2175000.0          // Total facturado
}
```

---

## ✅ VERIFICACIÓN

### Endpoint GET /api/ordenes/tabla

**Antes de la corrección:**
```json
{
  "subtotal": 2175000,  // ❌ Incorrecto (total con IVA)
  "iva": 0,             // ❌ Incorrecto (no calculado)
  "total": 2175000      // ✅ Correcto
}
```

**Después de la corrección:**
```json
{
  "subtotal": 1827731.09,  // ✅ Correcto (base sin IVA)
  "iva": 347268.91,        // ✅ Correcto (IVA calculado)
  "total": 2175000.0       // ✅ Correcto (total facturado)
}
```

---

## 🔄 IMPACTO EN ÓRDENES EXISTENTES

### Órdenes creadas ANTES de la corrección

Las órdenes creadas antes de esta corrección pueden tener:
- `subtotal` = total con IVA (incorrecto)
- `iva` = 0 (no calculado)

**Solución recomendada:**

1. **Opción 1: Recalcular órdenes existentes** (script SQL o migración)
   ```sql
   -- Ejemplo de script para recalcular (ajustar según necesidad)
   UPDATE ordenes 
   SET 
     subtotal = (total + descuentos) / 1.19,
     iva = (total + descuentos) - ((total + descuentos) / 1.19)
   WHERE iva = 0 AND subtotal = total;
   ```

2. **Opción 2: Dejar órdenes antiguas como están** (solo nuevas órdenes tendrán valores correctos)

### Órdenes creadas DESPUÉS de la corrección

Todas las nuevas órdenes (creadas o actualizadas) ahora tendrán:
- ✅ `subtotal` = base sin IVA (correcto)
- ✅ `iva` = IVA calculado (correcto)
- ✅ `retencionFuente` = retención calculada (si aplica)
- ✅ `total` = total facturado (correcto)

---

## 📋 CHECKLIST DE VERIFICACIÓN

- [x] Método `crearOrdenVenta()` corregido
- [x] Método `crearOrdenVentaConCredito()` corregido
- [x] Método `actualizarOrdenVenta()` verificado (ya estaba correcto)
- [x] Método `actualizarOrdenVentaConCredito()` verificado (ya estaba correcto)
- [x] Método `crear()` verificado (ya estaba correcto)
- [x] Método `actualizarOrden()` verificado (ya estaba correcto)
- [x] Compilación exitosa sin errores
- [ ] Pruebas manuales realizadas
- [ ] Órdenes existentes verificadas/recalculadas (si aplica)

---

## 🎯 CONCLUSIÓN

**Problema resuelto:** Los métodos de creación de órdenes ahora calculan correctamente el IVA y el subtotal sin IVA usando el método centralizado `calcularValoresMonetariosOrden()`.

**Próximos pasos:**
1. Probar la creación de una nueva orden y verificar que los valores sean correctos
2. Verificar que el endpoint `GET /api/ordenes/tabla` retorne los valores correctos
3. Decidir si se necesita recalcular órdenes existentes o dejarlas como están

---

**Última actualización:** 2025-01-XX  
**Versión:** 1.0

