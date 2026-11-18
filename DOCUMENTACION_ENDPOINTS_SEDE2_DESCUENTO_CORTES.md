# Documentación de Endpoints y Guía de Implementación (Frontend): Descuento de Inventario por Cortes en Sede 2 (Flujo Sede 1)

## 1. Contexto y Objetivo para el Agente / Desarrollador Frontend

Actualmente, el sistema maneja dos flujos para el descuento de inventario cuando se vende un corte de perfil:
1. **Flujo sin inventario de cortes (`tipoUnidad = "CM"` + `cmBase`)**: Donde no se crean ni reutilizan registros en la tabla `cortes` / `inventario_cortes`, sino que al confirmar la venta se indica en cada ítem si el corte salió de un **perfil entero (`cmBase = 600`)** —lo cual descuenta `1` unidad del perfil base— o de un **pedazo/retazo (`cmBase < 600`)** —lo cual **no descuenta** unidad del inventario—.
2. **Flujo con inventario de cortes (`cortes[]`)**: Donde se enviaba el arreglo `cortes[]` para crear y controlar sobrantes en inventario.

### Cambio requerido en Frontend
A partir de ahora, **la Sede 2 (`sedeId = 2`) también utilizará el mismo método de descuento de unidades de la Sede 1 (`sedeId = 1`)**. Ya no se manejarán los cortes en inventario para la Sede 2 durante la creación/confirmación de órdenes.

> [!IMPORTANT]
> **Referencia clave en el código Frontend actual:**
> **El frontend de la Sede 1 (`sedeId === 1`) a la hora de vender, cotizar, editar, confirmar e imprimir YA se renderiza y construye el payload de la manera correcta para implementar esto.**
> Por lo tanto, antes de crear lógica nueva desde cero, **analiza cómo está implementado y renderizado el flujo de venta/cotización para la Sede 1 en el frontend** (validaciones, selectores de `tipoUnidad` y `cmBase`, ocultamiento del modal de cortes, armado del payload y formato de impresión) y extiende esa misma condición para que aplique tanto a `sedeId === 1` como a `sedeId === 2`.

---

## 2. Regla de Negocio del Descuento de Inventario (Sedes 1 y 2)

En las sedes que usan este flujo (`sedeId = 1` y `sedeId = 2`), cada ítem de la orden (`items[]`) soporta dos propiedades adicionales:
- `tipoUnidad`: `"UNID"` | `"PERFIL"` | `"MT"` | `"CM"`
- `cmBase`: `number | null` (entero mayor a `0`, aplica únicamente cuando `tipoUnidad === "CM"`).

### ¿Cómo descuenta inventario el Backend?

| Tipo de Ítem (`tipoUnidad`) | Valor de `cmBase` | Estado de la Orden (`venta`) | Comportamiento en el Backend |
| :--- | :--- | :--- | :--- |
| `"CM"` (Corte en cm) | `null` | `false` (**Cotización**) | **Permitido.** Guarda la cotización sin tocar el inventario (permite que bodega confirme luego de qué pieza sale). |
| `"CM"` (Corte en cm) | `null` | `true` (**Venta / Confirmación**) | **Rechazado (`400 Bad Request`).** Para confirmar una venta es obligatorio indicar `cmBase`. |
| `"CM"` (Corte en cm) | `600` (Perfil completo) | `true` (**Venta / Confirmación**) | **Descuenta `1.0` unidad** del producto base en el inventario de la sede (sin importar los cm vendidos en la línea). |
| `"CM"` (Corte en cm) | `< 600` (Ej. `300`, `400` - Pedazo/Retazo) | `true` (**Venta / Confirmación**) | **NO descuenta inventario (`0`)**, ya que se tomó un pedazo/retazo cuya barra entera ya fue descontada previamente. |
| `"UNID"`, `"PERFIL"`, `"MT"` | `null` / N/A | `true` (**Venta / Confirmación**) | **Descuento normal:** resta `item.cantidad` del inventario del producto en la sede. |

> [!NOTE]
> Cuando una orden confirmada (`venta = true`) se **anula** o se regresa a cotización (`venta = false`), el backend aplica la operación inversa: si el ítem es `"CM"` con `cmBase == 600`, devuelve `+1.0` unidad al inventario del perfil; si `cmBase < 600`, no suma nada.

---

## 3. Documentación de Endpoints Involucrados

