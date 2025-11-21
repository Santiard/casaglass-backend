# FUNCIONAMIENTO DEL SISTEMA DE REEMBOLSOS

## 📋 ESTRUCTURA DE ENTIDADES CREADAS

### 1. REEMBOLSO INGRESO (Devolución al Proveedor)

**Entidad**: `ReembolsoIngreso`
**Tabla**: `reembolsos_ingreso`

**Campos principales**:
- `id`: Identificador único
- `fecha`: Fecha del retorno (cuándo se devuelve al proveedor)
- `ingresoOriginal`: Referencia al `Ingreso` original que se está reembolsando
- `proveedor`: Proveedor al que se devuelve
- `numeroFacturaDevolucion`: Número de factura de devolución (opcional)
- `motivo`: Razón del reembolso
- `detalles`: Lista de `ReembolsoIngresoDetalle` (productos devueltos)
- `totalReembolso`: Total a reembolsar al proveedor
- `procesado`: Si ya se actualizó el inventario
- `estado`: PENDIENTE, PROCESADO, ANULADO

### 2. REEMBOLSO INGRESO DETALLE

**Entidad**: `ReembolsoIngresoDetalle`
**Tabla**: `reembolso_ingreso_detalles`

**Campos principales**:
- `id`: Identificador único
- `reembolsoIngreso`: Referencia al reembolso padre
- `ingresoDetalleOriginal`: Referencia al `IngresoDetalle` original (del ingreso que se reembolsa)
- `producto`: Producto que se está devolviendo
- `cantidad`: Cantidad de productos a devolver (puede ser parcial)
- `costoUnitario`: Costo unitario al momento del reembolso
- `totalLinea`: Total de la línea (cantidad × costoUnitario)

### 3. REEMBOLSO VENTA (Devolución del Cliente)

**Entidad**: `ReembolsoVenta`
**Tabla**: `reembolsos_venta`

**Campos principales**:
- `id`: Identificador único
- `fecha`: Fecha del retorno (cuándo el cliente devuelve)
- `ordenOriginal`: Referencia a la `Orden` original que se está reembolsando
- `cliente`: Cliente que devuelve
- `sede`: Sede donde se realiza el reembolso
- `motivo`: Razón del reembolso
- `detalles`: Lista de `ReembolsoVentaDetalle` (productos devueltos)
- `subtotal`: Subtotal del reembolso
- `descuentos`: Descuentos proporcionales
- `totalReembolso`: Total a reembolsar al cliente
- `formaReembolso`: EFECTIVO, TRANSFERENCIA, NOTA_CREDITO, AJUSTE_CREDITO
- `procesado`: Si ya se actualizó inventario y créditos
- `estado`: PENDIENTE, PROCESADO, ANULADO

### 4. REEMBOLSO VENTA DETALLE

**Entidad**: `ReembolsoVentaDetalle`
**Tabla**: `reembolso_venta_detalles`

**Campos principales**:
- `id`: Identificador único
- `reembolsoVenta`: Referencia al reembolso padre
- `ordenItemOriginal`: Referencia al `OrdenItem` original (del item de la orden que se reembolsa)
- `producto`: Producto que se está devolviendo
- `cantidad`: Cantidad de productos a devolver (puede ser parcial)
- `precioUnitario`: Precio unitario al momento del reembolso
- `totalLinea`: Total de la línea (cantidad × precioUnitario)

---

## 🔄 CÓMO FUNCIONA LA LÓGICA

### REEMBOLSO DE INGRESO (Devolución al Proveedor)

#### Paso 1: Crear Reembolso

```
Usuario selecciona:
- Ingreso original (ej: Ingreso #100 del 15/01/2025)
- Productos a devolver:
  - Producto A: 10 unidades (de 50 recibidas)
  - Producto B: 5 unidades (de 20 recibidas)
- Fecha del retorno: 20/01/2025
- Motivo: "Productos defectuosos"
- Número factura devolución: "DEV-001"
```

**Estructura en BD**:

```
ReembolsoIngreso:
  id: 1
  fecha: 2025-01-20
  ingresoOriginal: Ingreso #100
  proveedor: Proveedor X
  motivo: "Productos defectuosos"
  numeroFacturaDevolucion: "DEV-001"
  totalReembolso: 150000.0
  procesado: false
  estado: PENDIENTE

ReembolsoIngresoDetalle #1:
  ingresoDetalleOriginal: IngresoDetalle #50 (Producto A, 50 unidades)
  producto: Producto A
  cantidad: 10
  costoUnitario: 10000.0
  totalLinea: 100000.0

ReembolsoIngresoDetalle #2:
  ingresoDetalleOriginal: IngresoDetalle #51 (Producto B, 20 unidades)
  producto: Producto B
  cantidad: 5
  costoUnitario: 10000.0
  totalLinea: 50000.0
```

#### Paso 2: Procesar Reembolso

Cuando se procesa el reembolso:

1. **Validar cantidades**:
   - Verificar que 10 ≤ 50 (Producto A) ✅
   - Verificar que 5 ≤ 20 (Producto B) ✅

2. **Actualizar inventario**:
   - RESTAR 10 unidades de Producto A del inventario
   - RESTAR 5 unidades de Producto B del inventario
   - (En la sede donde estaban almacenados)

3. **Marcar como procesado**:
   - `procesado = true`
   - `estado = PROCESADO`

#### Resultado:
- El inventario se reduce en 15 productos
- El proveedor debe reembolsar $150,000
- Se mantiene referencia al ingreso original y sus detalles

---

### REEMBOLSO DE VENTA (Devolución del Cliente)

#### Paso 1: Crear Reembolso

```
Usuario selecciona:
- Orden original (ej: Orden #1001 del 10/01/2025)
- Productos a devolver:
  - Producto X: 3 unidades (de 5 vendidas)
  - Producto Y: 2 unidades (de 3 vendidas)
- Fecha del retorno: 25/01/2025
- Motivo: "Productos no cumplen especificaciones"
- Forma de reembolso: EFECTIVO
```

**Estructura en BD**:

```
ReembolsoVenta:
  id: 1
  fecha: 2025-01-25
  ordenOriginal: Orden #1001
  cliente: Cliente Y
  sede: Sede Centro
  motivo: "Productos no cumplen especificaciones"
  subtotal: 300000.0
  descuentos: 0.0
  totalReembolso: 300000.0
  formaReembolso: EFECTIVO
  procesado: false
  estado: PENDIENTE

ReembolsoVentaDetalle #1:
  ordenItemOriginal: OrdenItem #200 (Producto X, 5 unidades)
  producto: Producto X
  cantidad: 3
  precioUnitario: 50000.0
  totalLinea: 150000.0

ReembolsoVentaDetalle #2:
  ordenItemOriginal: OrdenItem #201 (Producto Y, 3 unidades)
  producto: Producto Y
  cantidad: 2
  precioUnitario: 75000.0
  totalLinea: 150000.0
```

#### Paso 2: Procesar Reembolso

Cuando se procesa el reembolso:

1. **Validar cantidades**:
   - Verificar que 3 ≤ 5 (Producto X) ✅
   - Verificar que 2 ≤ 3 (Producto Y) ✅

2. **Actualizar inventario**:
   - SUMAR 3 unidades de Producto X al inventario
   - SUMAR 2 unidades de Producto Y al inventario
   - (En la sede donde se realizó la venta)

3. **Ajustar crédito (si aplica)**:
   - Si la orden original fue a crédito:
     - Reducir el saldo del crédito en $300,000
     - Si el saldo llega a 0, cerrar el crédito

4. **Marcar como procesado**:
   - `procesado = true`
   - `estado = PROCESADO`

#### Resultado:
- El inventario se incrementa en 5 productos
- El cliente recibe $300,000 (en efectivo)
- Si fue a crédito, su deuda se reduce
- Se mantiene referencia a la orden original y sus items

---

## 📊 EJEMPLOS PRÁCTICOS

### Ejemplo 1: Reembolso Parcial de Ingreso

**Escenario**: Se recibieron 100 unidades, pero 15 están defectuosas.

```
Ingreso Original:
  - Producto Z: 100 unidades a $5,000 c/u = $500,000

Reembolso:
  - Producto Z: 15 unidades a $5,000 c/u = $75,000
  - Motivo: "Defectuosas"
  - Fecha: 2025-01-22

Resultado:
  - Inventario: -15 unidades
  - Proveedor debe reembolsar: $75,000
  - Quedan 85 unidades válidas en inventario
```

