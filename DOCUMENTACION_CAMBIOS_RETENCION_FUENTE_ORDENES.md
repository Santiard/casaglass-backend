# DOCUMENTACIÓN DE CAMBIOS - RETENCIÓN DE FUENTE EN ÓRDENES

## FECHA: 2025-01-XX
## VERSIÓN: 2.0

---

## 📋 RESUMEN EJECUTIVO

Se agregó el campo `retencionFuente` (valor monetario) a la entidad `Orden` para almacenar el valor calculado de la retención en la fuente. El campo `tieneRetencionFuente` (boolean) se mantiene para indicar si la orden aplica retención, pero ahora también se guarda el **valor monetario calculado** para trazabilidad.

**Cambio principal:** El backend ahora calcula automáticamente el valor de la retención cuando `tieneRetencionFuente = true` y la base imponible supera el umbral configurado.

---

## 🔄 CAMBIOS EN LA ENTIDAD ORDEN

### Nuevo Campo Agregado

```java
/**
 * Valor monetario de la retención en la fuente
 * Se calcula automáticamente cuando tieneRetencionFuente = true
 * y la base imponible (subtotal - descuentos) supera el umbral configurado
 */
@Column(name = "retencion_fuente", nullable = false)
private Double retencionFuente = 0.0;
```

### Campos Existentes (Sin Cambios)

```java
/**
 * Indica si la orden tiene retención de fuente aplicada
 */
@Column(name = "tiene_retencion_fuente", nullable = false)
private boolean tieneRetencionFuente = false;
```

### Cambio en el Cálculo del Total

**ANTES:**
```java
total = subtotal - descuentos
```

**AHORA:**
```java
total = subtotal - descuentos - retencionFuente
```

---

## 📦 CAMBIOS EN LOS DTOs

### 1. OrdenTablaDTO

**Campo agregado:**
```java
private Double retencionFuente; // Valor monetario de la retención en la fuente
```

**Estructura completa:**
```java
{
  "id": 123,
  "numero": 1001,
  "fecha": "2025-01-15",
  "subtotal": 1000000.00,
  "descuentos": 0.00,
  "retencionFuente": 25000.00,  // ✅ NUEVO CAMPO
  "tieneRetencionFuente": true,  // Campo existente
  "total": 975000.00,            // Ahora incluye: subtotal - descuentos - retencionFuente
  // ... otros campos
}
```

### 2. OrdenVentaDTO

**Sin cambios en estructura** - El frontend sigue enviando:
```java
private boolean tieneRetencionFuente = false; // El frontend marca si aplica
```

**IMPORTANTE:** El frontend NO envía el valor de `retencionFuente`. El backend lo calcula automáticamente.

### 3. OrdenActualizarDTO

**Sin cambios en estructura** - El frontend sigue enviando:
```java
private boolean tieneRetencionFuente = false; // El frontend marca si aplica
```

**IMPORTANTE:** El frontend NO envía el valor de `retencionFuente`. El backend lo recalcula automáticamente.

---

## 🧮 CÓMO FUNCIONA EL CÁLCULO DE RETENCIÓN

### Condiciones para Aplicar Retención

1. ✅ `tieneRetencionFuente = true` (marcado por el frontend)
2. ✅ Base imponible >= Umbral configurado
   - Base imponible = `subtotal - descuentos`
   - Umbral = `reteThreshold` desde `BusinessSettings` (default: 1,000,000 COP)

### Fórmula de Cálculo

```java
// 1. Calcular base imponible
baseImponible = subtotal - descuentos

// 2. Verificar si supera el umbral
if (baseImponible >= reteThreshold && tieneRetencionFuente) {
    // 3. Calcular retención
    retencionFuente = baseImponible * (reteRate / 100)
    // reteRate viene de BusinessSettings (default: 2.5%)
} else {
    retencionFuente = 0.0
}

// 4. Calcular total
total = subtotal - descuentos - retencionFuente
```

### Ejemplo Práctico

**Datos de entrada:**
- `subtotal`: 1,200,000 COP
- `descuentos`: 50,000 COP
- `tieneRetencionFuente`: `true`
- `reteThreshold`: 1,000,000 COP (desde BusinessSettings)
- `reteRate`: 2.5% (desde BusinessSettings)

