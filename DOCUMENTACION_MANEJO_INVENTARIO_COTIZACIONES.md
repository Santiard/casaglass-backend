# 📦 DOCUMENTACIÓN: Manejo de Inventario en Cotizaciones vs Ventas

## 🎯 Objetivo
Implementar lógica para que el inventario **solo se descuente cuando una orden es una venta confirmada**, no al crear cotizaciones.

---

## 📊 Modelo de Datos

### Campo `venta` en la entidad `Orden`
```java
@Column(nullable = false)
private boolean venta = false;
```

- **`venta = false`** → COTIZACIÓN (no descuenta inventario)
- **`venta = true`** → VENTA CONFIRMADA (sí descuenta inventario)

---

## 🔄 Flujo de Negocio

### 1️⃣ Crear Cotización
```
POST /api/ordenes
{
  "venta": false,
  "items": [...]
}
```
**Comportamiento:**
- ✅ Se crea la orden en base de datos
- ❌ **NO se descuenta inventario**
- 📋 Estado: COTIZACIÓN

### 2️⃣ Confirmar Cotización → Venta
```
PUT /api/ordenes/tabla/{id}
{
  "venta": true,
  "items": [...]
}
```
**Comportamiento:**
- ✅ Se actualiza `venta = true`
- ✅ **Se descuenta inventario automáticamente**
- 💰 Estado: VENTA CONFIRMADA

### 3️⃣ Revertir Venta → Cotización
```
PUT /api/ordenes/tabla/{id}
{
  "venta": false,
  "items": [...]
}
```
**Comportamiento:**
- ✅ Se actualiza `venta = false`
- ✅ **Se restaura inventario automáticamente**
- 📋 Estado: COTIZACIÓN

### 4️⃣ Crear Venta Directa
```
POST /api/ordenes/venta
{
  "venta": true,
  "items": [...]
}
```
**Comportamiento:**
- ✅ Se crea orden con `venta = true`
- ✅ **Se descuenta inventario automáticamente**
- 💰 Estado: VENTA CONFIRMADA

---

## 🛠️ Implementación Técnica

### Cambio 1: Método `crear()` - POST /api/ordenes

**Ubicación:** `OrdenService.java` línea ~147

**Código Anterior:**
```java
Orden ordenGuardada = repo.save(orden);

// ❌ PROBLEMA: Siempre descuenta inventario
actualizarInventarioPorVenta(ordenGuardada);

return ordenGuardada;
```

**Código Nuevo:**
```java
Orden ordenGuardada = repo.save(orden);

// ⚠️ SOLO descontar inventario si es una VENTA confirmada
// Las cotizaciones (venta=false) NO afectan el stock
if (ordenGuardada.isVenta()) {
    System.out.println("✅ VENTA CONFIRMADA - Descontando inventario...");
    actualizarInventarioPorVenta(ordenGuardada);
} else {
    System.out.println("📋 COTIZACIÓN - Inventario NO afectado");
}

return ordenGuardada;
```

---

### Cambio 2: Método `actualizarOrden()` - PUT /api/ordenes/tabla/{id}

**Ubicación:** `OrdenService.java` línea ~1572

**Se agregó detección de cambio de estado:**

```java
public OrdenTablaDTO actualizarOrden(Long ordenId, OrdenActualizarDTO dto) {
    Orden orden = repo.findById(ordenId)
            .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

    // 🔄 GUARDAR ESTADO ANTERIOR DE VENTA
    boolean eraVentaAntes = orden.isVenta();

    // ... actualizar campos ...

    Orden ordenActualizada = repo.save(orden);
    
    // 📦 MANEJO DE INVENTARIO: Detectar conversión cotización ↔ venta
    if (!eraVentaAntes && ordenActualizada.isVenta()) {
        System.out.println("✅ COTIZACIÓN CONFIRMADA → VENTA - Descontando inventario...");
        actualizarInventarioPorVenta(ordenActualizada);
    } else if (eraVentaAntes && !ordenActualizada.isVenta()) {
        System.out.println("⚠️ VENTA REVERTIDA → COTIZACIÓN - Restaurando inventario...");
        restaurarInventarioPorAnulacion(ordenActualizada);
    } else if (!ordenActualizada.isVenta()) {
        System.out.println("📋 Actualización de COTIZACIÓN - Inventario NO afectado");
    }
    
    // ... resto de la lógica ...
}
```

