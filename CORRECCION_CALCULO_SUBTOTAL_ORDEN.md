# 🔧 Corrección: Cálculo de Subtotal en Órdenes

## ❌ Problema Identificado

El backend estaba **restando el IVA** del subtotal cuando los precios **ya incluyen el IVA del 19%**.

**Síntoma:**
- Producto de $100.000 (con IVA incluido)
- Orden se crea con total de **$84.033,61** ❌
- Debería ser **$100.000** ✅

---

## ✅ Solución Implementada

Se eliminó la lógica que restaba el IVA del subtotal. Ahora el subtotal y el total mantienen el valor **CON IVA incluido**.

### Cambios Realizados

**Archivo**: `src/main/java/com/casaglass/casaglass_backend/service/OrdenService.java`

**Métodos corregidos:**
- ✅ `crear()` - Crear orden genérica
- ✅ `crearOrdenVenta()` - Crear orden de venta
- ✅ `crearOrdenVentaConCredito()` - Crear orden de venta a crédito
- ✅ `actualizarOrdenVenta()` - Actualizar orden de venta (2 métodos)
- ✅ `actualizarOrden()` - Actualizar orden desde tabla

### Código Anterior (Incorrecto):
```java
// Calcular subtotal SIN IVA (restando el IVA del subtotal bruto)
// Fórmula: subtotal = subtotalBruto / (1 + IVA%)
Double ivaRate = obtenerIvaRate();
Double subtotal = subtotalBruto / (1 + (ivaRate / 100.0));
subtotal = Math.round(subtotal * 100.0) / 100.0;
orden.setSubtotal(subtotal);
```

### Código Nuevo (Correcto):
```java
// El subtotal debe mantener el valor CON IVA incluido (los precios ya lo incluyen)
// NO se resta el IVA porque los precios ya lo incluyen
orden.setSubtotal(subtotalBruto);
```

---

## 📊 Fórmulas de Cálculo

### Antes (Incorrecto):
```
subtotalBruto = Σ(cantidad × precioUnitario)  // Con IVA incluido
subtotal = subtotalBruto / 1.19               // ❌ Restando IVA (incorrecto)
total = subtotal - descuentos                 // ❌ Total incorrecto
```

### Ahora (Correcto):
```
subtotalBruto = Σ(cantidad × precioUnitario)  // Con IVA incluido
subtotal = subtotalBruto                       // ✅ Mantiene IVA incluido
total = subtotal - descuentos                  // ✅ Total correcto
```

---

## 🔢 Ejemplo Práctico

### Escenario:
- **Producto**: $100.000 (con IVA del 19% incluido)
- **Cantidad**: 1
- **Descuentos**: 0

### Cálculo Anterior (Incorrecto):
```
subtotalBruto = 1 × 100.000 = 100.000
subtotal = 100.000 / 1.19 = 84.033,61  // ❌ Restando IVA
total = 84.033,61 - 0 = 84.033,61      // ❌ Incorrecto
```

### Cálculo Nuevo (Correcto):
```
subtotalBruto = 1 × 100.000 = 100.000
subtotal = 100.000                        // ✅ Mantiene IVA incluido
total = 100.000 - 0 = 100.000            // ✅ Correcto
```

---

## ✅ Verificación

### Test 1: Producto de $100.000
- **Precio unitario**: 100.000 (con IVA)
- **Cantidad**: 1
- **Subtotal esperado**: 100.000 ✅
- **Total esperado**: 100.000 ✅

### Test 2: Múltiples productos
- **Producto 1**: 100.000 × 2 = 200.000
- **Producto 2**: 50.000 × 1 = 50.000
- **Subtotal esperado**: 250.000 ✅
- **Total esperado**: 250.000 ✅

### Test 3: Con descuentos
- **Subtotal**: 100.000
- **Descuentos**: 10.000
- **Total esperado**: 90.000 ✅

---

## 📝 Notas Importantes

1. **Los precios siempre incluyen IVA del 19%**
2. **El subtotal de la orden mantiene el IVA incluido**
3. **El total de la orden = subtotal - descuentos** (sin restar IVA)
4. **El IVA se calcula y separa solo al crear la factura**

---

## 🔄 Flujo Completo

### 1. Crear Orden
```
Precio unitario: 100.000 (con IVA)
Subtotal: 100.000 (con IVA)
Total: 100.000 (con IVA)
```

### 2. Crear Factura
```
Subtotal de orden: 100.000 (con IVA)
IVA a calcular: 100.000 × 0.19 = 19.000
Subtotal sin IVA: 100.000 - 19.000 = 81.000 (o 100.000 / 1.19)
```

**Pero espera...** Si el usuario dice que el IVA se calcula como `subtotal * 0.19`, entonces:
- Si subtotal = 100.000 (con IVA)
- IVA = 100.000 × 0.19 = 19.000

Esto significa que el IVA es el 19% del valor total (con IVA incluido), lo cual es inusual pero es lo que el usuario quiere.

---

## 🎯 Conclusión

**El subtotal y el total de la orden ahora mantienen el valor CON IVA incluido.**

**No se resta el IVA** porque los precios ya lo incluyen desde el inicio.

**El total de la orden = subtotal - descuentos** (sin modificar por IVA).

