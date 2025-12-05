# 📋 Documentación: Endpoints para Obtener Orden

## 🎯 Endpoints Disponibles

### 1. **GET /api/ordenes/{id}/detalle** ⭐ (Preferido)
**Endpoint optimizado con DTO estructurado**

### 2. **GET /api/ordenes/{id}** (Fallback)
**Endpoint que retorna la entidad Orden completa**

---

## 📊 Endpoint 1: GET /api/ordenes/{id}/detalle

### Descripción
Retorna la orden con estructura completa y optimizada usando `OrdenDetalleDTO`. Este endpoint es **preferido** porque:
- ✅ Estructura clara y predecible
- ✅ Incluye toda la información necesaria
- ✅ Cliente con datos completos
- ✅ Items con información del producto
- ✅ Optimizado para el frontend

### Request
```
GET /api/ordenes/{id}/detalle
```

**Parámetros:**
- `id` (Long, Path Variable) - ID de la orden

### Response (200 OK)

```json
{
  "id": 100,
  "numero": 1001,
  "fecha": "2025-01-15",
  "obra": "Casa nueva",
  "descripcion": "Descripción de la orden",
  "tieneRetencionFuente": false,
  "subtotal": 400000.0,
  "descuentos": 50000.0,
  "total": 350000.0,
  "cliente": {
    "id": 5,
    "nombre": "Juan Pérez",
    "nit": "123456789-0",
    "direccion": "Calle 123 #45-67",
    "telefono": "3001234567"
  },
  "items": [
    {
      "id": 1,
      "producto": {
        "id": 10,
        "codigo": "PROD-001",
        "nombre": "Producto Ejemplo",
        "color": "BLANCO",
        "tipo": "UNID"
      },
      "descripcion": "Descripción del item",
      "cantidad": 2,
      "precioUnitario": 200000.0,
      "totalLinea": 400000.0
    }
  ]
}
```

### Estructura del Response (OrdenDetalleDTO)

#### Campos Principales
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | Long | ID de la orden |
| `numero` | Long | Número de orden |
| `fecha` | String (LocalDate) | Fecha de la orden (formato: YYYY-MM-DD) |
| `obra` | String | Nombre de la obra/proyecto |
| `descripcion` | String | Descripción/observaciones adicionales |
| `tieneRetencionFuente` | Boolean | Indica si la orden tiene retención de fuente |
| `subtotal` | Double | Subtotal de la orden (suma de items) |
| `descuentos` | Double | Descuentos aplicados |
| `total` | Double | Total final (subtotal - descuentos) |

#### Cliente (ClienteDetalleDTO)
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | Long | ID del cliente |
| `nombre` | String | Nombre completo del cliente |
| `nit` | String | NIT del cliente |
| `direccion` | String | Dirección del cliente |
| `telefono` | String | Teléfono del cliente |

#### Items (List<ItemDetalleDTO>)
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | Long | ID del item |
| `producto` | ProductoItemDTO | Información del producto |
| `descripcion` | String | Descripción del item |
| `cantidad` | Integer | Cantidad vendida |
| `precioUnitario` | Double | Precio unitario |
| `totalLinea` | Double | Total de la línea (cantidad × precioUnitario) |

#### Producto (ProductoItemDTO)
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | Long | ID del producto |
| `codigo` | String | Código del producto |
| `nombre` | String | Nombre del producto |
| `color` | String | Color del producto (enum serializado) |
| `tipo` | String | Tipo del producto (enum serializado) |

### Response (404 Not Found)
```json
{
  // Respuesta vacía (sin body)
}
```

---

## 📊 Endpoint 2: GET /api/ordenes/{id}

### Descripción
Retorna la entidad `Orden` completa con todas sus relaciones. Este endpoint es un **fallback** porque:
- ⚠️ Retorna la entidad completa (puede incluir relaciones circulares)
- ⚠️ Puede ser más pesado en términos de datos
- ✅ Útil cuando se necesita toda la información de la orden

### Request
```
GET /api/ordenes/{id}
```

**Parámetros:**
- `id` (Long, Path Variable) - ID de la orden

### Response (200 OK)

La respuesta es la entidad `Orden` completa con todas sus relaciones. Estructura aproximada:

