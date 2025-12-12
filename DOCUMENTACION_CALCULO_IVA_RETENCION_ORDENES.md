# DOCUMENTACIÓN: CÁLCULO DE IVA Y RETENCIÓN DE FUENTE EN ÓRDENES

## FECHA: 2025-01-XX
## VERSIÓN: 3.0

---

## 📋 RESUMEN EJECUTIVO

Se corrigió la lógica de cálculo de IVA y retención de fuente en las órdenes según la especificación del frontend. Los cambios principales son:

1. **El campo `subtotal` ahora almacena la base imponible SIN IVA** (antes almacenaba el total facturado CON IVA)
2. **Se agregó el campo `iva`** a la entidad Orden para almacenar el valor del IVA calculado
3. **La retención de fuente se calcula sobre el subtotal sin IVA** (antes se calculaba sobre el total facturado)
4. **El total de la orden es el total facturado menos descuentos** (sin restar la retención, que solo afecta el valor a pagar)

---

## 🔄 CAMBIOS EN LA ENTIDAD ORDEN

### Nuevo Campo Agregado

```java
/**
 * Valor del IVA calculado
 * Se calcula como: (suma de items - descuentos) - subtotal (base sin IVA)
 */
@Column(nullable = false)
private Double iva = 0.0;
```

### Cambios en Documentación de Campos

```java
/**
 * Subtotal de la orden (base imponible SIN IVA)
 * Se calcula como: (suma de items - descuentos) / 1.19
 */
@Column(nullable = false)
private Double subtotal = 0.0;

/**
 * Total de la orden (total facturado CON IVA, sin restar retención)
 * Se calcula como: suma de items - descuentos
 */
@Column(nullable = false)
private Double total = 0.0;
```

---

## 🧮 LÓGICA DE CÁLCULO

### Paso 1: Calcular Subtotal Facturado (Total con IVA)

```java
// Suma de (precioUnitario × cantidad) de todos los items
double subtotalFacturado = 0.0;
for (OrdenItem item : orden.getItems()) {
    double totalLinea = item.getPrecioUnitario() * item.getCantidad();
    item.setTotalLinea(totalLinea);
    subtotalFacturado += totalLinea;
}
// Ejemplo: 2 items de $1,000,000 cada uno → subtotalFacturado = 2,000,000
```

**Nota:** El `precioUnitario` ya incluye IVA (19%).

### Paso 2: Calcular Base Imponible (Subtotal sin IVA)

```java
// Base imponible = (subtotal facturado - descuentos) / 1.19
Double descuentos = orden.getDescuentos(); // Ejemplo: 0
Double baseConIva = subtotalFacturado - descuentos; // 2,000,000 - 0 = 2,000,000
Double ivaRate = 19.0; // 19%
Double subtotalSinIva = baseConIva / (1.0 + (ivaRate / 100.0)); 
// 2,000,000 / 1.19 = 1,680,672.27
```

### Paso 3: Calcular IVA

```java
Double iva = baseConIva - subtotalSinIva;
// 2,000,000 - 1,680,672.27 = 319,327.73
```

### Paso 4: Calcular Retención de Fuente (si aplica)

```java
Double retencionFuente = 0.0;
if (orden.isTieneRetencionFuente()) {
    // Obtener configuración desde BusinessSettings
    Double reteRate = 2.5; // 2.5%
    Long reteThreshold = 1_000_000L; // Umbral mínimo
    
    // Verificar si supera el umbral
    if (subtotalSinIva >= reteThreshold) {
        // Calcular sobre el subtotal SIN IVA
        retencionFuente = subtotalSinIva * (reteRate / 100.0);
        // 1,680,672.27 × 0.025 = 42,016.81
    }
}
```

### Paso 5: Calcular Total de la Orden

```java
// El total es el total facturado menos descuentos (sin restar retención)
Double total = subtotalFacturado - descuentos;
// 2,000,000 - 0 = 2,000,000
```

**Nota:** La retención NO se resta del total de la orden. Solo afecta el valor a pagar.

---

## 📊 EJEMPLO COMPLETO

### Datos de Entrada

```json
{
  "descuentos": 0,
  "tieneRetencionFuente": true,
  "items": [
    {
      "productoId": 1,
      "cantidad": 2,
      "precioUnitario": 1000000  // Precio CON IVA incluido
    }
  ]
}
```

### Cálculos del Backend

