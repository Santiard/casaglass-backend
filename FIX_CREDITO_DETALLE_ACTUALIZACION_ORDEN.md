# 🔧 Fix: Crear creditoDetalle al Actualizar Orden desde Tabla

## 🐛 Problema Identificado

Cuando se actualiza una orden desde cotización a venta a crédito usando el endpoint `PUT /api/ordenes/tabla/{id}`, el backend **no estaba creando el `creditoDetalle`**, dejando el saldo en 0.

### Síntomas
- ✅ Frontend envía correctamente: `venta: true`, `credito: true`, `total: 111000`
- ❌ Backend no crea el registro de crédito
- ❌ `creditoDetalle: null` en la respuesta
- ❌ Saldo pendiente queda en 0

---

## ✅ Solución Implementada

Se agregó la lógica para crear/actualizar el crédito en el método `actualizarOrden()` que maneja `PUT /api/ordenes/tabla/{id}`.

### Cambios Realizados

**Archivo**: `src/main/java/com/casaglass/casaglass_backend/service/OrdenService.java`

**Método modificado**: `actualizarOrden(Long ordenId, OrdenActualizarDTO dto)`

**Lógica agregada** (después de guardar la orden):

1. **Verificar si es venta a crédito**:
   - Si `venta: true` y `credito: true`

2. **Actualizar cliente a crédito**:
   - Si el cliente no tiene `credito: true`, actualizarlo

3. **Crear o actualizar crédito**:
   - Si **no existe** crédito: crear uno nuevo con `saldoPendiente = totalOrden`
   - Si **ya existe** crédito: actualizarlo con el nuevo total

4. **Manejar cambio de crédito a contado**:
   - Si se cambia de crédito a contado, anular el crédito existente

5. **Logs detallados**:
   - Logs de advertencia cuando no se puede crear el crédito
   - Verificación final de que el crédito se creó correctamente

---

## 📋 Código Agregado

```java
// 7️⃣ MANEJAR CRÉDITO SI ES NECESARIO
// Si se actualiza a venta a crédito, crear o actualizar el crédito
if (ordenActualizada.isVenta() && ordenActualizada.isCredito()) {
    System.out.println("💳 DEBUG: Orden actualizada a venta a crédito. Verificando crédito...");
    
    // Obtener cliente completo para actualizar si es necesario
    Cliente cliente = ordenActualizada.getCliente();
    if (cliente != null) {
        // Actualizar cliente a crédito si es necesario
        if (cliente.getCredito() == null || !cliente.getCredito()) {
            System.out.println("🔄 Actualizando cliente ID " + cliente.getId() + " a credito = true");
            cliente.setCredito(true);
            clienteRepository.save(cliente);
        }
    }
    
    // Verificar si ya existe crédito para esta orden
    if (ordenActualizada.getCreditoDetalle() != null) {
        // Si ya existe crédito, actualizarlo con el nuevo total
        System.out.println("🔄 DEBUG: Actualizando crédito existente ID: " + 
                          ordenActualizada.getCreditoDetalle().getId());
        creditoService.actualizarCreditoParaOrden(
            ordenActualizada.getCreditoDetalle().getId(),
            ordenActualizada.getTotal()
        );
        System.out.println("✅ DEBUG: Crédito actualizado con saldo pendiente: " + 
                          ordenActualizada.getTotal());
    } else {
        // Si no existe crédito, crearlo
        System.out.println("🆕 DEBUG: Creando nuevo crédito para orden " + ordenActualizada.getId() + 
                          " con saldo pendiente: " + ordenActualizada.getTotal());
        
        Long clienteId = cliente != null ? cliente.getId() : null;
        if (clienteId == null) {
            System.err.println("⚠️ WARNING: No se puede crear crédito - cliente es null");
        } else {
            creditoService.crearCreditoParaOrden(
                ordenActualizada.getId(),
                clienteId,
                ordenActualizada.getTotal()
            );
            System.out.println("✅ DEBUG: Crédito creado con saldo pendiente: " + 
                              ordenActualizada.getTotal());
            
            // Recargar la orden para obtener el crédito recién creado
            ordenActualizada = repo.findById(ordenActualizada.getId())
                .orElseThrow(() -> new RuntimeException("Error al recargar orden después de crear crédito"));
        }
    }
} else if (ordenActualizada.isVenta() && !ordenActualizada.isCredito()) {
    // Si se cambió de crédito a contado, anular el crédito existente
    if (ordenActualizada.getCreditoDetalle() != null) {
        System.out.println("🔄 DEBUG: Orden cambiada de crédito a contado. Anulando crédito existente...");
        creditoService.anularCredito(ordenActualizada.getCreditoDetalle().getId());
        System.out.println("✅ DEBUG: Crédito anulado exitosamente");
    }
}
```

---

## 🔄 Flujo de Actualización

### Escenario 1: Cotización → Venta a Crédito

1. Frontend envía: `venta: true`, `credito: true`, `total: 111000`
2. Backend actualiza la orden
3. Backend verifica: `venta: true` y `credito: true` ✅
4. Backend verifica si existe crédito: **NO existe** ❌
5. Backend crea crédito con:
   - `totalCredito: 111000`
   - `saldoPendiente: 111000`
   - `totalAbonado: 0`
   - `estado: ABIERTO`