```json
{
  "id": 100,
  "numero": 1001,
  "fecha": "2025-01-15",
  "obra": "Casa nueva",
  "descripcion": "Descripción de la orden",
  "venta": true,
  "credito": false,
  "incluidaEntrega": false,
  "tieneRetencionFuente": false,
  "subtotal": 400000.0,
  "descuentos": 50000.0,
  "total": 350000.0,
  "estado": "ACTIVA",
  "cliente": {
    "id": 5,
    "nombre": "Juan Pérez",
    "nit": "123456789-0",
    "direccion": "Calle 123 #45-67",
    "telefono": "3001234567",
    "correo": "juan@example.com",
    "ciudad": "Bogotá",
    "credito": true,
    // ... otros campos del cliente
  },
  "sede": {
    "id": 1,
    "nombre": "Sede Principal",
    "direccion": "Calle Principal 123",
    "ciudad": "Bogotá"
  },
  "trabajador": {
    "id": 2,
    "nombre": "Carlos Vendedor",
    // ... otros campos del trabajador
  },
  "items": [
    {
      "id": 1,
      "descripcion": "Descripción del item",
      "cantidad": 2,
      "precioUnitario": 200000.0,
      "totalLinea": 400000.0,
      "producto": {
        "id": 10,
        "codigo": "PROD-001",
        "nombre": "Producto Ejemplo",
        "color": "BLANCO",
        "tipo": "UNID",
        "costo": 150000.0,
        "precio1": 200000.0,
        "precio2": 220000.0,
        "precio3": 240000.0,
        "categoria": {
          "id": 1,
          "nombre": "Categoría Ejemplo"
        },
        // ... otros campos del producto
      },
      "orden": {
        // ⚠️ Relación circular (puede causar problemas de serialización)
      }
    }
  ],
  "creditoDetalle": {
    "id": 50,
    "fechaInicio": "2025-01-15",
    "totalCredito": 350000.0,
    "totalAbonado": 0.0,
    "saldoPendiente": 350000.0,
    "estado": "ABIERTO",
    "cliente": {
      // ... datos del cliente
    },
    "orden": {
      // ⚠️ Relación circular
    }
  },
  "factura": {
    "id": 30,
    "numeroFactura": "FAC-2025-001",
    "fecha": "2025-01-15",
    "subtotal": 400000.0,
    "descuentos": 50000.0,
    "iva": 0.0,
    "retencionFuente": 0.0,
    "total": 350000.0,
    "estado": "PENDIENTE",
    "orden": {
      // ⚠️ Relación circular
    }
  }
}
```

### Campos Principales de la Entidad Orden

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | Long | ID de la orden |
| `numero` | Long | Número de orden |
| `fecha` | String (LocalDate) | Fecha de la orden |
| `obra` | String | Nombre de la obra/proyecto |
| `descripcion` | String | Descripción/observaciones |
| `venta` | Boolean | Si es una venta |
| `credito` | Boolean | Si es a crédito |
| `incluidaEntrega` | Boolean | Si está incluida en una entrega |
| `tieneRetencionFuente` | Boolean | Si tiene retención de fuente |
| `subtotal` | Double | Subtotal de la orden |
| `descuentos` | Double | Descuentos aplicados |
| `total` | Double | Total final |
| `estado` | String (Enum) | Estado: `ACTIVA` o `ANULADA` |
| `cliente` | Cliente (Entity) | Cliente completo con todas sus relaciones |
| `sede` | Sede (Entity) | Sede completa |
| `trabajador` | Trabajador (Entity) | Trabajador completo |
| `items` | List<OrdenItem> | Items con producto completo |
| `creditoDetalle` | Credito (Entity) | Detalle del crédito si aplica |
| `factura` | Factura (Entity) | Factura asociada si existe |

### Response (404 Not Found)
```json
{
  // Respuesta vacía (sin body)
}
```

---

## 🔄 Comparación de Endpoints

| Característica | GET /detalle | GET /{id} |
|----------------|--------------|-----------|
| **Estructura** | DTO optimizado | Entidad completa |
| **Tamaño** | Más ligero | Más pesado |
| **Relaciones circulares** | ❌ No | ⚠️ Puede tener |
| **Cliente** | ✅ Datos completos | ✅ Datos completos |
| **Items** | ✅ Con producto básico | ✅ Con producto completo |
| **Crédito** | ❌ No incluido | ✅ Incluido si existe |
| **Factura** | ❌ No incluida | ✅ Incluida si existe |
| **Recomendado** | ✅ Sí (preferido) | ⚠️ Solo como fallback |