### Ejemplo 2: Reembolso Completo de Venta a Crédito

**Escenario**: Cliente devuelve toda una orden que fue a crédito.

```
Orden Original:
  - Total: $1,000,000
  - Forma de pago: CRÉDITO
  - Saldo pendiente: $1,000,000

Reembolso:
  - Todos los productos de la orden
  - Total: $1,000,000
  - Forma de reembolso: AJUSTE_CREDITO
  - Fecha: 2025-01-28

Resultado:
  - Inventario: +productos devueltos
  - Crédito: Saldo ajustado a $0
  - Crédito: Estado = CERRADO
  - Cliente: Sin deuda pendiente
```

### Ejemplo 3: Múltiples Reembolsos Parciales

**Escenario**: Un ingreso tiene 2 reembolsos en fechas diferentes.

```
Ingreso #100 (15/01/2025):
  - Producto A: 100 unidades

Reembolso #1 (20/01/2025):
  - Producto A: 10 unidades (defectuosas)
  - Estado: PROCESADO

Reembolso #2 (25/01/2025):
  - Producto A: 5 unidades (más defectuosas)
  - Estado: PROCESADO

Total devuelto: 15 unidades
Quedan en inventario: 85 unidades
```

---

## 🔗 RELACIONES ENTRE ENTIDADES

### ReembolsoIngreso
```
ReembolsoIngreso (1)
  └──> Ingreso (1) [ingresoOriginal]
  └──> Proveedor (1)
  └──> ReembolsoIngresoDetalle (N)
        └──> IngresoDetalle (1) [ingresoDetalleOriginal]
        └──> Producto (1)
```

### ReembolsoVenta
```
ReembolsoVenta (1)
  └──> Orden (1) [ordenOriginal]
  └──> Cliente (1)
  └──> Sede (1)
  └──> ReembolsoVentaDetalle (N)
        └──> OrdenItem (1) [ordenItemOriginal]
        └──> Producto (1)
```

---

## ✅ VALIDACIONES IMPORTANTES

### Reembolso de Ingreso
- ✅ La cantidad a devolver NO puede exceder la cantidad recibida en el `ingresoDetalleOriginal`
- ✅ El ingreso original debe estar procesado
- ✅ No se puede procesar un reembolso ya procesado
- ✅ Se puede crear múltiples reembolsos del mismo ingreso (parciales)

### Reembolso de Venta
- ✅ La cantidad a devolver NO puede exceder la cantidad vendida en el `ordenItemOriginal`
- ✅ La orden original debe estar ACTIVA (no anulada)
- ✅ Si la venta fue a crédito, el reembolso ajusta el saldo
- ✅ Se puede crear múltiples reembolsos de la misma orden (parciales)

---

## 🎯 VENTAJAS DE ESTE DISEÑO

1. **Trazabilidad Completa**:
   - Siempre sabes qué ingreso/orden se está reembolsando
   - Siempre sabes qué detalle/item específico se está reembolsando
   - Fecha exacta del retorno

2. **Reembolsos Parciales**:
   - Puedes devolver solo algunos productos
   - Puedes devolver cantidades parciales
   - Puedes hacer múltiples reembolsos del mismo documento

3. **Historial Completo**:
   - Puedes ver todos los reembolsos de un ingreso
   - Puedes ver todos los reembolsos de una orden
   - Puedes ver todos los reembolsos de un proveedor/cliente

4. **Impacto en Inventario**:
   - Automático al procesar
   - Reversible (anulando y creando reembolso inverso)

5. **Impacto Financiero**:
   - Registra el monto exacto a reembolsar
   - Ajusta créditos automáticamente
   - Permite diferentes formas de reembolso

---

## 📝 PRÓXIMOS PASOS

Para completar la implementación, necesitas:

1. ✅ **Entidades creadas** (4 entidades)
2. ✅ **Repositorios creados** (4 repositorios)
3. ⏳ **DTOs** (para crear y actualizar reembolsos)
4. ⏳ **Servicios** (lógica de negocio y validaciones)
5. ⏳ **Controladores** (endpoints REST)
6. ⏳ **Migraciones de BD** (crear tablas)

¿Quieres que continúe con los servicios y controladores?