| Concepto | Cálculo | Valor (COP) |
|----------|---------|-------------|
| **Subtotal Facturado** | `2 × 1,000,000` | 2,000,000 |
| **Base con IVA** | `2,000,000 - 0` | 2,000,000 |
| **Subtotal (Base sin IVA)** | `2,000,000 / 1.19` | 1,680,672.27 |
| **IVA (19%)** | `2,000,000 - 1,680,672.27` | 319,327.73 |
| **Retención (2.5%)** | `1,680,672.27 × 0.025` | 42,016.81 |
| **Total Orden** | `2,000,000 - 0` | 2,000,000 |
| **Valor a Pagar** | `2,000,000 - 42,016.81` | 1,957,983.19 |

### Valores Guardados en la BD

```java
orden.setSubtotal(1_680_672.27);      // Base sin IVA
orden.setIva(319_327.73);             // IVA
orden.setDescuentos(0.0);              // Descuentos
orden.setRetencionFuente(42_016.81);   // Retención
orden.setTotal(2_000_000.0);           // Total facturado
```

---

## 📦 CAMBIOS EN LOS DTOs

### OrdenTablaDTO

**Campos agregados/modificados:**

```java
private Double subtotal; // Base imponible SIN IVA (CAMBIADO)
private Double iva;      // Valor del IVA calculado (NUEVO)
private Double descuentos;
private Double total;    // Total facturado (sin restar retención)
```

**Ejemplo de respuesta:**

```json
{
  "id": 123,
  "numero": 1001,
  "subtotal": 1680672.27,    // Base sin IVA
  "iva": 319327.73,          // IVA calculado
  "descuentos": 0.0,
  "retencionFuente": 42016.81,
  "total": 2000000.0,         // Total facturado
  "tieneRetencionFuente": true
}
```

---

## 🔄 MIGRACIÓN DE BASE DE DATOS

### Script SQL: Agregar Columna IVA

```sql
ALTER TABLE ordenes 
ADD COLUMN iva DECIMAL(19, 2) NOT NULL DEFAULT 0.00 
COMMENT 'Valor monetario del IVA calculado. Se calcula como: (total facturado - descuentos) - subtotal (base sin IVA)';
```

**Nota:** Todas las órdenes existentes tendrán `iva = 0.00` por defecto. Si se desea calcular el IVA para órdenes existentes, se puede ejecutar:

```sql
-- OPCIONAL: Calcular IVA para órdenes existentes
UPDATE ordenes 
SET iva = (subtotal * 0.19) / 1.19
WHERE subtotal > 0;
```

**⚠️ IMPORTANTE:** Este script opcional asume que el `subtotal` actual es el total facturado CON IVA. Si ya se migró a la nueva lógica, no ejecutar este script.

---

## ⚠️ CAMBIOS IMPORTANTES

### 1. El campo `subtotal` cambió de significado

**ANTES:**
- `subtotal` = Total facturado CON IVA incluido

**AHORA:**
- `subtotal` = Base imponible SIN IVA (subtotal facturado / 1.19)

### 2. El total NO incluye la retención

**ANTES:**
- `total` = subtotal - descuentos - retencionFuente

**AHORA:**
- `total` = subtotal facturado - descuentos (sin restar retención)

### 3. La retención se calcula sobre el subtotal sin IVA

**ANTES:**
- `retencionFuente` = (subtotal - descuentos) × reteRate

**AHORA:**
- `retencionFuente` = subtotalSinIva × reteRate

---

## ✅ CHECKLIST DE VERIFICACIÓN

- [x] Campo `iva` agregado a la entidad Orden
- [x] Script SQL creado para agregar columna `iva`
- [x] Método `calcularValoresMonetariosOrden()` implementado
- [x] Todos los métodos de creación/actualización actualizados
- [x] Campo `iva` agregado a `OrdenTablaDTO`
- [x] Método de conversión actualizado para incluir `iva`
- [x] Compilación exitosa sin errores

---

## 📞 CONTACTO

Si tienes dudas sobre estos cambios, consulta:
- Especificación del frontend: `ESPECIFICACION_CALCULO_RETENCION_ORDENES.md`
- Código fuente: `OrdenService.calcularValoresMonetariosOrden()`

---

## 📅 HISTORIAL DE CAMBIOS

- **2025-01-XX:** Corregida lógica de cálculo de IVA y retención según especificación
- **2025-01-XX:** Agregado campo `iva` a la entidad Orden
- **2025-01-XX:** Actualizado cálculo de `subtotal` para almacenar base sin IVA
- **2025-01-XX:** Corregido cálculo de `retencionFuente` para usar subtotal sin IVA
- **2025-01-XX:** Corregido cálculo de `total` para no restar retención

