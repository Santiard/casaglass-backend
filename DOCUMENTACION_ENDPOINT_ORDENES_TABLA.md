# 📚 DOCUMENTACIÓN: GET /api/ordenes/tabla

## FECHA: 2025-01-XX
## VERSIÓN: 1.0

---

## 📋 RESUMEN

Endpoint optimizado para obtener el listado de órdenes en formato tabla. Soporta múltiples filtros, paginación y ordenamiento. Retorna solo los campos esenciales para mejorar el rendimiento.

---

## 🔗 INFORMACIÓN BÁSICA

**URL:** `/api/ordenes/tabla`  
**Método:** `GET`  
**Autenticación:** No requerida (según configuración actual)  
**Content-Type:** `application/json`

---

## 📥 PARÁMETROS DE QUERY

Todos los parámetros son **opcionales**. Se pueden combinar múltiples filtros.

### Filtros de Búsqueda

| Parámetro | Tipo | Descripción | Ejemplo |
|-----------|------|-------------|---------|
| `clienteId` | `Long` | Filtrar por ID de cliente | `?clienteId=5` |
| `sedeId` | `Long` | Filtrar por ID de sede | `?sedeId=2` |
| `trabajadorId` | `Long` | Filtrar por ID de trabajador | `?trabajadorId=10` |
| `estado` | `String` | Filtrar por estado: `ACTIVA` o `ANULADA` | `?estado=ACTIVA` |
| `fechaDesde` | `String` (YYYY-MM-DD) | Fecha desde (inclusive) | `?fechaDesde=2025-01-01` |
| `fechaHasta` | `String` (YYYY-MM-DD) | Fecha hasta (inclusive) | `?fechaHasta=2025-01-31` |
| `venta` | `Boolean` | `true` para ventas, `false` para cotizaciones | `?venta=true` |
| `credito` | `Boolean` | `true` para órdenes a crédito | `?credito=true` |
| `facturada` | `Boolean` | `true` para órdenes facturadas, `false` para no facturadas | `?facturada=false` |

### Parámetros de Paginación

| Parámetro | Tipo | Descripción | Default | Ejemplo |
|-----------|------|-------------|---------|---------|
| `page` | `Integer` | Número de página (1-indexed) | Sin paginación | `?page=1` |
| `size` | `Integer` | Tamaño de página | `20` (si se usa paginación) | `?size=50` |
| | | | Máximo: `100` | |

### Parámetros de Ordenamiento

| Parámetro | Tipo | Descripción | Valores Válidos | Default |
|-----------|------|-------------|-----------------|---------|
| `sortBy` | `String` | Campo para ordenar | `fecha`, `numero`, `total` | `fecha` |
| `sortOrder` | `String` | Orden ascendente o descendente | `ASC`, `DESC` | `DESC` |

---

## 📤 RESPUESTAS

### Respuesta con Paginación

Se retorna cuando se proporcionan **ambos** parámetros `page` y `size`.

**Estructura:**

```json
{
  "content": [
    {
      // OrdenTablaDTO (ver estructura abajo)
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "page": 1,
  "size": 20,
  "hasNext": true,
  "hasPrevious": false
}
```

### Respuesta sin Paginación

Se retorna cuando **no** se proporcionan `page` y `size`, o solo se proporciona uno.

**Estructura:**

```json
[
  {
    // OrdenTablaDTO (ver estructura abajo)
  }
]
```

---

## 📦 ESTRUCTURA DE OrdenTablaDTO

```json
{
  "id": 123,
  "numero": 1001,
  "fecha": "2025-01-15",
  "obra": "Construcción Edificio ABC",
  "descripcion": "Orden de venta para proyecto residencial",
  "venta": true,
  "credito": false,
  "tieneRetencionFuente": true,
  "retencionFuente": 42016.81,
  "estado": "ACTIVA",
  "facturada": false,
  "subtotal": 1680672.27,
  "iva": 319327.73,
  "descuentos": 0.0,
  "total": 2000000.0,
  "cliente": {
    "id": 5,
    "nit": "900123456-7",
    "nombre": "Constructora XYZ S.A.S.",
    "correo": "contacto@xyz.com",
    "ciudad": "Bogotá",
    "direccion": "Calle 123 #45-67",
    "telefono": "6012345678"
  },
  "trabajador": {
    "nombre": "Juan Pérez"
  },
  "sede": {
    "nombre": "Sede Principal"
  },
  "creditoDetalle": {
    "id": 10,
    "fechaInicio": "2025-01-15",
    "totalCredito": 3000000.0,
    "totalAbonado": 1000000.0,
    "saldoPendiente": 2000000.0,
    "estado": "ABIERTO"
  },
  "items": [
    {
      "id": 456,
      "producto": {
        "codigo": "VID-001",
        "nombre": "Vidrio Templado 6mm"
      },
      "descripcion": "Vidrio para ventana",
      "cantidad": 10,
      "precioUnitario": 200000.0,
      "totalLinea": 2000000.0
    }
  ]
}
```

