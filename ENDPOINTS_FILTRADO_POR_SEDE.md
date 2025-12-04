# 📋 Especificación de Endpoints con Filtrado por Sede

Este documento describe todos los endpoints que aceptan el parámetro `sedeId` opcional para filtrar resultados por sede.

---

## 📌 Resumen General

Todos los endpoints descritos en este documento:
- ✅ Aceptan `sedeId` como **query parameter opcional**
- ✅ Si `sedeId` es `null` o no se envía: retornan **todos los registros** (comportamiento para admins)
- ✅ Si `sedeId` se envía: filtran solo los registros relacionados a esa sede
- ✅ Mantienen la **misma estructura de respuesta** que los endpoints actuales
- ✅ **No rompen compatibilidad**: los endpoints sin `sedeId` siguen funcionando igual

---

## 1. 📦 Órdenes

### Endpoint
```
GET /api/ordenes/tabla?sedeId={sedeId}
```

### Query Parameters
| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `sedeId` | `Long` | No | ID de la sede. Si se envía, retorna solo órdenes de esa sede. Si es `null` o no se envía, retorna todas (para admins). |

### Lógica de Filtrado
- Filtra por `orden.sede.id === sedeId` o `orden.sedeId === sedeId`
- Si `sedeId` es `null` o no se envía: retorna todas las órdenes

### Ejemplo de Uso
```http
GET /api/ordenes/tabla?sedeId=1
GET /api/ordenes/tabla  (retorna todas)
```

### Respuesta
**Tipo**: `List<OrdenTablaDTO>`

**Estructura** (misma que el endpoint actual):
```json
[
  {
    "id": 100,
    "numero": 1001,
    "fecha": "2025-01-15",
    "obra": "Casa nueva",
    "subtotal": 100000.0,
    "descuentos": 0.0,
    "total": 100000.0,
    "venta": true,
    "credito": false,
    "estado": "ACTIVA",
    "cliente": {
      "id": 1,
      "nit": "1234567-8",
      "nombre": "Juan Pérez",
      "correo": "juan@example.com",
      "ciudad": "Bogotá",
      "direccion": "Calle 123",
      "telefono": "3001234567"
    },
    "sede": {
      "id": 1,
      "nombre": "Insula"
    },
    "trabajador": {
      "id": 5,
      "nombre": "Carlos Rodríguez"
    },
    "items": [...]
  }
]
```

### Códigos de Estado
- `200 OK`: Lista obtenida exitosamente
- `400 Bad Request`: Si `sedeId` no es un número válido

---

## 2. 🚚 Traslados/Movimientos

### Endpoint
```
GET /api/traslados-movimientos?sedeId={sedeId}
```

### Query Parameters
| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `sedeId` | `Long` | No | ID de la sede. Si se envía, retorna traslados donde el usuario esté en `sedeOrigen` o `sedeDestino`. Si es `null` o no se envía, retorna todos (para admins). |

### Lógica de Filtrado
- Filtra por `traslado.sedeOrigen.id === sedeId OR traslado.sedeDestino.id === sedeId`
- Incluye registros donde la sede aparece como origen **o** destino
- Si `sedeId` es `null` o no se envía: retorna todos los traslados

### Ejemplo de Uso
```http
GET /api/traslados-movimientos?sedeId=2
GET /api/traslados-movimientos  (retorna todos)
```

### Respuesta
**Tipo**: `List<TrasladoMovimientoDTO>`

**Estructura** (misma que el endpoint actual):
```json
[
  {
    "id": 50,
    "fecha": "2025-01-15",
    "sedeOrigen": {
      "id": 1,
      "nombre": "Insula"
    },
    "sedeDestino": {
      "id": 2,
      "nombre": "Centro"
    },
    "trabajadorConfirmacion": {
      "id": 5,
      "nombre": "Carlos Rodríguez"
    },
    "fechaConfirmacion": "2025-01-15",
    "detalles": [
      {
        "id": 100,
        "cantidad": 10,
        "producto": {
          "id": 25,
          "nombre": "Producto ejemplo",
          "codigo": "PROD001",
          "categoria": "Categoría ejemplo"
        }
      }
    ]
  }
]
```

