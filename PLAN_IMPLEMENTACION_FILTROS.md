# PLAN DE IMPLEMENTACIÓN DE FILTROS - BACKEND

## FASE 1 - CRÍTICO (ALTA PRIORIDAD)

### ✅ FASE 1.1: GET /api/ordenes/tabla
**Estado:** En progreso
**Filtros a agregar:**
- clienteId, sedeId, estado, fechaDesde, fechaHasta
- venta, credito, facturada
- page, size (paginación)
- sortBy, sortOrder (ordenamiento)

### ⏳ FASE 1.2: GET /api/ordenes
**Estado:** Pendiente
**Filtros a agregar:** Mismos que tabla

### ⏳ FASE 1.3: GET /api/ordenes/credito
**Estado:** Pendiente
**Filtros a agregar:** fechaDesde, fechaHasta, estado, page, size

### ⏳ FASE 1.4: GET /api/ingresos
**Estado:** Pendiente
**Filtros a agregar:** proveedorId, fechaDesde, fechaHasta, procesado, numeroFactura, page, size, sortBy, sortOrder

### ⏳ FASE 1.5: GET /api/traslados-movimientos
**Estado:** Pendiente
**Filtros a agregar:** sedeOrigenId, sedeDestinoId, sedeId, fechaDesde, fechaHasta, estado, confirmado, trabajadorId, page, size

### ⏳ FASE 1.6: GET /api/creditos
**Estado:** Pendiente
**Filtros a agregar:** clienteId, fechaDesde, fechaHasta, estado, sedeId, page, size

### ⏳ FASE 1.7: Crear GET /api/abonos
**Estado:** Pendiente
**Nuevo endpoint con filtros:** clienteId, creditoId, fechaDesde, fechaHasta, metodoPago, sedeId, page, size

---

## FASE 2 - IMPORTANTE (MEDIA PRIORIDAD)
- Facturas, Reembolsos, Entregas de Dinero, Gastos

## FASE 3 - MEJORAS (BAJA PRIORIDAD)
- Productos, Clientes, Proveedores, etc.

---

## NOTAS DE IMPLEMENTACIÓN

1. **Compatibilidad hacia atrás:** Los filtros son opcionales. Si no se envían, retornar comportamiento actual.
2. **Paginación:** Si se envía `page` y `size`, retornar `PageResponse`. Si no, retornar `List`.
3. **Validación:** Validar todos los parámetros antes de procesar.
4. **Documentación:** Documentar cada cambio en el código.

