# Cambios Implementados: Flujo Sede 1 Sin Inventario de Cortes

## Objetivo

Aplicar un flujo especial para la sede principal (sede ID 1) sin crear nuevas entidades:

- En sede 1 no se crea ni reutiliza inventario de cortes.
- Se soporta tipo de linea `CM(base)` para registrar origen del corte.
- Se mantiene el flujo actual de cortes para sedes distintas de 1.

## Regla central aplicada

- Si `sedeId == 1`: flujo sin control de cortes.
- Si `sedeId != 1`: flujo actual (cortes) sin cambios funcionales.

## Endpoints impactados (mismos endpoints existentes)

- `POST /api/ordenes/venta`
- `PUT /api/ordenes/venta/{id}`
- `PUT /api/ordenes/tabla/{id}`
- `GET /api/ordenes/tabla`
- `GET /api/ordenes/{id}/detalle`

No se agrego un endpoint nuevo de ordenes para evitar duplicar logica.

## Cambios de contrato en payload (items)

Se agregaron campos opcionales en items:

- `tipoUnidad`: `UNID | PERFIL | MT | CM`
- `cmBase`: entero opcional (solo aplica cuando `tipoUnidad = CM`)

DTOs actualizados:

- `OrdenVentaDTO.OrdenItemVentaDTO`
- `OrdenActualizarDTO.OrdenItemActualizarDTO`

## Regla de inventario implementada para sede 1

Para items con `tipoUnidad = CM`:

- **`cmBase == 600` Y `venta=true`**: Descuenta 1 UNIDAD del inventario (corte de pieza entero)
- **`cmBase < 600` Y `venta=true`**: NO descuenta inventario (es fragmento de pieza)
- **`cmBase` vacío Y `venta=false`**: ✅ PERMITIDO (cotización sin confirmación de bodega)
- **`cmBase` vacío Y `venta=true`**: ❌ RECHAZA 400 (venta requiere origen confirmado)

Para items `UNID/PERFIL/MT` en sede 1:

- Comportamiento normal por cantidad.

## Reglas de validacion implementadas para sede 1

- `tipoUnidad` invalido: 400.
- `cmBase <= 0` cuando viene informado: 400.
- Confirmar venta (`venta = true`) con item `CM` sin `cmBase`: 400.

## Persistencia sin nuevas entidades

No se modificaron entidades.

Para guardar `tipoUnidad` y `cmBase` por item, se embebe metadata en el campo `orden_items.nombre` con formato:

`<nombre visible> ##META:TIPO=CM;CMBASE=300`

Para tipos no CM:

`<nombre visible> ##META:TIPO=UNID`

## Lectura para frontend e impresion

En las respuestas de tabla y detalle se expone por item:

- `tipoUnidad`
- `cmBase`

El backend limpia el nombre visible para no mostrar el bloque `##META` en UI.

## Compatibilidad preservada

- Sedes 2 y 3 conservan flujo actual de cortes.
- No se tocaron endpoints de otros modulos.
- No se crearon tablas ni migraciones.

## Checklist de frontend (obligatorio)

### 1) Regla unica de sede

Crear helper unico:

- `esSedeSinControlCortes(sedeId) => Number(sedeId) === 1`

Usarlo en UI, payload y render.

### 2) Modal de orden en sede 1

- Ocultar flujo/modal de cortes.
- Capturar por item:
  - `tipoUnidad` (UNID, PERFIL, MT, CM)
  - `cmBase` (solo cuando tipoUnidad = CM)
- En lineas `CM`, usar `cantidad` como cms vendidos.

### 3) Payload hacia backend

Para sede 1:

- Enviar `items[]` con `tipoUnidad` y `cmBase`.
- No enviar `cortes[]` (o enviarlo vacio).

Para sedes 2/3:

- Mantener payload actual con `cortes[]`.

### 4) Flujo cotizacion -> confirmacion en sede 1

- Si existe item `CM` sin `cmBase`, guardar como cotizacion (`venta=false`).
- Cuando bodega confirme origen:
  - editar orden,
  - completar `cmBase`,
  - confirmar cambiando a `venta=true`.

### 5) Render de impresion/formato antiguo

Para sede 1, usar:

- Columnas: `CANT | PRODUCTO | COLOR | TIPO`
- `TIPO`:
  - `UNID`, `PERFIL`, `MT`
  - `CM(600)`, `CM(300)`, etc. usando `tipoUnidad` + `cmBase`.

### 6) Manejo de errores 400

Mostrar mensaje de validacion cuando backend responda:

- tipoUnidad invalido
- cmBase invalido
- confirmacion de venta CM sin cmBase

## Archivos backend tocados

- `src/main/java/com/casaglass/casaglass_backend/service/OrdenService.java`
- `src/main/java/com/casaglass/casaglass_backend/dto/OrdenVentaDTO.java`
- `src/main/java/com/casaglass/casaglass_backend/dto/OrdenActualizarDTO.java`
- `src/main/java/com/casaglass/casaglass_backend/dto/OrdenTablaDTO.java`
- `src/main/java/com/casaglass/casaglass_backend/dto/OrdenDetalleDTO.java`

## Validacion tecnica

- Compilacion Maven completada correctamente despues de los cambios.