### Códigos de Estado
- `200 OK`: Lista obtenida exitosamente
- `400 Bad Request`: Si `sedeId` no es un número válido

---

## 3. 📥 Ingresos

### Endpoint
```
GET /api/ingresos?sedeId={sedeId}
```

### Query Parameters
| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `sedeId` | `Long` | No | ID de la sede. Si se envía, retorna solo ingresos de esa sede. Si es `null` o no se envía, retorna todos (para admins). |

### Lógica de Filtrado
- **Nota**: Los ingresos actualmente no tienen un campo `sede` directo en el modelo.
- Por ahora, este endpoint retorna todos los ingresos independientemente del `sedeId` (todos se procesan en la sede principal).
- **TODO**: Si en el futuro se agrega un campo `sede` a `Ingreso`, este método deberá filtrar por `ingreso.sede.id === sedeId` o `ingreso.sedeId === sedeId`.
- Si `sedeId` es `null` o no se envía: retorna todos los ingresos

### Ejemplo de Uso
```http
GET /api/ingresos?sedeId=1
GET /api/ingresos  (retorna todos)
```

### Respuesta
**Tipo**: `List<Ingreso>`

**Estructura** (misma que el endpoint actual):
```json
[
  {
    "id": 50,
    "fecha": "2025-01-15",
    "numeroFactura": "FAC-001",
    "observaciones": "Ingreso de prueba",
    "totalCosto": 50000.0,
    "procesado": true,
    "proveedor": {
      "id": 10,
      "nombre": "Proveedor ejemplo",
      "nit": "9876543-2"
    },
    "detalles": [
      {
        "id": 100,
        "cantidad": 10,
        "costoUnitario": 5000.0,
        "producto": {
          "id": 25,
          "nombre": "Producto ejemplo",
          "codigo": "PROD001"
        }
      }
    ]
  }
]
```

### Códigos de Estado
- `200 OK`: Lista obtenida exitosamente
- `400 Bad Request`: Si `sedeId` no es un número válido

---

## 4. 💰 Reembolsos de Ingreso

### Endpoint
```
GET /api/reembolsos-ingreso?sedeId={sedeId}
```

### Query Parameters
| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `sedeId` | `Long` | No | ID de la sede. Si se envía, retorna solo reembolsos de ingresos de esa sede. Si es `null` o no se envía, retorna todos (para admins). |

### Lógica de Filtrado
- Filtra por la sede del ingreso relacionado: `reembolso.ingresoOriginal.sede.id === sedeId` o `reembolso.ingresoOriginal.sedeId === sedeId`
- **Nota**: Los ingresos actualmente no tienen un campo `sede` directo. Por ahora, este endpoint retorna todos los reembolsos.
- **TODO**: Si en el futuro se agrega un campo `sede` a `Ingreso`, este método deberá filtrar correctamente.
- Si `sedeId` es `null` o no se envía: retorna todos los reembolsos

### Ejemplo de Uso
```http
GET /api/reembolsos-ingreso?sedeId=1
GET /api/reembolsos-ingreso  (retorna todos)
```

### Respuesta
**Tipo**: `List<ReembolsoIngresoResponseDTO>`

**Estructura** (misma que el endpoint actual):
```json
[
  {
    "id": 50,
    "fecha": "2025-01-15",
    "estado": "PENDIENTE",
    "total": 50000.0,
    "ingresoOriginal": {
      "id": 100,
      "numeroFactura": "FAC-001",
      "fecha": "2025-01-10"
    },
    "proveedor": {
      "id": 10,
      "nombre": "Proveedor ejemplo"
    },
    "detalles": [
      {
        "id": 100,
        "cantidad": 10,
        "costoUnitario": 5000.0,
        "producto": {
          "id": 25,
          "nombre": "Producto ejemplo"
        }
      }
    ]
  }
]
```

