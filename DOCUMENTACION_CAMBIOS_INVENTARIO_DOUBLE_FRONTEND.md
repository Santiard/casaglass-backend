# 📋 Documentación de Cambios: Inventario de Integer a Double

## 🎯 Resumen de Cambios

Se ha modificado el backend para soportar **cantidades decimales** en productos de vidrio (vendidos en m²). Todos los campos `cantidad` que antes eran `Integer` ahora son `Double`.

---

## 🔧 Cambios en la Base de Datos

### Tabla `inventario`
```sql
ALTER TABLE inventario MODIFY COLUMN cantidad DECIMAL(10,2) NOT NULL;
```

### Tabla `inventario_corte`  
```sql
ALTER TABLE inventario_corte MODIFY COLUMN cantidad DECIMAL(10,2) NOT NULL;
```

### Otras tablas afectadas:
- `ingreso_detalle.cantidad` → `DECIMAL(10,2)`
- `orden_item.cantidad` → `DECIMAL(10,2)`
- `traslado_detalle.cantidad` → `DECIMAL(10,2)`
- `reembolso_ingreso_detalle.cantidad` → `DECIMAL(10,2)`
- `reembolso_venta_detalle.cantidad` → `DECIMAL(10,2)`

---

## 📡 Cambios en los DTOs (Respuestas del Backend)

### **Inventario**

#### ✅ Antes:
```json
{
  "id": 1,
  "productoId": 15,
  "sedeId": 2,
  "cantidad": 50
}
```

#### ✅ Ahora:
```json
{
  "id": 1,
  "productoId": 15,
  "sedeId": 2,
  "cantidad": 50.5
}
```

---

### **Orden de Venta (Items)**

#### ✅ Antes:
```json
{
  "id": 10,
  "cantidad": 3,
  "precioUnitario": 1500.0,
  "totalLinea": 4500.0
}
```

#### ✅ Ahora:
```json
{
  "id": 10,
  "cantidad": 3.75,
  "precioUnitario": 1500.0,
  "totalLinea": 5625.0
}
```

---

### **Ingreso (Detalle)**

#### ✅ Antes:
```json
{
  "id": 20,
  "productoId": 5,
  "cantidad": 100,
  "costoUnitario": 800.0
}
```

#### ✅ Ahora:
```json
{
  "id": 20,
  "productoId": 5,
  "cantidad": 100.25,
  "costoUnitario": 800.0
}
```

---

### **Traslado (Detalle)**

#### ✅ Antes:
```json
{
  "id": 30,
  "productoId": 8,
  "cantidad": 25
}
```

#### ✅ Ahora:
```json
{
  "id": 30,
  "productoId": 8,
  "cantidad": 25.5
}
```

---

### **Reembolsos (Venta e Ingreso)**

#### ✅ Antes:
```json
{
  "id": 40,
  "cantidad": 5,
  "costoUnitario": 1200.0
}
```

#### ✅ Ahora:
```json
{
  "id": 40,
  "cantidad": 5.25,
  "costoUnitario": 1200.0
}
```

---

## 🔌 Cambios en Endpoints

### **POST /api/ordenes/venta**

#### ✅ Antes:
```json
{
  "items": [
    {
      "productoId": 10,
      "cantidad": 3,
      "precioUnitario": 1500.0
    }
  ]
}
```

#### ✅ Ahora:
```json
{
  "items": [
    {
      "productoId": 10,
      "cantidad": 3.75,
      "precioUnitario": 1500.0
    }
  ]
}
```

---

### **POST /api/ingresos**

#### ✅ Antes:
```json
{
  "detalles": [
    {
      "productoId": 5,
      "cantidad": 100,
      "costoUnitario": 800.0
    }
  ]
}
```

#### ✅ Ahora:
```json
{
  "detalles": [
    {
      "productoId": 5,
      "cantidad": 100.25,
      "costoUnitario": 800.0
    }
  ]
}
```

---

