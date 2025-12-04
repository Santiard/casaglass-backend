# 📋 Cambios: Campo `tieneRetencionFuente` en Orden

## 🔍 Resumen del Cambio

Se agregó un nuevo campo booleano `tieneRetencionFuente` a la entidad `Orden` para indicar si la orden tiene retención de fuente aplicada o no.

### Campo Agregado
- **Nombre**: `tieneRetencionFuente`
- **Tipo**: `boolean`
- **Valor por defecto**: `false`
- **Nullable**: `false` (siempre tiene un valor)
- **Columna BD**: `tiene_retencion_fuente`

---

## 📝 Cambios en la Base de Datos

### Migración SQL Requerida

```sql
ALTER TABLE ordenes 
ADD COLUMN tiene_retencion_fuente BOOLEAN NOT NULL DEFAULT FALSE;
```

**⚠️ IMPORTANTE**: Debes ejecutar esta migración SQL antes de usar el nuevo campo.

---

## 🔄 Endpoints Afectados

### 1. POST /api/ordenes/venta

**Descripción**: Crear una nueva orden de venta

#### Body Request (Cambios)

**Antes**:
```json
{
  "fecha": "2025-01-15",
  "obra": "Casa nueva",
  "descripcion": "Orden de prueba",
  "venta": true,
  "credito": false,
  "incluidaEntrega": false,
  "descuentos": 0.0,
  "clienteId": 1,
  "sedeId": 1,
  "trabajadorId": 5,
  "items": [...]
}
```

**Ahora** (Nuevo campo agregado):
```json
{
  "fecha": "2025-01-15",
  "obra": "Casa nueva",
  "descripcion": "Orden de prueba",
  "venta": true,
  "credito": false,
  "incluidaEntrega": false,
  "tieneRetencionFuente": true,  // ✅ NUEVO CAMPO
  "descuentos": 0.0,
  "clienteId": 1,
  "sedeId": 1,
  "trabajadorId": 5,
  "items": [...]
}
```

**Campo `tieneRetencionFuente`**:
- **Tipo**: `boolean`
- **Requerido**: No (por defecto es `false`)
- **Descripción**: Indica si la orden tiene retención de fuente aplicada

#### Response (Sin cambios)

La respuesta sigue siendo la misma estructura `Orden`, pero ahora incluye el nuevo campo:

```json
{
  "id": 100,
  "numero": 1001,
  "fecha": "2025-01-15",
  "obra": "Casa nueva",
  "descripcion": "Orden de prueba",
  "venta": true,
  "credito": false,
  "incluidaEntrega": false,
  "tieneRetencionFuente": true,  // ✅ NUEVO CAMPO EN RESPUESTA
  "subtotal": 100000.0,
  "descuentos": 0.0,
  "total": 100000.0,
  "estado": "ACTIVA",
  "cliente": {...},
  "sede": {...},
  "items": [...]
}
```

---

### 2. PUT /api/ordenes/venta/{id}

**Descripción**: Actualizar una orden de venta existente

#### Body Request (Cambios)

**Antes**:
```json
{
  "fecha": "2025-01-15",
  "obra": "Casa nueva",
  "descripcion": "Orden actualizada",
  "venta": true,
  "credito": false,
  "incluidaEntrega": false,
  "descuentos": 0.0,
  "clienteId": 1,
  "sedeId": 1,
  "trabajadorId": 5,
  "items": [...]
}
```

**Ahora** (Nuevo campo agregado):
```json
{
  "fecha": "2025-01-15",
  "obra": "Casa nueva",
  "descripcion": "Orden actualizada",
  "venta": true,
  "credito": false,
  "incluidaEntrega": false,
  "tieneRetencionFuente": true,  // ✅ NUEVO CAMPO
  "descuentos": 0.0,
  "clienteId": 1,
  "sedeId": 1,
  "trabajadorId": 5,
  "items": [...]
}
```

**Campo `tieneRetencionFuente`**:
- **Tipo**: `boolean`
- **Requerido**: No (si no se envía, mantiene el valor actual)
- **Descripción**: Actualiza si la orden tiene retención de fuente aplicada

#### Response (Sin cambios)

La respuesta sigue siendo la misma estructura `Orden`, pero ahora incluye el nuevo campo actualizado.

---

### 3. GET /api/ordenes/tabla

**Descripción**: Obtener lista de órdenes optimizada para tabla

