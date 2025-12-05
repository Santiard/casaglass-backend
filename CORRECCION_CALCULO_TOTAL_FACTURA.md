# 🔧 Corrección: Cálculo del Total en Facturas

## ❌ Problema Identificado

El backend estaba **sumando el IVA al total** cuando el subtotal **ya incluye el IVA**, duplicando el IVA en el cálculo.

**Fórmula Incorrecta (Anterior):**
```java
total = (subtotal - descuentos) + iva - retencionFuente
```

**Problema:**
- El subtotal ya incluye IVA (600000)
- El frontend envía el IVA calculado (114000)
- El backend sumaba el IVA al subtotal, duplicando el IVA

---

## ✅ Solución Implementada

Se corrigió la fórmula para que **NO sume el IVA** al total, ya que el subtotal ya lo incluye.

**Fórmula Correcta (Nueva):**
```java
total = (subtotal - descuentos) - retencionFuente
```

---

## 🔧 Cambios Realizados

### Archivo: `src/main/java/com/casaglass/casaglass_backend/model/Factura.java`

**Método `calcularTotal()` corregido:**

**Antes (Incorrecto):**
```java
public void calcularTotal() {
    double baseImponible = subtotal - descuentos;
    double totalCalculado = baseImponible + iva - retencionFuente;  // ❌ Suma IVA
    this.total = Math.round(totalCalculado * 100.0) / 100.0;
}
```

**Ahora (Correcto):**
```java
public void calcularTotal() {
    double baseImponible = subtotal - descuentos;
    // El subtotal ya incluye IVA, solo se resta la retención de fuente
    double totalCalculado = baseImponible - retencionFuente;  // ✅ NO suma IVA
    this.total = Math.round(totalCalculado * 100.0) / 100.0;
}
```

---

## 📊 Ejemplo Práctico

### Escenario del Usuario:
- **Subtotal**: 600,000 (con IVA incluido)
- **Descuentos**: 0
- **IVA**: 114,000 (calculado en frontend: 600000 × 0.19)
- **Retención**: 6,075 (calculado en frontend: 486000 × 0.0125)

### Cálculo Anterior (Incorrecto):
```java
baseImponible = 600000 - 0 = 600000
total = 600000 + 114000 - 6075 = 707925  // ❌ Incorrecto (duplica IVA)
```

### Cálculo Nuevo (Correcto):
```java
baseImponible = 600000 - 0 = 600000
total = 600000 - 6075 = 593925  // ✅ Correcto
```

---

## 🔢 Ejemplos Completos

### Ejemplo 1: Sin Descuentos

**Datos:**
- Subtotal: 600,000 (con IVA)
- Descuentos: 0
- IVA: 114,000 (solo para registro)
- Retención: 6,075

**Cálculo:**
```java
total = 600000 - 0 - 6075 = 593925
```

### Ejemplo 2: Con Descuentos

**Datos:**
- Subtotal: 1,000,000 (con IVA)
- Descuentos: 100,000
- IVA: 190,000 (solo para registro)
- Retención: 12,500

**Cálculo:**
```java
baseImponible = 1000000 - 100000 = 900000
total = 900000 - 12500 = 887500
```

---

## 📋 Notas Importantes

1. **El subtotal ya incluye IVA del 19%**
2. **El campo `iva` se usa solo para registro/contabilidad**, no se suma al total
3. **El total se calcula**: `subtotal - descuentos - retencionFuente`
4. **El IVA que envía el frontend** es solo para mostrar en la factura, no afecta el total

---

## 🎯 Resumen

| Concepto | Valor | Uso |
|----------|-------|-----|
| **Subtotal** | 600,000 | Ya incluye IVA |
| **IVA** | 114,000 | Solo para registro (no se suma) |
| **Retención** | 6,075 | Se resta del total |
| **Total** | 593,925 | `subtotal - descuentos - retencionFuente` |

---

## ✅ Verificación

### Test 1: Subtotal 600,000
- **Total esperado**: 600,000 - 6,075 = 593,925 ✅

### Test 2: Con descuentos
- **Subtotal**: 1,000,000
- **Descuentos**: 100,000
- **Retención**: 12,500
- **Total esperado**: 900,000 - 12,500 = 887,500 ✅

---

## 🔄 Flujo Completo

1. **Frontend calcula:**
   - IVA: 600,000 × 0.19 = 114,000 (solo para mostrar)
   - Retención: 600,000 × 0.0125 = 7,500 (pero el usuario calculó 6,075 sobre 486,000)

2. **Frontend envía:**
   ```json
   {
     "subtotal": 600000,
     "descuentos": 0,
     "iva": 114000,
     "retencionFuente": 6075
   }
   ```

3. **Backend calcula total:**
   ```java
   total = 600000 - 0 - 6075 = 593925
   ```

---

## ⚠️ Nota sobre la Retención

El usuario calculó la retención como `486000 × 0.0125 = 6075`, pero según la documentación debería ser sobre el subtotal completo (600,000).

**Si se calcula sobre 600,000:**
- Retención: 600,000 × 0.0125 = 7,500

**Si se calcula sobre 486,000 (subtotal sin IVA):**
- Retención: 486,000 × 0.0125 = 6,075

El backend acepta el valor que envía el frontend, así que funciona con ambos casos.

---

## ✅ Conclusión

**El total ahora se calcula correctamente:**
```
total = (subtotal - descuentos) - retencionFuente
```

**El IVA NO se suma al total** porque ya está incluido en el subtotal.

