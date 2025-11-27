# 📋 DOCUMENTACIÓN: GET /api/entregas-dinero/ordenes-disponibles

## Endpoint
```
GET /api/entregas-dinero/ordenes-disponibles?sedeId=X&desde=YYYY-MM-DD&hasta=YYYY-MM-DD
```

## Parámetros (todos obligatorios)
- `sedeId` (Long): ID de la sede
- `desde` (LocalDate): Fecha inicial del período (formato: YYYY-MM-DD)
- `hasta` (LocalDate): Fecha final del período (formato: YYYY-MM-DD)

## Estructura de Respuesta

```json
{
  "ordenesContado": [
    {
      "id": 100,
      "numero": 1001,
      "fecha": "2025-01-15",
      "clienteNombre": "Juan Pérez",
      "clienteNit": "1234567-8",
      "total": 85000.0,
      "obra": "Casa nueva",
      "descripcion": "Venta de vidrios",
      "sedeNombre": "Sede Central",
      "trabajadorNombre": "María González",
      "yaEntregada": false,
      "entregaId": null,
      "esContado": true,
      "estado": "ACTIVA",
      "venta": true
    }
  ],
  "abonosDisponibles": [
    {
      "id": 50,
      "ordenId": 101,
      "numeroOrden": 1002,
      "fechaOrden": "2025-01-10",
      "fechaAbono": "2025-01-15",
      "clienteNombre": "Pedro García",
      "clienteNit": "9876543-2",
      "montoAbono": 50000.0,
      "montoOrden": 200000.0,
      "metodoPago": "EFECTIVO, TRANSFERENCIA - Banco de Bogotá",
      "factura": "REC-001",
      "obra": "Edificio comercial",
      "sedeNombre": "Sede Central",
      "trabajadorNombre": "María González",
      "yaEntregado": false,
      "estadoOrden": "ACTIVA",
      "ventaOrden": true
    }
  ],
  "totales": {
    "contado": 5,
    "credito": 3,
    "total": 8
  }
}
```

## Campos de `ordenesContado` (OrdenParaEntregaDTO)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | Long | ID de la orden |
| `numero` | Long | Número de la orden |
| `fecha` | LocalDate | Fecha de la orden (YYYY-MM-DD) |
| `clienteNombre` | String | Nombre del cliente |
| `clienteNit` | String | NIT del cliente |
| `total` | Double | Monto total de la orden |
| `obra` | String | Obra/proyecto de la orden |
| `descripcion` | String | Descripción/observaciones adicionales |
| `sedeNombre` | String | Nombre de la sede |
| `trabajadorNombre` | String | Nombre del trabajador |
| `yaEntregada` | Boolean | Si la orden ya está incluida en otra entrega |
| `entregaId` | Long | ID de la entrega actual (si aplica) |
| `esContado` | Boolean | true si NO es crédito (siempre true en este array) |
| `estado` | String | Estado de la orden: "ACTIVA", "ANULADA", etc. |
| `venta` | Boolean | true si es una venta (no compra) |

## Campos de `abonosDisponibles` (AbonoParaEntregaDTO)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | Long | **ID del abono** (no de la orden) |
| `ordenId` | Long | ID de la orden a la que pertenece el abono |
| `numeroOrden` | Long | Número de la orden |
| `fechaOrden` | LocalDate | Fecha de la orden (YYYY-MM-DD) |
| `fechaAbono` | LocalDate | Fecha del abono (YYYY-MM-DD) |
| `clienteNombre` | String | Nombre del cliente |
| `clienteNit` | String | NIT del cliente |
| `montoAbono` | Double | **Monto del abono** (no el total de la orden) |
| `montoOrden` | Double | Monto total de la orden |
| `metodoPago` | String | Método de pago del abono (puede ser largo, hasta 3000 caracteres) |
| `factura` | String | Número de factura/recibo del abono |
| `obra` | String | Obra/proyecto de la orden |
| `sedeNombre` | String | Nombre de la sede |
| `trabajadorNombre` | String | Nombre del trabajador |
| `yaEntregado` | Boolean | Si el abono ya está en otra entrega (basado en si la orden está incluida) |
| `estadoOrden` | String | Estado de la orden: "ACTIVA", "ANULADA", etc. |
| `ventaOrden` | Boolean | true si la orden asociada es una venta (no compra) |

