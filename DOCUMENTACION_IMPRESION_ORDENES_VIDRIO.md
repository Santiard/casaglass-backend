# Documentación Front - Impresión de Órdenes con Vidrio

## Objetivo

Permitir que el modal de impresión construya correctamente etiquetas como `VIDRIO 4MM` o `Nombre + 4MM` sin adivinar información desde el frontend.

## Endpoint principal

`GET /api/ordenes/{id}/detalle`

Este endpoint sigue siendo la fuente principal para el modal de impresión.

### Estructura relevante en la respuesta

En `items[].producto` ahora deben venir estos campos:

- `id`
- `codigo`
- `nombre`
- `color`
- `tipo`
- `mm`
- `grosorMm`
- `categoriaNombre`
- `esVidrio`

> Nota: `grosorMm` no es un campo persistido en la base de datos. En backend se expone como alias de `mm` para compatibilidad con el frontend.

## Regla canónica para identificar vidrio

- `esVidrio` es el campo principal y canónico para decidir si el item debe renderizarse como vidrio.
- `categoriaNombre` queda como apoyo, no como regla principal.
- Si `esVidrio === true`, el frontend no necesita inferir nada más para tratar el item como vidrio.

## Regla de render

Para cada item:

- Si `esVidrio === true`, usar el espesor para mostrar el vidrio.
- Si `categoriaNombre === "VIDRIO"`, usarlo solo como respaldo cuando `esVidrio` no esté disponible.
- Si `codigo` o `nombre` indican vidrio, usarlo solo como ayuda visual, no como fuente de verdad.

### Formato sugerido

- `nombre + " " + espesorMm + "MM"`
- Ejemplo: `VIDRIO CRISTAL 4MM`

### Prioridad del espesor

Cuando ambos campos vengan presentes:

1. Tomar primero `mm`.
2. Si `mm` viene null, usar `grosorMm`.
3. Si ambos vienen y tienen valores distintos, el backend debe considerarlos inconsistentes y el frontend debe usar `mm` como referencia visual.

### Origen real del espesor

- El valor real sale de `ProductoVidrio.mm`.
- `grosorMm` solo replica ese valor en el DTO de impresión.
- Si el item no es vidrio, ambos campos pueden venir null.

## Fallback de factura

`GET /api/facturas/orden/{ordenId}`

Este endpoint ahora devuelve `FacturaDetalleDTO`, no la entidad cruda.

La estructura esperada es consistente con la de órdenes:

- `factura.orden.items[].producto.codigo`
- `factura.orden.items[].producto.mm`
- `factura.orden.items[].producto.grosorMm`
- `factura.orden.items[].producto.categoriaNombre`
- `factura.orden.items[].producto.esVidrio`

## Lógica recomendada para frontend

1. Tomar primero `items[].producto.mm`.
2. Si viene null, usar `grosorMm`.
3. Si `esVidrio` es true o `categoriaNombre` es `VIDRIO`, pintar el espesor.
4. No depender del nombre del producto para inferir el mm.

## Contrato mínimo esperado

Para impresión confiable, el backend debe entregar siempre:

- código del producto
- nombre visible del producto
- indicador de vidrio
- espesor en mm
- categoría del producto

## Resumen corto para el equipo frontend

- `GET /api/ordenes/{id}/detalle` ya expone los datos para pintar vidrio.
- `GET /api/facturas/orden/{ordenId}` ahora devuelve un DTO consistente.
- El frontend puede componer la leyenda de impresión sin heurísticas adicionales.
