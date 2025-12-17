# DOCUMENTACIÓN: BUG EN CÁLCULO DE SALDO PENDIENTE CUANDO SE MARCA RETENCIÓN DESPUÉS DE CREAR ORDEN

## 📋 DESCRIPCIÓN DEL PROBLEMA

Cuando se marca `tieneRetencionFuente = true` DESPUÉS de crear una orden (mediante el endpoint PUT `/api/ordenes/{id}/retencion-fuente`), el `saldoPendiente` del crédito asociado NO se actualiza correctamente.

### Ejemplo del Bug:
- **Orden 1102**: Marcada con retención DESPUÉS de creación
  - Total facturado: `2,610,000`
  - Retención de fuente: `54,831.93`
  - Saldo pendiente ACTUAL: `2,610,000` ❌ (INCORRECTO)
  - Saldo pendiente ESPERADO: `2,555,168.07` ✅ (2,610,000 - 54,831.93)

- **Orden 1104**: Creada CON retención desde el inicio
  - Total facturado: `1,740,000`
  - Retención de fuente: `36,554.62`
  - Saldo pendiente ACTUAL: `1,703,445.38` ✅ (CORRECTO)

---

## 🔍 ANÁLISIS DEL FLUJO ACTUAL

### 1️⃣ ENDPOINT: `PUT /api/ordenes/{id}/retencion-fuente`

**Ubicación**: `OrdenService.java` - Método `actualizarRetencionFuente()` (línea ~2140-2210)

**Flujo actual**:
```java
@Transactional
public OrdenDTO actualizarRetencionFuente(Long id, RetencionFuenteDTO dto) {
    // 1. Buscar la orden
    Orden orden = ordenRepo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

    // 2. Actualizar campos de retención
    orden.setTieneRetencionFuente(dto.getTieneRetencionFuente());
    orden.setRetencionFuente(dto.getRetencionFuente());
    orden.setIva(dto.getIva());

    // 3. Guardar la orden actualizada
    Orden ordenActualizada = ordenRepo.save(orden);

    // 4. Si tiene crédito asociado, recalcular totales ⚠️ AQUÍ ESTÁ EL PROBLEMA
    if (ordenActualizada.getCreditoDetalle() != null && 
        ordenActualizada.getCreditoDetalle().getId() != null) {
        creditoService.recalcularTotales(ordenActualizada.getCreditoDetalle().getId());
    }

    return convertToDTO(ordenActualizada);
}
```

**✅ Lo que SÍ hace bien**: El endpoint SÍ llama a `creditoService.recalcularTotales()` cuando existe un crédito asociado.

**❌ El problema**: El método `recalcularTotales()` NO toma en cuenta la retención de fuente al calcular el saldo pendiente.

---

### 2️⃣ MÉTODO: `creditoService.recalcularTotales()`

**Ubicación**: `CreditoService.java` - Línea ~320

**Código actual**:
```java
@Transactional
public Credito recalcularTotales(Long creditoId) {
    Credito credito = creditoRepo.findById(creditoId)
        .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado"));

    // 1. Recalcular total abonado sumando todos los abonos
    Double totalAbonos = credito.getAbonos().stream()
        .mapToDouble(abono -> abono.getTotal() != null ? abono.getTotal() : 0.0)
        .sum();

    // 2. Actualizar total abonado
    credito.setTotalAbonado(normalize(totalAbonos));
    
    // 3. Actualizar saldo pendiente ⚠️ AQUÍ ESTÁ EL PROBLEMA
    credito.actualizarSaldo();

    return creditoRepo.save(credito);
}
```

**❌ El problema**: Este método solo recalcula `totalAbonado` y luego llama a `actualizarSaldo()`, pero NO considera la retención de fuente de la orden asociada.

---

### 3️⃣ MÉTODO: `credito.actualizarSaldo()`

**Ubicación**: `Credito.java` - Línea ~96