**Cálculo:**
1. Base imponible = 1,200,000 - 50,000 = 1,150,000 COP
2. ¿Supera umbral? 1,150,000 >= 1,000,000 → ✅ Sí
3. Retención = 1,150,000 × (2.5 / 100) = 28,750 COP
4. Total = 1,200,000 - 50,000 - 28,750 = 1,121,250 COP

**Resultado:**
```json
{
  "subtotal": 1200000.00,
  "descuentos": 50000.00,
  "retencionFuente": 28750.00,
  "tieneRetencionFuente": true,
  "total": 1121250.00
}
```

---

## 🌐 ENDPOINTS AFECTADOS

### 1. POST /api/ordenes/venta

**Descripción:** Crear una nueva orden de venta

**Body esperado (sin cambios):**
```json
{
  "fecha": "2025-01-15",
  "clienteId": 5,
  "sedeId": 1,
  "trabajadorId": 2,
  "obra": "Proyecto ABC",
  "descripcion": "Descripción de la orden",
  "venta": true,
  "credito": false,
  "incluidaEntrega": false,
  "tieneRetencionFuente": true,  // ✅ Frontend marca si aplica
  "descuentos": 50000.00,
  "items": [
    {
      "productoId": 10,
      "cantidad": 5,
      "precioUnitario": 240000.00,
      "descripcion": "Producto A"
    }
  ]
}
```

**Respuesta (cambios):**
```json
{
  "id": 123,
  "numero": 1001,
  "fecha": "2025-01-15",
  "subtotal": 1200000.00,
  "descuentos": 50000.00,
  "retencionFuente": 28750.00,  // ✅ NUEVO: Calculado automáticamente
  "tieneRetencionFuente": true,
  "total": 1121250.00,  // ✅ Cambiado: Ahora incluye retención
  // ... otros campos
}
```

**Notas:**
- El frontend NO envía `retencionFuente` en el body
- El backend calcula `retencionFuente` automáticamente
- El `total` ahora se calcula restando la retención

---

### 2. POST /api/ordenes/venta-credito

**Descripción:** Crear una orden de venta a crédito

**Body esperado (sin cambios):**
```json
{
  "fecha": "2025-01-15",
  "clienteId": 5,
  "sedeId": 1,
  "trabajadorId": 2,
  "venta": true,
  "credito": true,
  "tieneRetencionFuente": true,  // ✅ Frontend marca si aplica
  "descuentos": 0.00,
  "items": [
    {
      "productoId": 10,
      "cantidad": 5,
      "precioUnitario": 240000.00
    }
  ]
}
```

**Respuesta (cambios):**
```json
{
  "id": 124,
  "numero": 1002,
  "subtotal": 1200000.00,
  "descuentos": 0.00,
  "retencionFuente": 30000.00,  // ✅ NUEVO: Calculado automáticamente
  "tieneRetencionFuente": true,
  "total": 1170000.00,  // ✅ Cambiado: Ahora incluye retención
  "credito": true,
  "creditoDetalle": {
    "total": 1170000.00,  // ✅ El crédito también usa el total con retención
    // ... otros campos
  }
  // ... otros campos
}
```

---

### 3. PUT /api/ordenes/{id}/venta

**Descripción:** Actualizar una orden de venta existente

**Body esperado (sin cambios):**
```json
{
  "fecha": "2025-01-15",
  "clienteId": 5,
  "sedeId": 1,
  "tieneRetencionFuente": true,  // ✅ Frontend puede cambiar este valor
  "descuentos": 100000.00,
  "items": [
    {
      "productoId": 10,
      "cantidad": 10,
      "precioUnitario": 240000.00
    }
  ]
}
```

**Respuesta (cambios):**
```json
{
  "id": 123,
  "subtotal": 2400000.00,
  "descuentos": 100000.00,
  "retencionFuente": 57500.00,  // ✅ NUEVO: Recalculado automáticamente
  "tieneRetencionFuente": true,
  "total": 2242500.00,  // ✅ Cambiado: Ahora incluye retención
  // ... otros campos
}
```