### **POST /api/traslados**

#### ✅ Antes:
```json
{
  "detalles": [
    {
      "productoId": 8,
      "cantidad": 25
    }
  ]
}
```

#### ✅ Ahora:
```json
{
  "detalles": [
    {
      "productoId": 8,
      "cantidad": 25.5
    }
  ]
}
```

---

### **PUT /api/inventario/producto/{productoId}**

#### ✅ Antes:
```json
{
  "cantidadInsula": 50,
  "cantidadCentro": 30,
  "cantidadPatios": 20
}
```

#### ✅ Ahora:
```json
{
  "cantidadInsula": 50.75,
  "cantidadCentro": 30.5,
  "cantidadPatios": 20.25
}
```

---

## ⚠️ Migración en el Frontend

### **1. Actualizar tipos en TypeScript**

```typescript
// ❌ Antes
interface InventarioDTO {
  id: number;
  productoId: number;
  sedeId: number;
  cantidad: number; // ❌ Era integer
}

// ✅ Ahora
interface InventarioDTO {
  id: number;
  productoId: number;
  sedeId: number;
  cantidad: number; // ✅ Ahora es double (JavaScript number soporta decimales)
}
```

### **2. Actualizar inputs en formularios**

```html
<!-- ❌ Antes -->
<input type="number" step="1" v-model="cantidad" />

<!-- ✅ Ahora -->
<input type="number" step="0.01" v-model="cantidad" />
```

### **3. Validaciones**

```javascript
// ❌ Antes
if (!Number.isInteger(cantidad)) {
  throw new Error("La cantidad debe ser un número entero");
}

// ✅ Ahora
if (cantidad <= 0 || cantidad > 999999.99) {
  throw new Error("La cantidad debe estar entre 0.01 y 999999.99");
}
```

### **4. Formateo para mostrar**

```javascript
// Para mostrar cantidades con 2 decimales
const cantidadFormateada = cantidad.toFixed(2); // "50.75"

// Para productos normales (no vidrios), redondear al entero más cercano
const cantidadEntera = Math.round(cantidad); // 51
```

---

## 📊 Casos de Uso

### **Caso 1: Venta de vidrio en m²**
```json
{
  "productoId": 15,
  "cantidad": 2.50,
  "precioUnitario": 3000.0
}
```
**Total**: `2.50 × 3000 = 7500.00`

### **Caso 2: Ingreso de vidrio**
```json
{
  "productoId": 15,
  "cantidad": 125.75,
  "costoUnitario": 2500.0
}
```
**Total**: `125.75 × 2500 = 314375.00`

### **Caso 3: Productos normales (enteros)**
```json
{
  "productoId": 8,
  "cantidad": 100.0,
  "precioUnitario": 500.0
}
```
**Total**: `100.0 × 500 = 50000.00`

---

## ✅ Checklist de Migración Frontend

- [ ] Actualizar interfaces TypeScript de:
  - InventarioDTO
  - OrdenItemDTO  
  - IngresoDetalleDTO
  - TrasladoDetalleDTO
  - ReembolsoIngresoDetalleDTO
  - ReembolsoVentaDetalleDTO
  - CorteInventarioDTO
  - OrdenVentaCreateDTO

- [ ] Cambiar inputs de `step="1"` a `step="0.01"`

- [ ] Actualizar validaciones (permitir decimales)

- [ ] Formatear valores con `.toFixed(2)` en tablas/reportes

- [ ] Probar:
  - Crear órdenes con cantidades decimales
  - Crear ingresos con cantidades decimales
  - Crear traslados con cantidades decimales
  - Actualizar inventarios con decimales
  - Reembolsos parciales con decimales

---

## 📞 Soporte

Si encuentras algún problema o tienes dudas, por favor contacta al equipo de backend.

**Nota**: Todos los campos `cantidad` ahora soportan hasta 2 decimales (`DECIMAL(10,2)`). El rango válido es: `0.00` a `99999999.99`.