### Códigos de Estado
- `200 OK`: Lista obtenida exitosamente
- `400 Bad Request`: Si `sedeId` no es un número válido
- `500 Internal Server Error`: Error interno del servidor

---

## 5. 🛒 Reembolsos de Venta

### Endpoint
```
GET /api/reembolsos-venta?sedeId={sedeId}
```

### Query Parameters
| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `sedeId` | `Long` | No | ID de la sede. Si se envía, retorna solo reembolsos de ventas de esa sede. Si es `null` o no se envía, retorna todos (para admins). |

### Lógica de Filtrado
- Filtra por la sede de la orden relacionada: `reembolso.ordenOriginal.sede.id === sedeId` o `reembolso.ordenOriginal.sedeId === sedeId`
- Si `sedeId` es `null` o no se envía: retorna todos los reembolsos

### Ejemplo de Uso
```http
GET /api/reembolsos-venta?sedeId=3
GET /api/reembolsos-venta  (retorna todos)
```

### Respuesta
**Tipo**: `List<ReembolsoVentaResponseDTO>`

**Estructura** (misma que el endpoint actual):
```json
[
  {
    "id": 50,
    "fecha": "2025-01-15",
    "estado": "PENDIENTE",
    "total": 100000.0,
    "ordenOriginal": {
      "id": 100,
      "numero": 1001,
      "fecha": "2025-01-10",
      "sede": {
        "id": 3,
        "nombre": "Patios"
      }
    },
    "cliente": {
      "id": 1,
      "nombre": "Juan Pérez"
    },
    "detalles": [
      {
        "id": 100,
        "cantidad": 5,
        "precioUnitario": 20000.0,
        "producto": {
          "id": 25,
          "nombre": "Producto ejemplo"
        }
      }
    ]
  }
]
```

### Códigos de Estado
- `200 OK`: Lista obtenida exitosamente
- `400 Bad Request`: Si `sedeId` no es un número válido
- `500 Internal Server Error`: Error interno del servidor

---

## 6. 🧾 Facturas

### Endpoint
```
GET /api/facturas/tabla?sedeId={sedeId}
```

### Query Parameters
| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `sedeId` | `Long` | No | ID de la sede. Si se envía, retorna solo facturas de órdenes de esa sede. Si es `null` o no se envía, retorna todas (para admins). |

### Lógica de Filtrado
- Filtra por la sede de la orden relacionada: `factura.orden.sede.id === sedeId` o `factura.orden.sedeId === sedeId`
- Si `sedeId` es `null` o no se envía: retorna todas las facturas

### Ejemplo de Uso
```http
GET /api/facturas/tabla?sedeId=1
GET /api/facturas/tabla  (retorna todas)
```

### Respuesta
**Tipo**: `List<FacturaTablaDTO>`

**Estructura** (misma que el endpoint actual):
```json
[
  {
    "id": 50,
    "numeroFactura": "FAC-001",
    "fecha": "2025-01-15",
    "obra": "Casa nueva",
    "subtotal": 135000.0,
    "descuentos": 50000.0,
    "iva": 0.0,
    "retencionFuente": 0.0,
    "total": 85000.0,
    "formaPago": "EFECTIVO",
    "estado": "PENDIENTE",
    "fechaPago": null,
    "observaciones": "Factura con descuento",
    "cliente": {
      "nombre": "Juan Pérez",
      "nit": "1234567-8"
    },
    "orden": {
      "numero": 1001
    }
  }
]
```

### Códigos de Estado
- `200 OK`: Lista obtenida exitosamente
- `400 Bad Request`: Si `sedeId` no es un número válido

---

## 7. 💵 Entregas de Dinero

### Endpoint
```
GET /api/entregas-dinero?sedeId={sedeId}
```