### Campos de OrdenTablaDTO

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | `Long` | ID único de la orden |
| `numero` | `Long` | Número de orden |
| `fecha` | `String` (YYYY-MM-DD) | Fecha de la orden |
| `obra` | `String` | Nombre de la obra/proyecto |
| `descripcion` | `String` | Descripción/observaciones adicionales |
| `venta` | `Boolean` | `true` si es venta, `false` si es cotización |
| `credito` | `Boolean` | `true` si es orden a crédito |
| `tieneRetencionFuente` | `Boolean` | Indica si la orden tiene retención de fuente |
| `retencionFuente` | `Double` | Valor monetario de la retención en la fuente |
| `estado` | `String` | Estado: `ACTIVA` o `ANULADA` |
| `facturada` | `Boolean` | `true` si la orden tiene una factura asociada |
| `subtotal` | `Double` | Subtotal de la orden (base imponible SIN IVA) |
| `iva` | `Double` | Valor del IVA calculado |
| `descuentos` | `Double` | Descuentos aplicados |
| `total` | `Double` | Total facturado (subtotal + iva - descuentos) |
| `cliente` | `ClienteTablaDTO` | Información completa del cliente |
| `trabajador` | `TrabajadorTablaDTO` | Información del trabajador |
| `sede` | `SedeTablaDTO` | Información de la sede |
| `creditoDetalle` | `CreditoTablaDTO` | Detalle del crédito (si aplica) |
| `items` | `List<OrdenItemTablaDTO>` | Lista de items de la orden |

---

## 📝 EJEMPLOS DE USO

### Ejemplo 1: Listar todas las órdenes (sin paginación)

**Request:**
```
GET /api/ordenes/tabla
```

**Response (200 OK):**
```json
[
  {
    "id": 123,
    "numero": 1001,
    "fecha": "2025-01-15",
    "obra": "Construcción Edificio ABC",
    "venta": true,
    "credito": false,
    "estado": "ACTIVA",
    "facturada": false,
    "subtotal": 1680672.27,
    "iva": 319327.73,
    "retencionFuente": 42016.81,
    "total": 2000000.0,
    "cliente": { ... },
    "trabajador": { ... },
    "sede": { ... },
    "items": [ ... ]
  }
]
```

---

### Ejemplo 2: Listar órdenes con paginación

**Request:**
```
GET /api/ordenes/tabla?page=1&size=20
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 123,
      "numero": 1001,
      ...
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "page": 1,
  "size": 20,
  "hasNext": true,
  "hasPrevious": false
}
```

---

### Ejemplo 3: Filtrar por cliente y estado

**Request:**
```
GET /api/ordenes/tabla?clienteId=5&estado=ACTIVA
```

**Response (200 OK):**
```json
[
  {
    "id": 123,
    "numero": 1001,
    "estado": "ACTIVA",
    "cliente": {
      "id": 5,
      "nombre": "Constructora XYZ S.A.S.",
      ...
    },
    ...
  }
]
```

---

### Ejemplo 4: Filtrar por rango de fechas con ordenamiento

**Request:**
```
GET /api/ordenes/tabla?fechaDesde=2025-01-01&fechaHasta=2025-01-31&sortBy=total&sortOrder=DESC
```

**Response (200 OK):**
```json
[
  {
    "id": 125,
    "numero": 1003,
    "total": 5000000.0,
    ...
  },
  {
    "id": 123,
    "numero": 1001,
    "total": 2000000.0,
    ...
  }
]
```

---

### Ejemplo 5: Filtrar órdenes no facturadas con paginación

**Request:**
```
GET /api/ordenes/tabla?facturada=false&page=1&size=50&sortBy=fecha&sortOrder=DESC
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 123,
      "numero": 1001,
      "facturada": false,
      ...
    }
  ],
  "totalElements": 45,
  "totalPages": 1,
  "page": 1,
  "size": 50,
  "hasNext": false,
  "hasPrevious": false
}
```