**Notas:**
- Si cambias `tieneRetencionFuente` de `false` a `true`, el backend recalcula la retención
- Si cambias `tieneRetencionFuente` de `true` a `false`, el backend establece `retencionFuente = 0.0`
- Si cambias `subtotal` o `descuentos`, el backend recalcula la retención si `tieneRetencionFuente = true`

---

### 4. PUT /api/ordenes/{id}/venta-credito

**Descripción:** Actualizar una orden de venta a crédito existente

**Body esperado:** Igual que `PUT /api/ordenes/{id}/venta`

**Respuesta:** Igual que `PUT /api/ordenes/{id}/venta` pero con `credito: true`

---

### 5. PUT /api/ordenes/{id}

**Descripción:** Actualizar orden desde la tabla (método genérico)

**Body esperado (sin cambios):**
```json
{
  "id": 123,
  "fecha": "2025-01-15",
  "tieneRetencionFuente": true,  // ✅ Frontend puede cambiar este valor
  "descuentos": 50000.00,
  "items": [
    {
      "id": 456,
      "productoId": 10,
      "cantidad": 5,
      "precioUnitario": 240000.00,
      "totalLinea": 1200000.00
    }
  ]
}
```

**Respuesta (cambios):**
```json
{
  "id": 123,
  "subtotal": 1200000.00,
  "descuentos": 50000.00,
  "retencionFuente": 28750.00,  // ✅ NUEVO: Recalculado automáticamente
  "tieneRetencionFuente": true,
  "total": 1121250.00,  // ✅ Cambiado: Ahora incluye retención
  // ... otros campos
}
```

---

### 6. GET /api/ordenes/tabla

**Descripción:** Obtener lista de órdenes para tabla (con filtros y paginación)

**Query Parameters:** Sin cambios

**Respuesta (cambios):**
```json
{
  "content": [
    {
      "id": 123,
      "numero": 1001,
      "subtotal": 1200000.00,
      "descuentos": 50000.00,
      "retencionFuente": 28750.00,  // ✅ NUEVO: Incluido en la respuesta
      "tieneRetencionFuente": true,
      "total": 1121250.00,  // ✅ Cambiado: Ahora incluye retención
      // ... otros campos
    }
  ],
  "totalElements": 100,
  "totalPages": 10,
  // ... otros campos de paginación
}
```

---

### 7. GET /api/ordenes/{id}

**Descripción:** Obtener una orden por ID

**Respuesta (cambios):**
```json
{
  "id": 123,
  "numero": 1001,
  "subtotal": 1200000.00,
  "descuentos": 50000.00,
  "retencionFuente": 28750.00,  // ✅ NUEVO: Incluido en la respuesta
  "tieneRetencionFuente": true,
  "total": 1121250.00,  // ✅ Cambiado: Ahora incluye retención
  // ... otros campos
}
```

---

### 8. PUT /api/ordenes/{id}/facturar

**Descripción:** Marcar orden como facturada (crea factura automáticamente)

**Body esperado:** Sin cambios
```json
{
  "facturada": true
}
```

**Cambio importante:** La factura ahora usa el valor de `retencionFuente` de la orden:

**Antes:**
```java
facturaDTO.setRetencionFuente(0.0);  // ❌ Siempre 0
```

**Ahora:**
```java
facturaDTO.setRetencionFuente(orden.getRetencionFuente());  // ✅ Usa el valor de la orden
```

**Respuesta:** Sin cambios en estructura, pero la factura creada tendrá el valor correcto de retención.

---

## 🔧 CONFIGURACIÓN DE RETENCIÓN

La retención se calcula usando valores de `BusinessSettings` que se almacenan en la base de datos.

### 📍 ¿De dónde viene el porcentaje?

**NO viene del frontend.** El backend obtiene los valores desde la tabla `business_settings` en la base de datos:

1. **Backend busca en BD:** `SELECT * FROM business_settings LIMIT 1`
2. **Si existe configuración:** Usa `reteRate` y `reteThreshold` de la BD
3. **Si NO existe:** Usa valores por defecto:
   - `reteRate`: 2.5%
   - `reteThreshold`: 1,000,000 COP

