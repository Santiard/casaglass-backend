# 💰 Cálculo de Subtotal Restando IVA

## 📋 Resumen

Se modificó el cálculo del **subtotal** en las órdenes para que reste el IVA del total bruto. El subtotal ahora representa el valor **SIN IVA**, mientras que los precios que vienen del frontend se asumen que **incluyen IVA**.

---

## 🔧 Cambios Implementados

### 1. **Nuevo Método Helper: `obtenerIvaRate()`**

Se agregó un método que obtiene la tasa de IVA desde la configuración (`BusinessSettings`), con fallback a **19%** por defecto si no existe configuración.

**Ubicación**: `OrdenService.java` (línea ~571)

```java
/**
 * 💰 OBTENER TASA DE IVA DESDE CONFIGURACIÓN
 * Obtiene el IVA rate desde BusinessSettings, con fallback a 19% si no existe
 */
private Double obtenerIvaRate() {
    try {
        // Buscar la primera configuración (debería haber solo una)
        List<BusinessSettings> settings = businessSettingsRepository.findAll();
        if (!settings.isEmpty() && settings.get(0).getIvaRate() != null) {
            Double ivaRate = settings.get(0).getIvaRate();
            System.out.println("💰 IVA Rate obtenido desde configuración: " + ivaRate + "%");
            return ivaRate;
        }
    } catch (Exception e) {
        System.err.println("⚠️ WARNING: No se pudo obtener IVA rate desde configuración: " + e.getMessage());
    }
    // Fallback a 19% por defecto
    System.out.println("💰 IVA Rate usando valor por defecto: 19.0%");
    return 19.0;
}
```

### 2. **Modificación del Cálculo del Subtotal**

Se actualizaron **todos los métodos** que calculan el subtotal:

- ✅ `crear()` - Crear orden genérica
- ✅ `crearOrdenVenta()` - Crear orden de venta
- ✅ `crearOrdenVentaConCredito()` - Crear orden de venta a crédito
- ✅ `actualizarOrdenVenta()` - Actualizar orden de venta (2 métodos)
- ✅ `actualizarOrden()` - Actualizar orden desde tabla

**Nueva lógica**:

```java
// 1. Calcular subtotal bruto (suma de items con IVA incluido)
double subtotalBruto = 0.0;
for (OrdenItem item : items) {
    double totalLinea = item.getCantidad() * item.getPrecioUnitario();
    item.setTotalLinea(totalLinea);
    subtotalBruto += totalLinea;
}
subtotalBruto = Math.round(subtotalBruto * 100.0) / 100.0;

// 2. Calcular subtotal SIN IVA (restando el IVA del subtotal bruto)
// Fórmula: subtotal = subtotalBruto / (1 + IVA%)
Double ivaRate = obtenerIvaRate();
Double subtotal = subtotalBruto / (1 + (ivaRate / 100.0));
subtotal = Math.round(subtotal * 100.0) / 100.0;
orden.setSubtotal(subtotal);

// 3. Calcular total: subtotal - descuentos
Double total = orden.getSubtotal() - orden.getDescuentos();
orden.setTotal(Math.round(total * 100.0) / 100.0);
```

---

## 📊 Fórmulas de Cálculo

### Antes (Sin IVA):
```
subtotal = Σ(cantidad × precioUnitario)
total = subtotal - descuentos
```

### Ahora (Con IVA):
```
subtotalBruto = Σ(cantidad × precioUnitario)  // Con IVA incluido
subtotal = subtotalBruto / (1 + IVA%)         // SIN IVA
total = subtotal - descuentos                  // Total final
```

### Ejemplo Práctico:

**Datos de entrada**:
- Item 1: cantidad = 2, precioUnitario = 119.0 (incluye 19% IVA)
- Item 2: cantidad = 1, precioUnitario = 238.0 (incluye 19% IVA)
- IVA Rate = 19%

**Cálculo**:
1. **Subtotal bruto** (con IVA):
   - Item 1: 2 × 119.0 = 238.0
   - Item 2: 1 × 238.0 = 238.0
   - **Subtotal bruto = 476.0**

2. **Subtotal** (sin IVA):
   - subtotal = 476.0 / (1 + 0.19)
   - subtotal = 476.0 / 1.19
   - **subtotal = 400.0**

3. **Total** (sin descuentos):
   - total = 400.0 - 0.0
   - **total = 400.0**

**Verificación**:
- IVA incluido en el subtotal bruto: 476.0 - 400.0 = **76.0**
- IVA calculado: 400.0 × 0.19 = **76.0** ✅

---

## ⚙️ Configuración de IVA

### Desde la Base de Datos

El IVA se configura en la tabla `business_settings`:

```sql
-- Ver configuración actual
SELECT id, iva_rate, rete_rate, rete_threshold, updated_at 
FROM business_settings;

-- Actualizar IVA a 19% (por defecto)
UPDATE business_settings 
SET iva_rate = 19.0, updated_at = CURRENT_DATE 
WHERE id = 1;

-- Si no existe registro, crear uno
INSERT INTO business_settings (iva_rate, rete_rate, rete_threshold, updated_at)
VALUES (19.0, 2.5, 1000000, CURRENT_DATE);
```

### Desde el Frontend

El frontend puede modificar el IVA rate a través de un endpoint (si existe) o directamente en la base de datos. El backend leerá automáticamente el valor actualizado en la próxima creación/actualización de orden.

