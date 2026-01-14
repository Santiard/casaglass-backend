# Documentación Endpoints Cliente Especial

Estos endpoints están reservados para el cliente especial (ID 499 - JAIRO JAVIER VELANDIA). Permiten consultar sus créditos con filtros avanzados y marcar de forma masiva cuáles ya fueron pagados presencialmente.

## 1. GET /api/creditos/cliente-especial

### Propósito
Lista los créditos del cliente especial con filtros opcionales por sede, estado y rango de fechas. El backend aplica el ordenamiento solicitado y puede devolver respuesta paginada o una lista completa de `CreditoResponseDTO`.

### Parámetros de consulta (opcionales)

| Parámetro  | Tipo    | Descripción | Valores permitidos / default |
|-----------|---------|-------------|-------------------------------|
| `sedeId`  | Long    | Filtra por la sede en la que se originó la orden a crédito | ID de una sede existente |
| `estado`  | String  | Estado del crédito | `ABIERTO`, `CERRADO`, `VENCIDO`, `ANULADO` |
| `fechaDesde` | String (YYYY-MM-DD) | Fecha mínima de creación del crédito (inclusive) | Debe ser <= `fechaHasta` si ambos se envían |
| `fechaHasta` | String (YYYY-MM-DD) | Fecha máxima de creación del crédito (inclusive) | Debe ser >= `fechaDesde` si ambos se envían |
| `page`    | Integer | Número de página (1-index) para paginación manual | Default: sin paginar |
| `size`    | Integer | Tamaño de página | Default 50, máximo 200 |
| `sortBy`  | String  | Campo de ordenamiento | `fecha` (default), `montoTotal`, `saldoPendiente` |
| `sortOrder` | String | Dirección de ordenamiento | `ASC` o `DESC` (default) |

### Respuesta
Dependiendo de si se envían `page` y `size`:

- **Sin paginación:** arreglo de `CreditoResponseDTO`.
- **Con paginación:** objeto `PageResponse` con `content`, `totalElements`, `page`, `size`.

`CreditoResponseDTO` incluye:

- `id`, `fechaInicio`, `totalCredito`, `estado`, `observaciones`.
- `cliente`: `{ id, nombre, nit, telefono, correo }` (estructura `ClienteSimpleDTO`).
- `orden`: `{ id, numero, fecha, obra, total, subtotal, iva, descuentos, tieneRetencionFuente, sede }`.
- `abonos`: lista resumida con `{ id, fecha, total, observaciones }`.

> 🔕 **Campos que el frontend ignora en esta pantalla**: aunque el backend sigue enviando `totalAbonado`, `saldoPendiente`, `retencionFuente`, `numeroFactura` y `fechaCierre`, la tabla del cliente especial ya no muestra esas columnas.

### Ejemplo de respuesta (sin paginación)

```json
[
  {
    "id": 321,
    "cliente": {
      "id": 499,
      "nombre": "JAIRO JAVIER VELANDIA"
    },
    "orden": {
      "id": 812,
      "numero": 12045,
      "fecha": "2026-01-05",
      "obra": "Centro",
      "total": 450000.0,
      "subtotal": 378151.26,
      "iva": 71848.74,
      "tieneRetencionFuente": true
    },
    "fechaInicio": "2026-01-05",
    "totalCredito": 450000.0,
    "estado": "ABIERTO",
    "abonos": []
  }
]
```

Si se requiere exponer nuevamente alguno de los campos omitidos, basta con leerlos del mismo DTO sin tocar el endpoint.

### Validaciones y errores comunes

- `400 Bad Request`: fechas invertidas, estado inválido o parámetros con formato incorrecto.
- `500 Internal Server Error`: problemas inesperados durante la consulta.

## 2. POST /api/creditos/cliente-especial/marcar-pagados

### Propósito
Permite cerrar varios créditos del cliente especial en un solo paso cuando se confirma fuera del sistema que ya pagó. No crea registros de abonos; simplemente actualiza los créditos:

- `saldoPendiente` se establece en 0.0.
- `totalAbonado` se iguala al total del crédito menos la retención de fuente (si aplica).
- `estado` pasa a `CERRADO` y se fija `fechaCierre` con la fecha actual.