#### Query Parameters (Sin cambios)

```
GET /api/ordenes/tabla?sedeId=1
GET /api/ordenes/tabla?clienteId=1
GET /api/ordenes/tabla?trabajadorId=5
GET /api/ordenes/tabla
```

#### Response (Cambios)

**Antes**:
```json
[
  {
    "id": 100,
    "numero": 1001,
    "fecha": "2025-01-15",
    "obra": "Casa nueva",
    "descripcion": "Orden de prueba",
    "venta": true,
    "credito": false,
    "estado": "ACTIVA",
    "facturada": false,
    "subtotal": 100000.0,
    "descuentos": 0.0,
    "total": 100000.0,
    "cliente": {...},
    "sede": {...},
    "trabajador": {...},
    "items": [...]
  }
]
```

**Ahora** (Nuevo campo agregado):
```json
[
  {
    "id": 100,
    "numero": 1001,
    "fecha": "2025-01-15",
    "obra": "Casa nueva",
    "descripcion": "Orden de prueba",
    "venta": true,
    "credito": false,
    "tieneRetencionFuente": true,  // ✅ NUEVO CAMPO
    "estado": "ACTIVA",
    "facturada": false,
    "subtotal": 100000.0,
    "descuentos": 0.0,
    "total": 100000.0,
    "cliente": {...},
    "sede": {...},
    "trabajador": {...},
    "items": [...]
  }
]
```

---

### 4. GET /api/ordenes/{id}

**Descripción**: Obtener una orden por ID

#### Response (Cambios)

**Antes**:
```json
{
  "id": 100,
  "numero": 1001,
  "fecha": "2025-01-15",
  "obra": "Casa nueva",
  "descripcion": "Orden de prueba",
  "venta": true,
  "credito": false,
  "incluidaEntrega": false,
  "subtotal": 100000.0,
  "descuentos": 0.0,
  "total": 100000.0,
  "estado": "ACTIVA",
  "cliente": {...},
  "sede": {...},
  "items": [...]
}
```

**Ahora** (Nuevo campo agregado):
```json
{
  "id": 100,
  "numero": 1001,
  "fecha": "2025-01-15",
  "obra": "Casa nueva",
  "descripcion": "Orden de prueba",
  "venta": true,
  "credito": false,
  "incluidaEntrega": false,
  "tieneRetencionFuente": true,  // ✅ NUEVO CAMPO
  "subtotal": 100000.0,
  "descuentos": 0.0,
  "total": 100000.0,
  "estado": "ACTIVA",
  "cliente": {...},
  "sede": {...},
  "items": [...]
}
```

---

### 5. GET /api/ordenes/{id}/detalle

**Descripción**: Obtener detalle completo de una orden

#### Response (Cambios)

**Antes**:
```json
{
  "id": 100,
  "numero": 1001,
  "fecha": "2025-01-15",
  "obra": "Casa nueva",
  "descripcion": "Orden de prueba",
  "subtotal": 100000.0,
  "descuentos": 0.0,
  "total": 100000.0,
  "cliente": {...},
  "items": [...]
}
```

**Ahora** (Nuevo campo agregado):
```json
{
  "id": 100,
  "numero": 1001,
  "fecha": "2025-01-15",
  "obra": "Casa nueva",
  "descripcion": "Orden de prueba",
  "tieneRetencionFuente": true,  // ✅ NUEVO CAMPO
  "subtotal": 100000.0,
  "descuentos": 0.0,
  "total": 100000.0,
  "cliente": {...},
  "items": [...]
}
```

---

### 6. PUT /api/ordenes/tabla/{id}

**Descripción**: Actualizar una orden desde la tabla

#### Body Request (Cambios)

**Antes**:
```json
{
  "id": 100,
  "numero": 1001,
  "fecha": "2025-01-15",
  "obra": "Casa nueva",
  "descripcion": "Orden actualizada",
  "venta": true,
  "credito": false,
  "descuentos": 0.0,
  "clienteId": 1,
  "sedeId": 1,
  "trabajadorId": 5,
  "items": [...]
}
```

**Ahora** (Nuevo campo agregado):
```json
{
  "id": 100,
  "numero": 1001,
  "fecha": "2025-01-15",
  "obra": "Casa nueva",
  "descripcion": "Orden actualizada",
  "venta": true,
  "credito": false,
  "tieneRetencionFuente": true,  // ✅ NUEVO CAMPO
  "descuentos": 0.0,
  "clienteId": 1,
  "sedeId": 1,
  "trabajadorId": 5,
  "items": [...]
}
```