**Código actual**:
```java
public void actualizarSaldo() {
    // ⚠️ FÓRMULA INCOMPLETA
    this.saldoPendiente = this.totalCredito - this.totalAbonado;
    
    // Actualizar estado automáticamente
    if (this.saldoPendiente <= 0) {
        this.estado = EstadoCredito.CERRADO;
        this.fechaCierre = LocalDate.now();
    } else if (this.saldoPendiente < this.totalCredito) {
        this.estado = EstadoCredito.PARCIALMENTE_PAGADO;
    } else {
        this.estado = EstadoCredito.ABIERTO;
    }
}
```

**❌ EL PROBLEMA RAÍZ**:
La fórmula de `saldoPendiente` es **INCOMPLETA**. 

**Fórmula actual**:
```
saldoPendiente = totalCredito - totalAbonado
```

**Fórmula correcta debería ser**:
```
saldoPendiente = totalCredito - totalAbonado - retencionFuente
```

**¿Por qué?**
La retención de fuente es un **descuento del total facturado** que el cliente NO debe pagar porque ya se lo retuvieron. Debe restarse del saldo pendiente al igual que los abonos.

---

## 🎯 COMPARACIÓN: ¿POR QUÉ FUNCIONA AL CREAR LA ORDEN CON RETENCIÓN?

### Caso 1: Orden creada CON retención desde el inicio (✅ Funciona)

**Ubicación**: `CreditoService.java` - Método `crearCreditoParaOrden()` (línea ~180)

```java
@Transactional(propagation = Propagation.REQUIRED)
public Credito crearCreditoParaOrden(Orden orden) {
    Double totalOrden = orden.getTotal(); // Total facturado CON IVA
    Double retencionFuente = (orden.getTieneRetencionFuente() && orden.getRetencionFuente() != null) 
                              ? orden.getRetencionFuente() 
                              : 0.0;
    
    // ✅ AQUÍ SE RESTA LA RETENCIÓN AL CREAR EL CRÉDITO
    Double saldoPendienteInicial = normalize(totalOrden - retencionFuente);
    
    Credito credito = new Credito();
    credito.setTotalCredito(normalize(totalOrden));
    credito.setTotalAbonado(0.0);
    credito.setSaldoPendiente(saldoPendienteInicial); // ✅ Correcto desde el inicio
    
    return creditoRepo.save(credito);
}
```

**✅ Funciona porque**: Al momento de CREAR el crédito, se calcula el `saldoPendienteInicial` restando la retención.

---

### Caso 2: Orden actualizada con retención DESPUÉS (❌ NO funciona)

**Flujo**:
1. Se crea orden SIN retención → `saldoPendiente = totalCredito - totalAbonado` (correcto)
2. Se marca `tieneRetencionFuente = true` → Se llama a `recalcularTotales()`
3. `recalcularTotales()` llama a `actualizarSaldo()`
4. `actualizarSaldo()` usa fórmula: `saldoPendiente = totalCredito - totalAbonado` ❌
5. **NO se resta la retención de fuente** → saldo queda INCORRECTO

---

## 📊 RESUMEN DEL PROBLEMA

| Aspecto | Estado Actual | Estado Esperado |
|---------|---------------|-----------------|
| **Fórmula en `actualizarSaldo()`** | `totalCredito - totalAbonado` | `totalCredito - totalAbonado - retencionFuente` |
| **Acceso a retención** | ❌ No tiene acceso a `orden.retencionFuente` | ✅ Debe acceder mediante `this.orden.getRetencionFuente()` |
| **Método `recalcularTotales()`** | Solo recalcula abonos | Debería también considerar retención |
| **Orden 1102** | Saldo: 2,610,000 ❌ | Saldo: 2,555,168.07 ✅ |
| **Orden 1104** | Saldo: 1,703,445.38 ✅ | Saldo: 1,703,445.38 ✅ (ya correcto) |

---