## Filtros y Condiciones

### Para `ordenesContado`:
- ✅ De la sede especificada (`sedeId`)
- ✅ En el período indicado (`desde` a `hasta`)
- ✅ Venta a contado (`credito = false`)
- ✅ Es una venta (`venta = true`)
- ✅ No incluida en otra entrega (`incluidaEntrega = false`)
- ✅ Estado ACTIVA (`estado = 'ACTIVA'`)

### Para `abonosDisponibles`:
- ✅ De la sede especificada (`sedeId`)
- ✅ Abono realizado en el período indicado (`fechaAbono` entre `desde` y `hasta`)
- ✅ De órdenes a crédito (`credito = true`)
- ✅ Orden es una venta (`venta = true`)
- ✅ Orden no incluida en otra entrega (`incluidaEntrega = false`)
- ✅ Orden estado ACTIVA (`estado = 'ACTIVA'`)
- ✅ Abono no incluido en otra entrega (verificado por query)

**Nota importante**: No se filtra por estado del crédito. Un abono aparece en la lista aunque el crédito se haya cerrado después, porque el abono fue realizado en el período consultado y necesita ser entregado.

## Notas Importantes

1. **Para órdenes a crédito**: El endpoint NO devuelve las órdenes completas, sino los **ABONOS individuales**. Cada abono es un pago parcial de una orden a crédito.

2. **Campo `metodoPago`**: Puede contener descripciones largas (hasta 3000 caracteres) con:
   - Múltiples métodos de pago: "EFECTIVO, TRANSFERENCIA - Banco de Bogotá"
   - Retenciones: "TRANSFERENCIA - Retención 3.5%"
   - Observaciones: "EFECTIVO - Pago parcial, pendiente $50,000"

3. **Campo `montoAbono`**: Es el monto del abono individual, NO el total de la orden. Para el total de la orden, usar `montoOrden`.

4. **Campo `id` en abonos**: Es el ID del abono, NO el ID de la orden. Para el ID de la orden, usar `ordenId`.

5. **Campo `yaEntregado`**: Se basa en si la orden está marcada como `incluidaEntrega = true`. Si una orden a crédito tiene múltiples abonos, todos los abonos mostrarán `yaEntregado = true` si la orden ya fue incluida en una entrega.

## Ejemplo de Uso en Frontend

```javascript
const ordenes = await EntregasService.obtenerOrdenesDisponibles(sedeId, fechaDesde, fechaHasta);

// Estructura esperada:
// {
//   ordenesContado: [...],
//   abonosDisponibles: [...],
//   totales: { contado: 5, credito: 3, total: 8 }
// }

// Procesar órdenes a contado
ordenes.ordenesContado.forEach(orden => {
  console.log(`Orden #${orden.numero}: $${orden.total}`);
});

// Procesar abonos (órdenes a crédito)
ordenes.abonosDisponibles.forEach(abono => {
  console.log(`Abono #${abono.id} de Orden #${abono.numeroOrden}: $${abono.montoAbono}`);
  console.log(`Método de pago: ${abono.metodoPago}`);
  console.log(`Total de la orden: $${abono.montoOrden}`);
});
```

## Diferencias Clave

| Aspecto | Órdenes a Contado | Órdenes a Crédito (Abonos) |
|---------|------------------|---------------------------|
| **Qué se devuelve** | Orden completa | Abono individual |
| **ID principal** | `id` (de la orden) | `id` (del abono) |
| **Monto** | `total` (total de la orden) | `montoAbono` (monto del abono) |
| **Fecha principal** | `fecha` (fecha de la orden) | `fechaAbono` (fecha del abono) |
| **Múltiples registros** | Una orden = un registro | Una orden puede tener múltiples abonos |