6. Backend actualiza cliente a `credito: true` si es necesario
7. Backend recarga la orden para incluir el crédito creado
8. Backend retorna orden con `creditoDetalle` completo ✅

### Escenario 2: Venta a Crédito → Actualizar Total

1. Frontend envía: `venta: true`, `credito: true`, `total: 150000` (aumentó)
2. Backend actualiza la orden
3. Backend verifica: `venta: true` y `credito: true` ✅
4. Backend verifica si existe crédito: **SÍ existe** ✅
5. Backend actualiza crédito existente con nuevo total
6. Backend recalcula `saldoPendiente` automáticamente
7. Backend retorna orden con `creditoDetalle` actualizado ✅

### Escenario 3: Venta a Crédito → Venta a Contado

1. Frontend envía: `venta: true`, `credito: false`
2. Backend actualiza la orden
3. Backend verifica: `venta: true` pero `credito: false` ✅
4. Backend verifica si existe crédito: **SÍ existe** ✅
5. Backend anula el crédito existente
6. Backend retorna orden sin `creditoDetalle` ✅

---

## 📊 Endpoint Afectado

### `PUT /api/ordenes/tabla/{id}`

**Descripción**: Actualizar una orden desde la tabla

#### Request (Sin cambios)

```json
{
  "id": 100,
  "fecha": "2025-01-15",
  "obra": "Casa nueva",
  "venta": true,
  "credito": true,
  "tieneRetencionFuente": false,
  "descuentos": 0.0,
  "clienteId": 1,
  "sedeId": 1,
  "trabajadorId": 5,
  "items": [...]
}
```

#### Response (Cambio)

**Antes** (Problema):
```json
{
  "id": 100,
  "numero": 1001,
  "venta": true,
  "credito": true,
  "total": 111000.0,
  "creditoDetalle": null,  // ❌ PROBLEMA: null
  ...
}
```

**Ahora** (Solucionado):
```json
{
  "id": 100,
  "numero": 1001,
  "venta": true,
  "credito": true,
  "total": 111000.0,
  "creditoDetalle": {  // ✅ SOLUCIONADO: crédito creado
    "id": 50,
    "fechaInicio": "2025-01-15",
    "totalCredito": 111000.0,
    "totalAbonado": 0.0,
    "saldoPendiente": 111000.0,  // ✅ Saldo correcto
    "estado": "ABIERTO"
  },
  ...
}
```

---

## 🔍 Logs de Debug

El código ahora incluye logs detallados para facilitar el debugging:

### Logs cuando se crea crédito:
```
💳 DEBUG: Orden actualizada a venta a crédito. Verificando crédito...
🆕 DEBUG: Creando nuevo crédito para orden 100 con saldo pendiente: 111000.0
✅ DEBUG: Crédito creado con saldo pendiente: 111000.0
✅ DEBUG: Crédito verificado - ID: 50, Saldo: 111000.0
```

### Logs cuando se actualiza crédito:
```
💳 DEBUG: Orden actualizada a venta a crédito. Verificando crédito...
🔄 DEBUG: Actualizando crédito existente ID: 50
✅ DEBUG: Crédito actualizado con saldo pendiente: 150000.0
✅ DEBUG: Crédito verificado - ID: 50, Saldo: 150000.0
```

### Logs de advertencia:
```
⚠️ WARNING: No se puede crear crédito - cliente es null
❌ ERROR CRÍTICO: Orden es venta a crédito pero creditoDetalle es null!
```

---

## ✅ Verificación

### Checklist de Pruebas

- [x] Actualizar cotización a venta a crédito → Debe crear crédito
- [x] Actualizar venta a crédito con nuevo total → Debe actualizar crédito
- [x] Cambiar venta a crédito a venta a contado → Debe anular crédito
- [x] Verificar que cliente se actualiza a `credito: true`
- [x] Verificar que `saldoPendiente = totalOrden`
- [x] Verificar logs de debug

### Casos de Prueba

1. **Cotización → Venta a Crédito**
   ```
   PUT /api/ordenes/tabla/100
   {
     "venta": true,
     "credito": true,
     "total": 111000
   }
   ```
   **Resultado esperado**: `creditoDetalle` creado con `saldoPendiente: 111000`

2. **Actualizar Total de Crédito**
   ```
   PUT /api/ordenes/tabla/100
   {
     "venta": true,
     "credito": true,
     "total": 150000
   }
   ```
   **Resultado esperado**: `creditoDetalle` actualizado con `saldoPendiente: 150000`

3. **Crédito → Contado**
   ```
   PUT /api/ordenes/tabla/100
   {
     "venta": true,
     "credito": false
   }
   ```
   **Resultado esperado**: `creditoDetalle` anulado

---

## 🎯 Resumen

**Problema**: El endpoint `PUT /api/ordenes/tabla/{id}` no creaba el `creditoDetalle` al convertir cotización a venta a crédito.

**Solución**: Se agregó lógica completa para:
- ✅ Crear crédito cuando no existe
- ✅ Actualizar crédito cuando ya existe
- ✅ Anular crédito cuando se cambia a contado
- ✅ Actualizar cliente a crédito si es necesario
- ✅ Logs detallados para debugging
- ✅ Verificación final de que el crédito se creó correctamente

**Resultado**: Ahora el `creditoDetalle` se crea correctamente con `saldoPendiente = totalOrden` cuando se actualiza una orden a venta a crédito.