### 🌐 ENDPOINTS PARA GESTIONAR CONFIGURACIÓN

#### GET /api/business-settings

**Descripción:** Obtener la configuración actual

**Respuesta:**
```json
{
  "id": 1,
  "ivaRate": 19.0,
  "reteRate": 2.5,
  "reteThreshold": 1000000,
  "updatedAt": "2025-01-15"
}
```

**Nota:** Si no existe configuración, retorna valores por defecto (sin guardar en BD).

---

#### PUT /api/business-settings

**Descripción:** Actualizar la configuración actual (o crear si no existe)

**Body esperado:**
```json
{
  "ivaRate": 19.0,        // Porcentaje de IVA (0-100)
  "reteRate": 2.5,        // Porcentaje de retención (0-100)
  "reteThreshold": 1000000  // Umbral mínimo en COP
}
```

**Respuesta:**
```json
{
  "id": 1,
  "ivaRate": 19.0,
  "reteRate": 2.5,
  "reteThreshold": 1000000,
  "updatedAt": "2025-01-15"
}
```

**Ejemplo de uso:**
```http
PUT /api/business-settings
Content-Type: application/json

{
  "ivaRate": 19.0,
  "reteRate": 3.0,        // Cambiar retención a 3%
  "reteThreshold": 1500000  // Cambiar umbral a 1,500,000 COP
}
```

---

#### GET /api/business-settings/{id}

**Descripción:** Obtener configuración por ID

---

#### POST /api/business-settings

**Descripción:** Crear una nueva configuración

**Body:** Igual que PUT

---

#### PUT /api/business-settings/{id}

**Descripción:** Actualizar configuración por ID

**Body:** Igual que PUT sin ID

---

#### DELETE /api/business-settings/{id}

**Descripción:** Eliminar configuración por ID

---

### 📝 Notas Importantes

1. **Normalmente solo hay una configuración:** Se recomienda usar `PUT /api/business-settings` (sin ID)
2. **Los cambios afectan órdenes nuevas:** Las órdenes ya creadas mantienen su retención calculada
3. **Validaciones:**
   - `ivaRate`: 0-100
   - `reteRate`: 0-100
   - `reteThreshold`: >= 0

---

## 📝 RESUMEN DE CAMBIOS PARA EL FRONTEND

### ✅ Lo que NO cambia (compatibilidad hacia atrás)

1. **Body de creación/actualización:** El frontend sigue enviando solo `tieneRetencionFuente` (boolean)
2. **No necesita calcular retención:** El backend lo hace automáticamente
3. **No necesita enviar `retencionFuente`:** El backend lo calcula y guarda

### ✅ Lo que SÍ cambia (nuevo en respuestas)

1. **Respuestas incluyen `retencionFuente`:** Todos los endpoints que retornan órdenes ahora incluyen este campo
2. **El `total` cambió:** Ahora es `subtotal - descuentos - retencionFuente` (antes era `subtotal - descuentos`)
3. **Mostrar retención en UI:** El frontend puede mostrar el valor de `retencionFuente` en las tablas y detalles

### 📋 Checklist para el Frontend

- [ ] Actualizar interfaces/type definitions para incluir `retencionFuente: number`
- [ ] Mostrar `retencionFuente` en la tabla de órdenes (si aplica)
- [ ] Mostrar `retencionFuente` en el detalle de orden
- [ ] Verificar que el cálculo del total en el frontend coincida con el backend
- [ ] Actualizar cualquier cálculo manual de total para incluir retención
- [ ] Verificar que las facturas muestren correctamente la retención de la orden

---

## 🧪 EJEMPLOS COMPLETOS

### Ejemplo 1: Crear Orden con Retención

**Request:**
```http
POST /api/ordenes/venta
Content-Type: application/json

{
  "fecha": "2025-01-15",
  "clienteId": 5,
  "sedeId": 1,
  "tieneRetencionFuente": true,
  "descuentos": 0,
  "items": [
    {
      "productoId": 10,
      "cantidad": 5,
      "precioUnitario": 240000.00
    }
  ]
}
```