---

### Ejemplo 6: Filtrar órdenes a crédito de un cliente

**Request:**
```
GET /api/ordenes/tabla?clienteId=5&credito=true&estado=ACTIVA
```

**Response (200 OK):**
```json
[
  {
    "id": 124,
    "numero": 1002,
    "credito": true,
    "creditoDetalle": {
      "id": 10,
      "montoTotal": 3000000.0,
      "montoPagado": 1000000.0,
      "montoPendiente": 2000000.0,
      "estado": "ABIERTO",
      ...
    },
    ...
  }
]
```

---

## ⚠️ VALIDACIONES Y ERRORES

### Validación de Fechas

Si `fechaDesde` es posterior a `fechaHasta`:

**Response (400 Bad Request):**
```json
{
  "error": "La fecha desde no puede ser posterior a la fecha hasta"
}
```

### Validación de Estado

Si el estado proporcionado no es válido:

**Response (400 Bad Request):**
```json
{
  "error": "Estado inválido: INVALIDO. Valores válidos: ACTIVA, ANULADA"
}
```

### Validación de Paginación

- Si `page < 1`, se ajusta a `page = 1`
- Si `size < 1`, se ajusta a `size = 20`
- Si `size > 100`, se ajusta a `size = 100`
- Si `page` está fuera de rango, se retorna una lista vacía con `totalElements` correcto

### Validación de Ordenamiento

- Si `sortBy` no es válido, se usa `fecha` por defecto
- Si `sortOrder` no es `ASC` o `DESC`, se usa `DESC` por defecto

---

## 🔢 CÓDIGOS DE ESTADO HTTP

| Código | Descripción |
|--------|-------------|
| `200 OK` | Solicitud exitosa |
| `400 Bad Request` | Parámetros inválidos (fechas, estado, etc.) |
| `500 Internal Server Error` | Error interno del servidor |

---

## 📊 NOTAS IMPORTANTES

### 1. Paginación

- La paginación es **opcional**. Si no se proporcionan `page` y `size`, se retorna la lista completa.
- La paginación es **1-indexed** (la primera página es `page=1`).
- El tamaño máximo de página es **100**.

### 2. Ordenamiento

- Por defecto, las órdenes se ordenan por **fecha DESC** (más recientes primero).
- Los campos válidos para ordenar son: `fecha`, `numero`, `total`.

### 3. Filtros

- Todos los filtros son **opcionales** y se pueden **combinar**.
- Los filtros se aplican con lógica **AND** (todos deben cumplirse).

### 4. Rendimiento

- Este endpoint está optimizado para tablas, retornando solo los campos esenciales.
- Para obtener información completa de una orden, usar `GET /api/ordenes/{id}`.

### 5. Campos Financieros

- `subtotal`: Base imponible **SIN IVA**
- `iva`: IVA calculado sobre el subtotal
- `retencionFuente`: Retención en la fuente (si aplica)
- `total`: Total facturado (subtotal + iva - descuentos)

---

## 🔗 ENDPOINTS RELACIONADOS

- `GET /api/ordenes/{id}` - Obtener detalle completo de una orden
- `POST /api/ordenes/venta` - Crear orden de venta
- `PUT /api/ordenes/venta/{id}` - Actualizar orden de venta
- `GET /api/ordenes/credito?clienteId=X` - Listar órdenes a crédito por cliente

---

## 📞 SOPORTE

Para más información sobre la estructura de datos o problemas con el endpoint, consultar:
- `OrdenTablaDTO.java` - Estructura completa del DTO
- `OrdenService.java` - Lógica de negocio
- `OrdenController.java` - Controlador del endpoint

---

## ✅ CHECKLIST DE USO

- [ ] Verificar que los parámetros de fecha estén en formato `YYYY-MM-DD`
- [ ] Verificar que el estado sea `ACTIVA` o `ANULADA`
- [ ] Si se usa paginación, proporcionar ambos `page` y `size`
- [ ] Verificar que `size` no exceda 100
- [ ] Verificar que `sortBy` sea `fecha`, `numero` o `total`
- [ ] Verificar que `sortOrder` sea `ASC` o `DESC`

---

**Última actualización:** 2025-01-XX  
**Versión del endpoint:** 1.0