---

## ✅ Casos de Uso Cubiertos

| Escenario | Endpoint | `venta` inicial | `venta` final | Acción Inventario |
|-----------|----------|-----------------|---------------|-------------------|
| Crear cotización | `POST /api/ordenes` | `false` | `false` | ❌ No descuenta |
| Crear venta directa | `POST /api/ordenes/venta` | `true` | `true` | ✅ Descuenta |
| Confirmar cotización | `PUT /api/ordenes/tabla/{id}` | `false` | `true` | ✅ Descuenta |
| Revertir a cotización | `PUT /api/ordenes/tabla/{id}` | `true` | `false` | ✅ Restaura |
| Actualizar cotización | `PUT /api/ordenes/tabla/{id}` | `false` | `false` | ❌ No afecta |
| Actualizar venta | `PUT /api/ordenes/tabla/{id}` | `true` | `true` | ❌ No afecta (ya descontado) |

---

## 📝 Métodos Auxiliares Utilizados

### `actualizarInventarioPorVenta(Orden orden)`
**Función:** Descuenta del inventario las cantidades de productos vendidos
- Itera sobre todos los items de la orden
- Para cada producto, reduce su cantidad en la sede correspondiente
- Maneja productos normales y cortes por separado
- Permite inventarios negativos (ventas anticipadas)

### `restaurarInventarioPorAnulacion(Orden orden)`
**Función:** Restaura el inventario sumando las cantidades de una orden anulada/revertida
- Itera sobre todos los items de la orden
- Para cada producto, incrementa su cantidad en la sede correspondiente
- Se usa cuando se anula una venta o se revierte de venta a cotización

---

## 🚀 Beneficios de la Implementación

1. ✅ **Cotizaciones sin impacto:** Las cotizaciones no bloquean stock innecesariamente
2. ✅ **Confirmación explícita:** Solo al confirmar venta se descuenta inventario
3. ✅ **Reversibilidad:** Se puede revertir una venta a cotización restaurando stock
4. ✅ **Trazabilidad:** Logs claros de cuándo se afecta el inventario
5. ✅ **Compatibilidad:** Funciona con productos normales y cortes

---

## ⚠️ Consideraciones Importantes

- **Anulación de órdenes:** Usar endpoint `PUT /api/ordenes/{id}/anular` para anular (restaura inventario)
- **No modificar items en cotizaciones confirmadas:** Si una cotización ya fue confirmada (`venta=true`), modificar los items puede causar descuadres de inventario
- **Validación frontend:** El frontend debe validar que solo se pueda confirmar una cotización una vez
- **Cortes:** Los cortes también respetan la lógica de cotización vs venta

---

## 🧪 Testing Recomendado

### Test 1: Crear Cotización
```http
POST /api/ordenes
{
  "venta": false,
  "clienteId": 1,
  "sedeId": 1,
  "items": [
    {"productoId": 5, "cantidad": 10, "precioUnitario": 100}
  ]
}
```
**Validar:** Inventario NO cambia

### Test 2: Confirmar Cotización
```http
PUT /api/ordenes/tabla/123
{
  "venta": true,
  "fecha": "2026-01-09",
  "clienteId": 1,
  "sedeId": 1,
  "items": [
    {"productoId": 5, "cantidad": 10, "precioUnitario": 100}
  ]
}
```
**Validar:** Inventario se descuenta 10 unidades

### Test 3: Revertir a Cotización
```http
PUT /api/ordenes/tabla/123
{
  "venta": false,
  ...
}
```
**Validar:** Inventario se restaura +10 unidades

---

## 📅 Fecha de Implementación
**9 de enero de 2026**

---

## 👤 Desarrollador
**JAAL** - CasaGlass Backend