**Campo `tieneRetencionFuente`**:
- **Tipo**: `boolean`
- **Requerido**: No (si no se envía, mantiene el valor actual)
- **Descripción**: Actualiza si la orden tiene retención de fuente aplicada

#### Response (Sin cambios)

La respuesta sigue siendo `OrdenTablaDTO`, pero ahora incluye el nuevo campo actualizado.

---

## 📊 Resumen de Cambios por Endpoint

| Endpoint | Método | Cambio en Request | Cambio en Response |
|----------|--------|-------------------|-------------------|
| `/api/ordenes/venta` | POST | ✅ Agregar `tieneRetencionFuente` (opcional) | ✅ Incluye `tieneRetencionFuente` |
| `/api/ordenes/venta/{id}` | PUT | ✅ Agregar `tieneRetencionFuente` (opcional) | ✅ Incluye `tieneRetencionFuente` |
| `/api/ordenes/tabla` | GET | ❌ Sin cambios | ✅ Incluye `tieneRetencionFuente` |
| `/api/ordenes/{id}` | GET | ❌ Sin cambios | ✅ Incluye `tieneRetencionFuente` |
| `/api/ordenes/{id}/detalle` | GET | ❌ Sin cambios | ✅ Incluye `tieneRetencionFuente` |
| `/api/ordenes/tabla/{id}` | PUT | ✅ Agregar `tieneRetencionFuente` (opcional) | ✅ Incluye `tieneRetencionFuente` |

---

## 🔧 Cambios en DTOs

### OrdenVentaDTO

**Campo agregado**:
```java
private boolean tieneRetencionFuente = false;
```

### OrdenTablaDTO

**Campo agregado**:
```java
private boolean tieneRetencionFuente;
```

### OrdenActualizarDTO

**Campo agregado**:
```java
private boolean tieneRetencionFuente = false;
```

### OrdenDetalleDTO

**Campo agregado**:
```java
private boolean tieneRetencionFuente;
```

---

## 💻 Ejemplos de Uso en Frontend

### Crear Orden con Retención de Fuente

```javascript
// POST /api/ordenes/venta
const crearOrdenConRetencionFuente = async () => {
  const ordenData = {
    fecha: "2025-01-15",
    obra: "Casa nueva",
    venta: true,
    credito: false,
    tieneRetencionFuente: true,  // ✅ NUEVO CAMPO
    descuentos: 0.0,
    clienteId: 1,
    sedeId: 1,
    trabajadorId: 5,
    items: [
      {
        productoId: 10,
        cantidad: 5,
        precioUnitario: 20000.0
      }
    ]
  };

  const response = await fetch('/api/ordenes/venta', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(ordenData)
  });

  const orden = await response.json();
  console.log('Orden creada con retención:', orden.tieneRetencionFuente);
};
```

### Actualizar Retención de Fuente