### Cuerpo de la solicitud

```json
{
  "creditoIds": [123, 124, 130],
  "ejecutadoPor": "Laura - Cartera",
  "observaciones": "Pagos confirmados presencialmente"
}
```

> `ejecutadoPor` y `observaciones` son opcionales. Si no se envían, el backend guarda "SISTEMA" como responsable y deja observaciones en blanco.

### Reglas de negocio y validaciones

1. `creditoIds` es obligatorio y debe contener al menos un ID.
2. Cada crédito debe existir y pertenecer al cliente especial (ID 499). Si uno falla, el proceso completo se aborta con `400`.
3. No se puede marcar un crédito que ya esté en estado `CERRADO`. En ese caso se responde `409 Conflict`.
4. Si la orden asociada tenía retención en la fuente, el servicio respeta ese valor al calcular el total abonado final.
5. Después del cierre exitoso, el backend crea automáticamente un registro histórico (`entregaEspecial`) con los créditos involucrados.

### Respuesta de éxito

```json
{
  "mensaje": "Créditos marcados como pagados exitosamente",
  "creditosPagados": 3,
  "entregaEspecialId": 15,
  "registro": {
    "id": 15,
    "fechaRegistro": "2026-01-13T09:55:21.234",
    "ejecutadoPor": "Laura - Cartera",
    "totalCreditos": 3,
    "totalMontoCredito": 1250000.0,
    "totalRetencion": 45693.28,
    "detalles": [
      {
        "creditoId": 321,
        "ordenId": 812,
        "numeroOrden": 12045,
        "obra": "Centro",
        "fechaCredito": "2025-12-01",
        "totalCredito": 450000.0,
        "saldoAnterior": 218457.14,
        "retencionFuente": 31542.86
      }
    ]
  }
}
```

### Respuestas de error

| Código | Caso | Ejemplo |
|--------|------|---------|
| 400 Bad Request | Falta `creditoIds`, IDs vacíos, crédito que no pertenece al cliente especial | `{ "error": "El crédito con ID 125 no pertenece al cliente especial", "tipo": "VALIDACION" }` |
| 409 Conflict | Se intenta cerrar un crédito que ya estaba cerrado | `{ "error": "El crédito con ID 130 ya está cerrado", "tipo": "CONFLICTO_ESTADO" }` |
| 500 Internal Server Error | Error inesperado en la actualización | `{ "error": "Error interno del servidor: ...", "tipo": "SERVIDOR" }` |

### Flujo recomendado para el equipo

1. Obtener los créditos pendientes del cliente especial mediante el GET con filtros (por sede o fecha si es necesario).
2. En la interfaz, permitir seleccionar los IDs que ya fueron pagados presencialmente.
3. Enviar esos IDs al POST anterior. Confirmar el mensaje de éxito.
4. Refrescar el estado de cuenta para verificar que ya no tienen saldo pendiente.
5. Revisar el nuevo historial de entregas especiales para auditar qué créditos se cerraron.

## 3. GET /api/creditos/cliente-especial/entregas

Listado histórico de lotes cerrados para el cliente especial. Útil para auditorías y para conocer cuándo se cerró cada orden.

### Parámetros

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `desde` | Date (YYYY-MM-DD) | Fecha inicial opcional para filtrar `fechaRegistro` |
| `hasta` | Date (YYYY-MM-DD) | Fecha final opcional |

### Respuesta

```json
[
  {
    "id": 15,
    "fechaRegistro": "2026-01-13T09:55:21.234",
    "ejecutadoPor": "Laura - Cartera",
    "totalCreditos": 3,
    "totalMontoCredito": 1250000.0,
    "totalRetencion": 45693.28,
    "observaciones": "Pagos confirmados presencialmente"
  }
]
```

> Este listado omite los detalles por performance. Para ver el desglose de créditos usa el endpoint siguiente.

## 4. GET /api/creditos/cliente-especial/entregas/{id}

Devuelve un registro completo con todos sus créditos. La respuesta es idéntica al objeto `registro` que regresa el POST.


Esta documentación cubre todo lo necesario para exponer los dos endpoints a otros equipos (frontend, operaciones o soporte) sin revisar el código fuente.