## 🛠️ SOLUCIÓN PROPUESTA

### Opción 1: Modificar `actualizarSaldo()` en `Credito.java` (RECOMENDADO)

```java
public void actualizarSaldo() {
    // Obtener retención de fuente de la orden asociada
    Double retencionFuente = 0.0;
    if (this.orden != null && 
        this.orden.getTieneRetencionFuente() && 
        this.orden.getRetencionFuente() != null) {
        retencionFuente = this.orden.getRetencionFuente();
    }
    
    // ✅ FÓRMULA CORRECTA: Restar tanto los abonos como la retención
    this.saldoPendiente = this.totalCredito - this.totalAbonado - retencionFuente;
    
    // Actualizar estado automáticamente
    if (this.saldoPendiente <= 0) {
        this.estado = EstadoCredito.CERRADO;
        this.fechaCierre = LocalDate.now();
    } else if (this.saldoPendiente < this.totalCredito) {
        this.estado = EstadoCredito.PARCIALMENTE_PAGADO;
    } else {
        this.estado = EstadoCredito.ABIERTO;
    }
}
```

**Ventajas**:
- ✅ Se corrige el problema en la raíz
- ✅ Funciona para TODOS los casos (crear orden, actualizar retención, agregar abonos)
- ✅ No requiere cambios en otros métodos
- ✅ Automático: cada vez que se llame `actualizarSaldo()` considerará la retención

---

### Opción 2: Modificar `recalcularTotales()` en `CreditoService.java`

```java
@Transactional
public Credito recalcularTotales(Long creditoId) {
    Credito credito = creditoRepo.findById(creditoId)
        .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado"));

    // Recalcular total abonado
    Double totalAbonos = credito.getAbonos().stream()
        .mapToDouble(abono -> abono.getTotal() != null ? abono.getTotal() : 0.0)
        .sum();
    credito.setTotalAbonado(normalize(totalAbonos));
    
    // ✅ Obtener retención de fuente de la orden
    Double retencionFuente = 0.0;
    if (credito.getOrden() != null && 
        credito.getOrden().getTieneRetencionFuente() && 
        credito.getOrden().getRetencionFuente() != null) {
        retencionFuente = credito.getOrden().getRetencionFuente();
    }
    
    // ✅ Calcular saldo pendiente con retención
    credito.setSaldoPendiente(normalize(
        credito.getTotalCredito() - credito.getTotalAbonado() - retencionFuente
    ));
    
    // Actualizar estado
    credito.actualizarSaldo(); // Solo para actualizar el estado

    return creditoRepo.save(credito);
}
```

**Desventajas**:
- ⚠️ Solo funciona cuando se llama `recalcularTotales()`
- ⚠️ Si se agrega un abono y solo se llama `actualizarSaldo()`, el bug persiste
- ⚠️ Duplica lógica (cálculo de saldo en 2 lugares diferentes)

---

## ✅ RECOMENDACIÓN FINAL

**Modificar `actualizarSaldo()` en `Credito.java` (Opción 1)** porque:
1. Corrige el problema de raíz
2. Funciona en TODOS los escenarios (crear, actualizar, abonar)
3. Mantiene la lógica centralizada
4. No requiere cambios adicionales en otros métodos

---

## 📝 PRUEBAS NECESARIAS DESPUÉS DEL FIX

1. **Orden 1102**: Verificar que `saldoPendiente` se actualice a `2,555,168.07`
2. **Orden 1104**: Verificar que siga mostrando `1,703,445.38` (no debe cambiar)
3. **Crear nueva orden CON retención**: Verificar que saldo sea correcto desde el inicio
4. **Crear orden SIN retención → Agregarla después**: Verificar que saldo se actualice
5. **Agregar abono a orden con retención**: Verificar que saldo disminuya correctamente

---

**Fecha de documentación**: 2025
**Autor**: Análisis del bug en cálculo de saldo pendiente con retención de fuente
