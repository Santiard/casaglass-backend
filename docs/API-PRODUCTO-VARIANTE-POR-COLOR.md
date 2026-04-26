# GET `/api/productos/variante` — producto por código y color (sin id)

Especificación alineada con el backend Spring. Sirve **OrdenModal** (y otras pantallas) para rehidratar un producto a partir de los mismos datos que muestra la línea de la orden, **sin** depender de `id` si no está guardado aún o cambió de sesión.

## Request

`GET /api/productos/variante`

| Parámetro | Obligatorio | Descripción |
|-----------|-------------|-------------|
| `codigo` | Sí (no vacío) | Código del ítem, **igual** que en `producto.codigo` en BD. |
| `color` | Sí (no vacío) | `ColorProducto` en mayúsculas: `MATE`, `BLANCO`, `NEGRO`, `BRONCE`, `NA`. |
| `nombre` | Sí (no vacío) | Nombre de la línea, debe coincidir con `producto.nombre` en BD (trim, ignorando mayúsculas), **texto completo** — no búsqueda parcial, para no confundir entero y corte con el mismo `codigo`+`color` (p. ej. "X" vs "X CORTE de 50cm"). |

Ejemplo:  
`GET /api/productos/variante?codigo=392&color=MATE&nombre=...`

## Respuestas

| HTTP | Cuerpo |
|------|--------|
| **200** | Objeto `Producto` (JSON) como en `GET /api/productos/{id}`: `id`, `codigo`, `nombre`, `color`, `categoria`, `precio1/2/3`, `tipo`, etc. Incluye subclases (p. ej. `Corte` con `largoCm` si aplica) según el tipo almacenado. |
| **400** | JSON `{ "error", "message" }` — faltan `codigo` / `color` / `nombre` o `color` no es un valor del enum. Códigos: `PARAMETROS_REQUERIDOS`, `COLOR_INVALIDO`. |
| **404** | Sin cuerpo o vacío: no hay fila con ese `codigo`+`color`+`nombre` exacto (misma lógica que arriba). |
| **409** | JSON: `error` = `VARIAS_VARIANTES` (datos duplicados: más de un registro con mismo `codigo`+`color`+`nombre` en catálogo). |

## Notas de implementación (backend)

- Consulta: `ProductoRepository.findByCodigoAndColor(codigo, color)` (match **exacto** de código, no `LIKE`), luego se filtra por `nombre` con **igualdad** a la línea (ignore case, trim), no por `contains`.
- Incluye filas de la jerarquía JPA (por ejemplo `Corte` extiende `Producto`).

## Front

- Constante de colores alineable con el enum: `MATE`, `BLANCO`, `NEGRO`, `BRONCE`, `NA`.
- Ruta: prefijo `api` global del cliente, p. ej. `GET ${baseUrl}/api/productos/variante?...`