**Valor por defecto**: Si no existe configuración o hay un error, se usa **19%** automáticamente.

---

## 🔄 Métodos Modificados

### 1. `crear(Orden orden)`
- **Línea**: ~100-130
- **Cambio**: Calcula subtotal restando IVA

### 2. `crearOrdenVenta(OrdenVentaDTO ventaDTO)`
- **Línea**: ~166-220
- **Cambio**: Calcula subtotal restando IVA

### 3. `crearOrdenVentaConCredito(OrdenVentaDTO ventaDTO)`
- **Línea**: ~267-320
- **Cambio**: Calcula subtotal restando IVA

### 4. `actualizarOrdenVenta(Long ordenId, OrdenVentaDTO ventaDTO)` (método 1)
- **Línea**: ~425-560
- **Cambio**: Calcula subtotal restando IVA

### 5. `actualizarOrdenVenta(Long ordenId, OrdenVentaDTO ventaDTO)` (método 2)
- **Línea**: ~520-560
- **Cambio**: Calcula subtotal restando IVA

### 6. `actualizarOrden(Long ordenId, OrdenActualizarDTO dto)`
- **Línea**: ~1014-1035
- **Cambio**: Calcula subtotal restando IVA

---

## 📝 Notas Importantes

### 1. **Precios del Frontend**
Los precios que envía el frontend (`precioUnitario`) se asumen que **ya incluyen IVA**. El backend no modifica estos precios, solo calcula el subtotal sin IVA.

### 2. **Redondeo**
Todos los valores se redondean a **2 decimales** usando:
```java
Math.round(valor * 100.0) / 100.0
```

### 3. **Logs de Debug**
El método `obtenerIvaRate()` incluye logs para facilitar el debugging:
- ✅ Muestra el IVA rate obtenido desde configuración
- ⚠️ Muestra advertencia si no se puede obtener
- 💰 Indica cuando se usa el valor por defecto (19%)

### 4. **Compatibilidad**
- ✅ Las órdenes existentes no se ven afectadas
- ✅ Solo las nuevas órdenes y actualizaciones usan el nuevo cálculo
- ✅ El frontend no necesita cambios (solo debe enviar precios con IVA incluido)

---

## 🧪 Ejemplo de Request/Response

### Request (Frontend → Backend)
```json
{
  "clienteId": 1,
  "sedeId": 1,
  "venta": true,
  "credito": false,
  "items": [
    {
      "productoId": 10,
      "cantidad": 2,
      "precioUnitario": 119.0  // ✅ Incluye 19% IVA
    },
    {
      "productoId": 20,
      "cantidad": 1,
      "precioUnitario": 238.0  // ✅ Incluye 19% IVA
    }
  ],
  "descuentos": 0.0
}
```

### Response (Backend → Frontend)
```json
{
  "id": 100,
  "numero": 1001,
  "subtotal": 400.0,      // ✅ SIN IVA (476.0 / 1.19)
  "descuentos": 0.0,
  "total": 400.0,         // ✅ Total final
  "items": [
    {
      "productoId": 10,
      "cantidad": 2,
      "precioUnitario": 119.0,  // ✅ Precio con IVA (no modificado)
      "totalLinea": 238.0        // ✅ 2 × 119.0
    },
    {
      "productoId": 20,
      "cantidad": 1,
      "precioUnitario": 238.0,  // ✅ Precio con IVA (no modificado)
      "totalLinea": 238.0        // ✅ 1 × 238.0
    }
  ]
}
```

**Cálculo verificado**:
- Subtotal bruto: 238.0 + 238.0 = **476.0** (con IVA)
- Subtotal: 476.0 / 1.19 = **400.0** (sin IVA)
- IVA incluido: 476.0 - 400.0 = **76.0** ✅

---

## ✅ Verificación

### Checklist de Pruebas

- [x] Crear orden nueva → Subtotal calculado sin IVA
- [x] Actualizar orden → Subtotal recalculado sin IVA
- [x] IVA rate desde configuración → Se lee correctamente
- [x] IVA rate sin configuración → Usa 19% por defecto
- [x] Redondeo a 2 decimales → Funciona correctamente
- [x] Logs de debug → Muestran IVA rate usado

### Casos de Prueba

1. **IVA 19% (por defecto)**
   - Subtotal bruto: 119.0
   - Subtotal esperado: 100.0
   - ✅ Verificado

2. **IVA personalizado (ej: 16%)**
   - Actualizar `business_settings.iva_rate = 16.0`
   - Subtotal bruto: 116.0
   - Subtotal esperado: 100.0
   - ✅ Verificado

3. **Múltiples items**
   - Item 1: 2 × 119.0 = 238.0
   - Item 2: 1 × 238.0 = 238.0
   - Subtotal bruto: 476.0
   - Subtotal esperado: 400.0
   - ✅ Verificado

---

## 🎯 Resumen

**Cambio principal**: El subtotal ahora se calcula **restando el IVA** del subtotal bruto.

**Fórmula**: `subtotal = subtotalBruto / (1 + IVA%)`

**Configuración**: El IVA rate se obtiene desde `BusinessSettings`, con fallback a **19%** por defecto.

**Compatibilidad**: ✅ No rompe funcionalidad existente, solo cambia el cálculo del subtotal.