No se crean endpoints nuevos; todos los endpoints actuales de órdenes ya soportan `tipoUnidad` y `cmBase`.

### 3.1. Crear Orden de Venta o Cotización
- **Endpoint:** `POST /api/ordenes/venta`
- **Controller:** `OrdenController.java`
- **DTO de Entrada:** `OrdenVentaDTO.java`

#### Reglas de envío para Sede 1 y Sede 2 (`sedeId: 1` o `sedeId: 2`):
- Enviar `cortes: []` (arreglo vacío o `null`). **No** enviar objetos dentro de `cortes[]`.
- En `items[]`, enviar `productoId` apuntando al **producto/perfil base**, `tipoUnidad` y `cmBase`:

#### Ejemplo Request — Crear Cotización (`venta: false`) con corte pendiente de definir origen:
```json
{
  "fecha": "2026-11-17",
  "obra": "Edificio Central",
  "descripcion": "Cotización ventanería",
  "venta": false,
  "credito": false,
  "incluidaEntrega": false,
  "tieneRetencionFuente": false,
  "tieneRetencionIca": false,
  "tieneRetencionIva": false,
  "porcentajeDescuento": 0.0,
  "clienteId": 12,
  "sedeId": 2,
  "trabajadorId": 4,
  "items": [
    {
      "productoId": 105,
      "nombre": "Perfil Cabezal Corte de 220 CMS",
      "tipoUnidad": "CM",
      "cmBase": null,
      "cantidad": 1,
      "precioUnitario": 45000.0
    }
  ],
  "cortes": []
}
```

#### Ejemplo Request — Crear Venta Directa (`venta: true`) indicando si es completo (`600`) o pedazo (`< 600`):
```json
{
  "fecha": "2026-11-17",
  "obra": "Edificio Central",
  "descripcion": "Venta confirmada",
  "venta": true,
  "credito": false,
  "montoEfectivo": 80000.0,
  "montoTransferencia": 0.0,
  "montoCheque": 0.0,
  "clienteId": 12,
  "sedeId": 2,
  "trabajadorId": 4,
  "items": [
    {
      "productoId": 105,
      "nombre": "Perfil Cabezal Corte de 220 CMS",
      "tipoUnidad": "CM",
      "cmBase": 600,
      "cantidad": 1,
      "precioUnitario": 45000.0
    },
    {
      "productoId": 108,
      "nombre": "Perfil Sillón Corte de 150 CMS",
      "tipoUnidad": "CM",
      "cmBase": 350,
      "cantidad": 1,
      "precioUnitario": 35000.0
    }
  ],
  "cortes": []
}
```
- En este ejemplo:
  - El ítem `105` (`cmBase: 600`) **descuenta `1` unidad** del producto `105` en la Sede 2.
  - El ítem `108` (`cmBase: 350`) **no descuenta unidades** del producto `108` en la Sede 2 (salió de un pedazo de 350 cm).

---

### 3.2. Actualizar / Confirmar Cotización desde la Tabla de Órdenes
- **Endpoint:** `PUT /api/ordenes/tabla/{id}`
- **Controller:** `OrdenController.java`
- **DTO de Entrada:** `OrdenActualizarDTO.java`

#### Comportamiento al confirmar una cotización (`venta: false` → `venta: true`):
1. **Si la cotización tenía un ítem `CM` con `cmBase: null` y se intenta confirmar sin enviar `cmBase`**:
   - El backend responde **`400 Bad Request`**:
     ```json
     {
       "error": "No se puede confirmar venta: el item \"...\" tipo CM no tiene cmBase (origen del corte) completado. Por favor actualiza el item con el cmBase antes de confirmar.",
       "tipo": "VALIDACION",
       "codigo": "VALIDACION_FALLIDA"
     }
     ```
2. **Confirmación correcta actualizando `cmBase`**:
   ```json
   {
     "id": 501,
     "fecha": "2026-11-17",
     "obra": "Edificio Central",
     "descripcion": "Cotización confirmada",
     "venta": true,
     "credito": false,
     "clienteId": 12,
     "trabajadorId": 4,
     "sedeId": 2,
     "items": [
       {
         "id": 910,
         "productoId": 105,
         "nombre": "Perfil Cabezal Corte de 220 CMS",
         "tipoUnidad": "CM",
         "cmBase": 600,
         "cantidad": 1,
         "precioUnitario": 45000.0,
         "totalLinea": 45000.0,
         "eliminar": false
       }
     ]
   }
   ```