**Response:**
```json
{
  "id": 123,
  "numero": 1001,
  "fecha": "2025-01-15",
  "subtotal": 1200000.00,
  "descuentos": 0.00,
  "retencionFuente": 30000.00,
  "tieneRetencionFuente": true,
  "total": 1170000.00,
  "estado": "ACTIVA"
}
```

**Cálculo:**
- Base imponible: 1,200,000 - 0 = 1,200,000 COP
- ¿Supera umbral? 1,200,000 >= 1,000,000 → ✅ Sí
- Retención: 1,200,000 × 0.025 = 30,000 COP
- Total: 1,200,000 - 0 - 30,000 = 1,170,000 COP

---

### Ejemplo 2: Crear Orden SIN Retención (no supera umbral)

**Request:**
```http
POST /api/ordenes/venta
Content-Type: application/json

{
  "fecha": "2025-01-15",
  "clienteId": 5,
  "sedeId": 1,
  "tieneRetencionFuente": true,
  "descuentos": 0,
  "items": [
    {
      "productoId": 10,
      "cantidad": 1,
      "precioUnitario": 500000.00
    }
  ]
}
```

**Response:**
```json
{
  "id": 124,
  "numero": 1002,
  "fecha": "2025-01-15",
  "subtotal": 500000.00,
  "descuentos": 0.00,
  "retencionFuente": 0.00,  // ✅ No aplica porque no supera umbral
  "tieneRetencionFuente": true,  // Frontend lo marcó, pero no aplica
  "total": 500000.00,
  "estado": "ACTIVA"
}
```

**Cálculo:**
- Base imponible: 500,000 - 0 = 500,000 COP
- ¿Supera umbral? 500,000 >= 1,000,000 → ❌ No
- Retención: 0.00 COP (no aplica)
- Total: 500,000 - 0 - 0 = 500,000 COP

---

### Ejemplo 3: Actualizar Orden - Cambiar Retención

**Request:**
```http
PUT /api/ordenes/124/venta
Content-Type: application/json

{
  "tieneRetencionFuente": false,  // ✅ Cambiar de true a false
  "descuentos": 0,
  "items": [
    {
      "productoId": 10,
      "cantidad": 1,
      "precioUnitario": 500000.00
    }
  ]
}
```

**Response:**
```json
{
  "id": 124,
  "subtotal": 500000.00,
  "descuentos": 0.00,
  "retencionFuente": 0.00,  // ✅ Se establece en 0 porque tieneRetencionFuente = false
  "tieneRetencionFuente": false,
  "total": 500000.00,
  "estado": "ACTIVA"
}
```

---

## ⚠️ NOTAS IMPORTANTES

1. **El frontend NO debe calcular retención:** El backend lo hace automáticamente
2. **El frontend NO debe enviar `retencionFuente`:** Solo envía `tieneRetencionFuente` (boolean)
3. **El `total` siempre incluye retención:** Si `retencionFuente > 0`, ya está descontado del total
4. **La retención se recalcula automáticamente:** Cada vez que se crea o actualiza una orden, se recalcula si aplica
5. **Facturación usa retención de la orden:** Cuando se factura, se usa el valor guardado en la orden

---

## 🔄 MIGRACIÓN DE BASE DE DATOS

Ejecutar el script SQL:
```sql
ALTER TABLE ordenes 
ADD COLUMN retencion_fuente DECIMAL(19, 2) NOT NULL DEFAULT 0.00;
```

**Nota:** Todas las órdenes existentes tendrán `retencionFuente = 0.00` por defecto.

---

## 📞 CONTACTO

Si tienes dudas sobre estos cambios, consulta con el equipo de desarrollo.

---

## 📅 HISTORIAL DE CAMBIOS

- **2025-01-XX:** Agregado campo `retencionFuente` a la entidad Orden
- **2025-01-XX:** Implementado cálculo automático de retención en backend
- **2025-01-XX:** Actualizado cálculo de total para incluir retención
- **2025-01-XX:** Actualizado endpoint de facturación para usar retención de la orden