---

# FLUJO COMPLETO: Cotización → Confirmación para Sede 1

## Paso 1: Vendedor crea COTIZACIÓN con items CM sin origen confirmado

El vendedor NO SABE aún de qué pieza se cortará (bodega aún no lo confirma).

**Request:** `POST /api/ordenes/venta`

```json
{
  "fecha": "2026-04-09",
  "obra": "Obra Nueva Vidrios",
  "descripcion": "Cortinas de 150cm",
  "venta": false,
  "credito": false,
  "incluidaEntrega": false,
  "tieneRetencion": false,
  "tieneRetencionIca": false,
  "clienteId": 1,
  "sedeId": 1,
  "trabajadorId": 1,
  "items": [
    {
      "productoId": 10,
      "cantidad": 2,
      "tipoUnidad": "CM",
      "cmBase": null,
      "precio": 50000.0,
      "precioCorte": 0.0
    }
  ],
  "cortes": []
}
```

**Respuesta exitosa:** 201 Created
- Orden guardada con `venta=false` (cotización)
- Item almacenado con nombre metadata: `Producto X ##META:TIPO=CM;CMBASE=null`
- ✅ NO DESCUENTA inventario (es cotización)
- ✅ NO VALIDA cmBase (aún no confirmado)

---

## Paso 2: Vendedor INTENTA confirmar SIN haber completado los cmBase

Por error (o porque bodega aún no confirmó), vendedor hace click en "Confirmar" sin completar cmBase:

**Request:** `PUT /api/ordenes/tabla/{id}`

```json
{
  "fecha": "2026-04-09",
  "obra": "Obra Nueva Vidrios",
  "descripcion": "Cortinas de 150cm",
  "venta": true,
  "credito": false,
  "incluidaEntrega": false,
  "tieneRetencion": false,
  "tieneRetencionIca": false,
  "clienteId": 1,
  "sedeId": 1,
  "trabajadorId": 1,
  "items": []
}
```

**Respuesta: ❌ 400 Bad Request**

```json
{
  "error": "No se puede confirmar venta: el item \"Producto X\" tipo CM no tiene cmBase (origen del corte) completado. Por favor actualiza el item con el cmBase antes de confirmar."
}
```

✅ Backend rechaza la confirmación hasta no tener cmBase completado.

---

## Paso 3: Bodega confirma origen del corte

Bodega verifica: "Este corte de 150cm salió de una pieza de 400cm" → cmBase=400

Vendedor EDITA la orden vía tabla para actualizar cmBase y confirmar:

**Request:** `PUT /api/ordenes/tabla/{id}`

```json
{
  "fecha": "2026-04-09",
  "obra": "Obra Nueva Vidrios",
  "descripcion": "Cortinas de 150cm",
  "venta": true,
  "credito": false,
  "incluidaEntrega": false,
  "tieneRetencion": false,
  "tieneRetencionIca": false,
  "clienteId": 1,
  "sedeId": 1,
  "trabajadorId": 1,
  "items": [
    {
      "id": 1,
      "productoId": 10,
      "cantidad": 2,
      "tipoUnidad": "CM",
      "cmBase": 400,
      "precio": 50000.0,
      "precioCorte": 0.0,
      "eliminar": false
    }
  ]
}
```

**Respuesta exitosa:** 200 OK
- Campo cambiado: `venta=true` (ahora es venta confirmada)
- Field actualizado: `cmBase=400` (origen confirmado)
- ✅ VALIDA cmBase presente (venta requiere origen)
- ✅ NO DESCUENTA inventario (porque cmBase < 600)
- ✅ DESCUENTA inventario SÓLO si cmBase=600 (corte de pieza entera)

---

## Escenario especial: Corte DE pieza entera (cmBase=600)

Si el corte salió de una **pieza entera** (600cm):

```json
{
  "id": 1,
  "productoId": 10,
  "cantidad": 1,
  "tipoUnidad": "CM",
  "cmBase": 600,
  "precio": 50000.0,
  "precioCorte": 0.0,
  "eliminar": false
}
```

**Resultado:**
- ✅ DESCUENTA 1 UNIDAD del inventario (porque cmBase=600)
- Inventario del producto 10: bajará de N a N-1

---

## Tabla de decisiones

| Acción | tipoUnidad | cmBase | venta.Antes | venta.Ahora | Acción Backend |
|--------|-----------|--------|------------|------------|---|
| Crear cotización | CM | NULL | N/A | false | ✅ Guarda, NO descuenta |
| Crear venta directa | CM | 600 | N/A | true | ✅ Guarda, DESCUENTA 1 |
| Intentar confirmar sin cmBase | CM | NULL | false | true | ❌ RECHAZA 400 |
| Confirmar con cmBase | CM | 400 | false | true | ✅ Guarda, NO descuenta |
| Actualizar fragmento | CM | 300 | false | true | ✅ Guarda, NO descuenta |
| Actualizar pieza entera | CM | 600 | false | true | ✅ Guarda, DESCUENTA 1 |
| Reverso a cotización | CM | 400 | true | false | ✅ Guarda, NO descuenta |
| Item normal | UNID/PERFIL/MT | - | - | any | ✅ Normal (por cantidad) |