```javascript
// PUT /api/ordenes/venta/{id}
const actualizarRetencionFuente = async (ordenId) => {
  const ordenData = {
    fecha: "2025-01-15",
    obra: "Casa nueva",
    venta: true,
    credito: false,
    tieneRetencionFuente: true,  // ✅ ACTUALIZAR CAMPO
    descuentos: 0.0,
    clienteId: 1,
    sedeId: 1,
    trabajadorId: 5,
    items: [...]
  };

  const response = await fetch(`/api/ordenes/venta/${ordenId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(ordenData)
  });

  const orden = await response.json();
  console.log('Retención actualizada:', orden.tieneRetencionFuente);
};
```

### Leer Retención de Fuente

```javascript
// GET /api/ordenes/tabla
const obtenerOrdenes = async () => {
  const response = await fetch('/api/ordenes/tabla');
  const ordenes = await response.json();

  ordenes.forEach(orden => {
    console.log(`Orden ${orden.numero}:`, {
      tieneRetencionFuente: orden.tieneRetencionFuente  // ✅ NUEVO CAMPO
    });
  });
};
```

### Filtrar Órdenes con Retención de Fuente

```javascript
// GET /api/ordenes/tabla
const obtenerOrdenesConRetencion = async () => {
  const response = await fetch('/api/ordenes/tabla');
  const ordenes = await response.json();

  // Filtrar solo órdenes con retención de fuente
  const ordenesConRetencion = ordenes.filter(
    orden => orden.tieneRetencionFuente === true
  );

  console.log('Órdenes con retención:', ordenesConRetencion);
};
```

---

## ⚠️ Notas Importantes

### Compatibilidad hacia atrás

- ✅ **Los endpoints siguen funcionando sin el campo**: Si no envías `tieneRetencionFuente`, se usa el valor por defecto `false`
- ✅ **Las respuestas siempre incluyen el campo**: Aunque sea `false`, el campo siempre estará presente en las respuestas

### Valores por Defecto

- Si no se envía `tieneRetencionFuente` al crear una orden: se establece en `false`
- Si no se envía `tieneRetencionFuente` al actualizar una orden: se mantiene el valor actual

### Validación

- El campo acepta valores `true` o `false`
- No hay validaciones adicionales de negocio (el frontend decide cuándo aplicar retención de fuente)

---

## 📁 Archivos Modificados

### Modelo
- `src/main/java/com/casaglass/casaglass_backend/model/Orden.java`
  - Agregado campo `tieneRetencionFuente` (boolean)

### DTOs
- `src/main/java/com/casaglass/casaglass_backend/dto/OrdenVentaDTO.java`
  - Agregado campo `tieneRetencionFuente` (boolean, default false)
- `src/main/java/com/casaglass/casaglass_backend/dto/OrdenTablaDTO.java`
  - Agregado campo `tieneRetencionFuente` (boolean)
- `src/main/java/com/casaglass/casaglass_backend/dto/OrdenActualizarDTO.java`
  - Agregado campo `tieneRetencionFuente` (boolean, default false)
- `src/main/java/com/casaglass/casaglass_backend/dto/OrdenDetalleDTO.java`
  - Agregado campo `tieneRetencionFuente` (boolean)

### Servicios
- `src/main/java/com/casaglass/casaglass_backend/service/OrdenService.java`
  - Actualizado `crearOrdenVenta()` para establecer `tieneRetencionFuente`
  - Actualizado `crearOrdenVentaConCredito()` para establecer `tieneRetencionFuente`
  - Actualizado `actualizarOrdenVenta()` para actualizar `tieneRetencionFuente`
  - Actualizado `actualizarOrden()` para actualizar `tieneRetencionFuente`
  - Actualizado `convertirAOrdenTablaDTO()` para incluir `tieneRetencionFuente`
  - Actualizado constructor de `OrdenDetalleDTO` para incluir `tieneRetencionFuente`

---

## ✅ Checklist de Implementación en Frontend

- [ ] Ejecutar migración SQL para agregar columna `tiene_retencion_fuente`
- [ ] Actualizar formularios de creación de orden para incluir checkbox/switch de `tieneRetencionFuente`
- [ ] Actualizar formularios de edición de orden para mostrar y permitir editar `tieneRetencionFuente`
- [ ] Actualizar tablas de órdenes para mostrar columna `tieneRetencionFuente` (opcional)
- [ ] Actualizar componentes que muestran detalle de orden para incluir `tieneRetencionFuente`
- [ ] Actualizar tipos/interfaces TypeScript/JavaScript para incluir `tieneRetencionFuente: boolean`
- [ ] Probar creación de orden con `tieneRetencionFuente: true`
- [ ] Probar creación de orden con `tieneRetencionFuente: false`
- [ ] Probar creación de orden sin enviar `tieneRetencionFuente` (debe usar `false` por defecto)
- [ ] Probar actualización de orden cambiando `tieneRetencionFuente`
- [ ] Verificar que las respuestas incluyen el campo `tieneRetencionFuente`

---

## 🎯 Resumen Ejecutivo

**Cambio realizado**: Se agregó el campo booleano `tieneRetencionFuente` a la entidad `Orden` para indicar si la orden tiene retención de fuente aplicada.

**Impacto**:
- ✅ **Backend**: Campo agregado a modelo, DTOs y servicios
- ✅ **Base de datos**: Requiere migración SQL
- ✅ **Frontend**: Debe actualizar formularios y componentes para manejar el nuevo campo
- ✅ **Compatibilidad**: Los endpoints siguen funcionando sin el campo (usa `false` por defecto)

**Próximos pasos**:
1. Ejecutar migración SQL
2. Actualizar formularios en frontend
3. Probar creación y actualización de órdenes con el nuevo campo


