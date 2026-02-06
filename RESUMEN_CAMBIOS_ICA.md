# 📌 Resumen Rápido: Cambios ICA en Frontend

## 🆕 Campos Nuevos en DTOs

### Orden (Crear/Editar)
```json
{
  "tieneRetencionIca": true,      // boolean - NUEVO
  "porcentajeIca": 1.0            // number (opcional) - NUEVO
}
```

### Orden (Response)
```json
{
  "tieneRetencionIca": true,      // boolean - NUEVO
  "porcentajeIca": 1.0,           // number - NUEVO
  "retencionIca": 10000.0         // number - NUEVO
}
```

### Factura (Crear/Editar)
```json
{
  "retencionIca": 10000.0         // number (opcional) - NUEVO
}
```

---

## 🆕 Nuevo Endpoint

### `PUT /api/ordenes/{id}/retencion-ica`
**Propósito:** Actualizar solo los campos de ICA sin modificar la orden completa.

**Request:**
```json
{
  "tieneRetencionIca": true,
  "porcentajeIca": 1.0,           // opcional
  "retencionIca": 10000.0,
  "iva": 190000.0                 // opcional
}
```

---

## 📊 Endpoints Modificados

| Endpoint | Método | Cambio |
|----------|--------|--------|
| `/api/ordenes/venta` | POST | ✅ Agregar `tieneRetencionIca`, `porcentajeIca` |
| `/api/ordenes/{id}` | PUT | ✅ Agregar `tieneRetencionIca`, `porcentajeIca` |
| `/api/ordenes/{id}/retencion-ica` | PUT | ⭐ **NUEVO** |
| `/api/ordenes/{id}/detalle` | GET | ✅ Response incluye campos ICA |
| `/api/ordenes` | GET | ✅ Response incluye campos ICA |
| `/api/facturas` | POST | ✅ Agregar `retencionIca` (opcional) |
| `/api/facturas/{id}` | PUT | ✅ Agregar `retencionIca` |
| `/api/facturas/{id}` | GET | ✅ Response incluye `retencionIca` |

---

## 🎯 Lógica de Cálculo

```
Si (subtotalSinIva >= umbral) Y (tieneRetencionIca = true):
  retencionIca = subtotalSinIva × (porcentajeIca / 100)
Sino:
  retencionIca = 0.0
```

**Notas:**
- Base: Subtotal sin IVA
- Porcentaje: Si no se envía, usa default (1.0%)
- Umbral: Por defecto 1,000,000 COP

---

## ✅ Checklist Mínimo

- [ ] Agregar checkbox `tieneRetencionIca` en formulario crear/editar orden
- [ ] Agregar input `porcentajeIca` (opcional) en formulario crear/editar orden
- [ ] Mostrar `retencionIca` en detalle de orden
- [ ] Mostrar `retencionIca` en detalle de factura
- [ ] Actualizar sección "Resumen de Impuestos" para incluir ICA
- [ ] (Opcional) Implementar endpoint `PUT /api/ordenes/{id}/retencion-ica`

---

## 📖 Documentación Completa

Ver `DOCUMENTACION_ICA_FRONTEND.md` para detalles completos, ejemplos de código, y guías de implementación.