### Query Parameters
| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `sedeId` | `Long` | No | ID de la sede. Si se envía, retorna solo entregas de esa sede. Si es `null` o no se envía, retorna todas (para admins). |

### Lógica de Filtrado
- **Ya implementado**: Este endpoint ya tiene soporte para `sedeId`
- Filtra por `entrega.sede.id === sedeId` o `entrega.sedeId === sedeId`
- Si `sedeId` es `null` o no se envía: retorna todas las entregas

### Ejemplo de Uso
```http
GET /api/entregas-dinero?sedeId=1
GET /api/entregas-dinero  (retorna todas)
```

### Respuesta
**Tipo**: `List<EntregaDineroResponseDTO>`

**Estructura** (misma que el endpoint actual):
```json
[
  {
    "id": 50,
    "fechaEntrega": "2025-01-15",
    "monto": 50000.0,
    "estado": "CONFIRMADA",
    "sede": {
      "id": 1,
      "nombre": "Insula"
    },
    "empleado": {
      "id": 5,
      "nombre": "Carlos Rodríguez"
    },
    "ordenes": [...]
  }
]
```

### Códigos de Estado
- `200 OK`: Lista obtenida exitosamente
- `400 Bad Request`: Si `sedeId` no es un número válido

---

## 📝 Notas Importantes

### Validación de `sedeId`
- Si `sedeId` se envía pero no es un número válido, el endpoint retorna `400 Bad Request`
- Si `sedeId` es un número válido pero no existe ninguna sede con ese ID, el endpoint retorna una lista vacía `[]`

### Compatibilidad
- Todos los endpoints mantienen **compatibilidad hacia atrás**
- Si no se envía `sedeId`, el comportamiento es idéntico al endpoint original
- No se requiere ningún cambio en el frontend si no se desea usar el filtrado por sede

### Implementación Pendiente
- **Ingresos**: Actualmente no tienen campo `sede` en el modelo. Si se agrega en el futuro, el filtrado deberá ser actualizado.
- **Reembolsos de Ingreso**: Dependen de que `Ingreso` tenga campo `sede`. Por ahora retornan todos los reembolsos.

---

## 🔧 Ubicación en el Código

### Controllers
- `OrdenController.java` - Línea 379-389
- `TrasladoMovimientoController.java` - Línea 34-47
- `IngresoController.java` - Línea 24-30
- `ReembolsoIngresoController.java` - Línea 23-35
- `ReembolsoVentaController.java` - Línea 23-35
- `FacturaController.java` - Línea 76-83
- `EntregaDineroController.java` - Línea 50-80 (ya implementado)

### Services
- `OrdenService.java` - Método `listarPorSedeParaTabla()` (línea 760-764)
- `TrasladoMovimientoService.java` - Método `obtenerMovimientosPorSede()` (línea 52-57)
- `IngresoService.java` - Método `listarIngresosPorSede()` (nuevo)
- `ReembolsoIngresoService.java` - Método `listarReembolsosPorSede()` (nuevo)
- `ReembolsoVentaService.java` - Método `listarReembolsosPorSede()` (nuevo)
- `FacturaService.java` - Método `listarParaTablaPorSede()` (nuevo)
- `EntregaDineroService.java` - Ya implementado

---

## ✅ Checklist de Implementación

- [x] GET /api/ordenes/tabla - Ya acepta `sedeId`
- [x] GET /api/traslados-movimientos - Agregado `sedeId` opcional
- [x] GET /api/ingresos - Agregado `sedeId` opcional (nota: Ingreso no tiene campo sede)
- [x] GET /api/reembolsos-ingreso - Agregado `sedeId` opcional (nota: depende de Ingreso.sede)
- [x] GET /api/reembolsos-venta - Agregado `sedeId` opcional
- [x] GET /api/facturas/tabla - Agregado `sedeId` opcional
- [x] GET /api/entregas-dinero - Ya implementado
- [x] Documentación MD creada