---

## 💡 Recomendaciones de Uso

### Usar GET /api/ordenes/{id}/detalle cuando:
- ✅ Necesitas mostrar la orden en un formulario de edición
- ✅ Necesitas los datos del cliente completos
- ✅ Necesitas los items con información del producto
- ✅ Quieres una estructura limpia y predecible
- ✅ No necesitas información de crédito o factura

### Usar GET /api/ordenes/{id} cuando:
- ⚠️ Necesitas información de crédito o factura
- ⚠️ Necesitas todos los campos del producto
- ⚠️ El endpoint `/detalle` no está disponible (fallback)

---

## 📝 Ejemplo de Uso en Frontend

### Estrategia Recomendada (con fallback)

```javascript
async function obtenerOrdenDetalle(ordenId) {
  try {
    // Intentar primero con /detalle (preferido)
    const response = await api.get(`/ordenes/${ordenId}/detalle`);
    return response.data;
  } catch (error) {
    if (error.response?.status === 404) {
      // Orden no encontrada
      throw new Error('Orden no encontrada');
    }
    
    // Si falla /detalle, usar fallback
    console.warn('Endpoint /detalle no disponible, usando fallback');
    try {
      const response = await api.get(`/ordenes/${ordenId}`);
      return response.data;
    } catch (fallbackError) {
      throw new Error('Error al obtener orden');
    }
  }
}
```

### Mapeo de Datos (si usas fallback)

Si necesitas mapear la respuesta del fallback a la estructura de `/detalle`:

```javascript
function mapearOrdenADetalle(orden) {
  return {
    id: orden.id,
    numero: orden.numero,
    fecha: orden.fecha,
    obra: orden.obra,
    descripcion: orden.descripcion,
    tieneRetencionFuente: orden.tieneRetencionFuente,
    subtotal: orden.subtotal,
    descuentos: orden.descuentos,
    total: orden.total,
    cliente: orden.cliente ? {
      id: orden.cliente.id,
      nombre: orden.cliente.nombre,
      nit: orden.cliente.nit,
      direccion: orden.cliente.direccion,
      telefono: orden.cliente.telefono
    } : null,
    items: orden.items?.map(item => ({
      id: item.id,
      producto: item.producto ? {
        id: item.producto.id,
        codigo: item.producto.codigo,
        nombre: item.producto.nombre,
        color: item.producto.color,
        tipo: item.producto.tipo
      } : null,
      descripcion: item.descripcion,
      cantidad: item.cantidad,
      precioUnitario: item.precioUnitario,
      totalLinea: item.totalLinea
    })) || []
  };
}
```

---

## ⚠️ Notas Importantes

1. **Relaciones Circulares**: El endpoint `GET /api/ordenes/{id}` puede tener relaciones circulares (ej: `orden.items[].producto.orden`), lo que puede causar problemas de serialización JSON. El endpoint `/detalle` evita esto usando DTOs.

2. **Campos Faltantes**: El endpoint `/detalle` **NO incluye**:
   - `creditoDetalle` (información del crédito)
   - `factura` (información de la factura)
   - `sede` (información de la sede)
   - `trabajador` (información del trabajador)
   
   Si necesitas estos campos, usa el endpoint `GET /api/ordenes/{id}` o consulta endpoints específicos.

3. **Estado de la Orden**: El endpoint `/detalle` no incluye el campo `estado`. Si lo necesitas, usa el endpoint completo.

4. **Performance**: El endpoint `/detalle` es más eficiente porque retorna solo los datos necesarios, mientras que el endpoint completo puede ser más pesado.

---

## 🔗 Endpoints Relacionados

- `GET /api/ordenes/tabla` - Listado optimizado para tabla
- `GET /api/ordenes/credito?clienteId=X` - Órdenes a crédito por cliente
- `PUT /api/ordenes/tabla/{id}` - Actualizar orden
- `PUT /api/ordenes/{id}/anular` - Anular orden