---

### 3.3. Actualizar Orden de Venta (Modal de Edición Completo)
- **Endpoint:** `PUT /api/ordenes/venta/{id}`
- **Controller:** `OrdenController.java`
- **DTO de Entrada:** `OrdenVentaDTO.java`
- **Comportamiento:**
  - Si la orden ya era venta (`venta = true`), restaura primero el inventario previo según las reglas de `cmBase` y luego aplica el descuento de los nuevos ítems.
  - Si era cotización (`venta = false`) y pasa a `venta = true`, valida que todos los ítems `CM` traigan `cmBase` y aplica el descuento (`cmBase == 600` descuenta `1`, `< 600` no descuenta).

---

### 3.4. Consultas de Tabla y Detalle (Lectura para UI e Impresión)
- **Endpoints:**
  - `GET /api/ordenes/tabla` (y variantes por sede/cliente/trabajador) → Retorna `OrdenTablaDTO`
  - `GET /api/ordenes/{id}/detalle` → Retorna `OrdenDetalleDTO`
- **Contrato de salida en `items[]`:**
  Cada ítem dentro de `items[]` ya devuelve explícitamente:
  ```json
  {
    "id": 910,
    "productoId": 105,
    "nombre": "Perfil Cabezal Corte de 220 CMS",
    "tipoUnidad": "CM",
    "cmBase": 600,
    "cantidad": 1.0,
    "precioUnitario": 45000.0,
    "totalLinea": 45000.0,
    "producto": {
      "codigo": "392",
      "nombre": "Perfil Cabezal Corte de 220 CMS",
      "color": "NEGRO"
    }
  }
  ```
  *(Internamente el backend guarda `##META:TIPO=CM;CMBASE=600` en la columna `nombre`, pero lo limpia automáticamente al serializar los DTOs de respuesta para que `nombre`, `tipoUnidad` y `cmBase` lleguen separados y limpios al frontend).*

---

## 4. Guía Paso a Paso para el Agente de Frontend

1. **Inspeccionar la implementación actual de la Sede 1:**
   - Buscar en el frontend dónde se evalúa `sedeId === 1` (por ejemplo, helpers como `esSedeSinControlCortes(sedeId)` o condicionales en el formulario de crear/editar orden de venta, tabla de órdenes e impresión).
   - **Nota explícita:** La interfaz de la Sede 1 a la hora de vender **ya se renderiza de la manera correcta** (selectores de `tipoUnidad`, captura de `cmBase` para saber si es perfil entero `600` o pedazo `< 600`, armado de `items[]` sin `cortes[]`, validación al confirmar cotización y visualización en impresión).
2. **Habilitar la Sede 2 en la misma regla:**
   - Actualizar el helper o condición centralizada en el frontend para que incluya la Sede 2:
     ```ts
     // Antes:
     const esSedeSinControlCortes = (sedeId?: number | string) => Number(sedeId) === 1;

     // Ahora (Sede 1 y Sede 2 usan el mismo flujo de descuento y renderizado):
     const SEDES_SIN_CONTROL_CORTES = [1, 2];
     const esSedeSinControlCortes = (sedeId?: number | string) =>
       SEDES_SIN_CONTROL_CORTES.includes(Number(sedeId));
     ```
3. **Verificar todos los puntos donde impacta en Sede 2:**
   - **Creación de Orden / Cotización:** Al seleccionar `sedeId = 2`, debe renderizar exactamente los mismos controles de línea (`tipoUnidad`: `UNID | PERFIL | MT | CM` y campo `cmBase` cuando es `CM`) que hoy muestra en Sede 1, sin abrir ni exigir el modal de inventario de cortes sobrantes.
   - **Armado del Payload (`POST /api/ordenes/venta`, `PUT /api/ordenes/venta/{id}`, `PUT /api/ordenes/tabla/{id}`):** Enviar `tipoUnidad` y `cmBase` en cada ítem y `cortes: []`.
   - **Confirmación de Cotizaciones en Tabla:** Si una cotización de Sede 2 tiene ítems `CM` sin `cmBase`, pedir/validar que se complete `cmBase` antes de cambiar `venta` a `true` (igual que en Sede 1).
   - **Impresión / Detalle de Orden:** Mostrar la columna `TIPO` con `CM(600)` o `CM(300)` usando `item.tipoUnidad` e `item.cmBase` tal como ya funciona en Sede 1.
