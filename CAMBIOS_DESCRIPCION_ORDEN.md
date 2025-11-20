# 📝 Cambios: Campo Descripción en Órdenes

## 🎯 Resumen

Se agregó un nuevo campo **`descripcion`** a las órdenes para que el vendedor pueda escribir observaciones o detalles adicionales.

---

## ✅ Cambios en el Backend

### 1. **Modelo `Orden`**
- ✅ Agregado campo `descripcion` (tipo TEXT, permite texto largo)
- ✅ Campo opcional (puede ser `null`)

### 2. **DTOs Actualizados**
- ✅ `OrdenVentaDTO` - Agregado campo `descripcion`
- ✅ `OrdenTablaDTO` - Agregado campo `descripcion`
- ✅ `OrdenActualizarDTO` - Agregado campo `descripcion`
- ✅ `OrdenDetalleDTO` - Agregado campo `descripcion`

### 3. **Servicios Actualizados**
- ✅ Todos los métodos de creación y actualización ahora procesan el campo `descripcion`

---

## 🔄 Endpoints Afectados

### ✅ Endpoints que AHORA ACEPTAN `descripcion`:

#### 1. **POST /api/ordenes/venta** - Crear orden de venta
- **Antes**: No tenía campo `descripcion`
- **Ahora**: Acepta campo `descripcion` (opcional)

#### 2. **PUT /api/ordenes/venta/{id}** - Actualizar orden de venta
- **Antes**: No tenía campo `descripcion`
- **Ahora**: Acepta campo `descripcion` (opcional)

#### 3. **PUT /api/ordenes/tabla/{id}** - Actualizar orden desde tabla
- **Antes**: No tenía campo `descripcion`
- **Ahora**: Acepta campo `descripcion` (opcional)

### ✅ Endpoints que AHORA RETORNAN `descripcion`:

#### 1. **GET /api/ordenes** - Listar órdenes
- **Ahora**: Retorna campo `descripcion` en cada orden

#### 2. **GET /api/ordenes/{id}** - Obtener orden por ID
- **Ahora**: Retorna campo `descripcion`

#### 3. **GET /api/ordenes/{id}/detalle** - Obtener detalle de orden
- **Ahora**: Retorna campo `descripcion`

#### 4. **GET /api/ordenes/tabla** - Listar órdenes para tabla
- **Ahora**: Retorna campo `descripcion` en cada orden

---

## 📡 Cambios en el Frontend

### 1. **Crear Orden de Venta**

#### Request (POST /api/ordenes/venta):

**ANTES:**
```json
{
  "clienteId": 1,
  "sedeId": 2,
  "obra": "Proyecto XYZ",
  "credito": false,
  "items": [...]
}
```

**AHORA (con descripción opcional):**
```json
{
  "clienteId": 1,
  "sedeId": 2,
  "obra": "Proyecto XYZ",
  "descripcion": "Cliente solicita entrega urgente. Llamar antes de entregar.",  // ← NUEVO CAMPO
  "credito": false,
  "items": [...]
}
```

#### Ejemplo en JavaScript/TypeScript:

```javascript
// Crear orden con descripción
const crearOrden = async (datosOrden) => {
  const payload = {
    clienteId: datosOrden.clienteId,
    sedeId: datosOrden.sedeId,
    obra: datosOrden.obra,
    descripcion: datosOrden.descripcion || null,  // ← NUEVO CAMPO (opcional)
    credito: datosOrden.credito || false,
    incluidaEntrega: datosOrden.incluidaEntrega || false,
    items: datosOrden.items
  };
  
  const response = await fetch('/api/ordenes/venta', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  
  return response.json();
};
```

---

### 2. **Actualizar Orden de Venta**

#### Request (PUT /api/ordenes/venta/{id}):

**ANTES:**
```json
{
  "clienteId": 1,
  "sedeId": 2,
  "obra": "Proyecto XYZ Actualizado",
  "credito": false,
  "items": [...]
}
```

**AHORA (con descripción opcional):**
```json
{
  "clienteId": 1,
  "sedeId": 2,
  "obra": "Proyecto XYZ Actualizado",
  "descripcion": "Descripción actualizada con nuevos detalles",  // ← NUEVO CAMPO
  "credito": false,
  "items": [...]
}
```

#### Ejemplo en JavaScript/TypeScript:

```javascript
// Actualizar orden con descripción
const actualizarOrden = async (ordenId, datosOrden) => {
  const payload = {
    clienteId: datosOrden.clienteId,
    sedeId: datosOrden.sedeId,
    obra: datosOrden.obra,
    descripcion: datosOrden.descripcion || null,  // ← NUEVO CAMPO (opcional)
    credito: datosOrden.credito || false,
    incluidaEntrega: datosOrden.incluidaEntrega || false,
    items: datosOrden.items
  };
  
  const response = await fetch(`/api/ordenes/venta/${ordenId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  
  return response.json();
};
```

---

### 3. **Actualizar Orden desde Tabla**

#### Request (PUT /api/ordenes/tabla/{id}):

**ANTES:**
```json
{
  "id": 456,
  "fecha": "2025-01-15",
  "obra": "Proyecto XYZ",
  "venta": true,
  "credito": false,
  "clienteId": 1,
  "sedeId": 2,
  "items": [...]
}
```

**AHORA (con descripción opcional):**
```json
{
  "id": 456,
  "fecha": "2025-01-15",
  "obra": "Proyecto XYZ",
  "descripcion": "Observaciones adicionales de la orden",  // ← NUEVO CAMPO
  "venta": true,
  "credito": false,
  "clienteId": 1,
  "sedeId": 2,
  "items": [...]
}
```

---

### 4. **Response de Endpoints GET**

#### GET /api/ordenes/{id} - Response:

**ANTES:**
```json
{
  "id": 456,
  "numero": 1001,
  "fecha": "2025-01-15",
  "obra": "Proyecto XYZ",
  "total": 150000,
  ...
}
```

**AHORA (incluye descripcion):**
```json
{
  "id": 456,
  "numero": 1001,
  "fecha": "2025-01-15",
  "obra": "Proyecto XYZ",
  "descripcion": "Cliente solicita entrega urgente",  // ← NUEVO CAMPO
  "total": 150000,
  ...
}
```

#### GET /api/ordenes/{id}/detalle - Response:

**ANTES:**
```json
{
  "id": 456,
  "numero": 1001,
  "fecha": "2025-01-15",
  "obra": "Proyecto XYZ",
  "total": 150000,
  "cliente": {...},
  "items": [...]
}
```

**AHORA (incluye descripcion):**
```json
{
  "id": 456,
  "numero": 1001,
  "fecha": "2025-01-15",
  "obra": "Proyecto XYZ",
  "descripcion": "Cliente solicita entrega urgente",  // ← NUEVO CAMPO
  "total": 150000,
  "cliente": {...},
  "items": [...]
}
```

#### GET /api/ordenes/tabla - Response:

**ANTES:**
```json
[
  {
    "id": 456,
    "numero": 1001,
    "fecha": "2025-01-15",
    "obra": "Proyecto XYZ",
    "venta": true,
    "credito": false,
    ...
  }
]
```

**AHORA (incluye descripcion):**
```json
[
  {
    "id": 456,
    "numero": 1001,
    "fecha": "2025-01-15",
    "obra": "Proyecto XYZ",
    "descripcion": "Cliente solicita entrega urgente",  // ← NUEVO CAMPO
    "venta": true,
    "credito": false,
    ...
  }
]
```

---

## 🎨 Ejemplo Completo: Formulario de Crear Orden

### Componente React/Vue/Angular:

```jsx
// Ejemplo React
function FormularioOrden() {
  const [orden, setOrden] = useState({
    clienteId: null,
    sedeId: null,
    obra: '',
    descripcion: '',  // ← NUEVO CAMPO
    credito: false,
    items: []
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    const payload = {
      clienteId: orden.clienteId,
      sedeId: orden.sedeId,
      obra: orden.obra,
      descripcion: orden.descripcion || null,  // ← Enviar descripción
      credito: orden.credito,
      incluidaEntrega: false,
      items: orden.items
    };

    try {
      const response = await fetch('/api/ordenes/venta', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      
      const data = await response.json();
      console.log('Orden creada:', data);
    } catch (error) {
      console.error('Error:', error);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {/* ... otros campos ... */}
      
      <div>
        <label>Obra/Proyecto:</label>
        <input
          type="text"
          value={orden.obra}
          onChange={(e) => setOrden({...orden, obra: e.target.value})}
        />
      </div>
      
      {/* ← NUEVO CAMPO */}
      <div>
        <label>Descripción/Observaciones:</label>
        <textarea
          value={orden.descripcion}
          onChange={(e) => setOrden({...orden, descripcion: e.target.value})}
          placeholder="Escribe observaciones o detalles adicionales..."
          rows={4}
        />
      </div>
      
      {/* ... otros campos ... */}
      
      <button type="submit">Crear Orden</button>
    </form>
  );
}
```

---

## 📋 Resumen de Atributos

### ✅ Atributo Nuevo: `descripcion`

| Propiedad | Valor |
|-----------|-------|
| **Tipo** | `string` o `null` |
| **Obligatorio** | ❌ No (opcional) |
| **Longitud** | Sin límite (TEXT en BD) |
| **Uso** | Observaciones/detalles adicionales de la orden |

### 📤 Enviar en Request (POST/PUT):

```json
{
  "descripcion": "Texto libre con observaciones"  // ← Opcional
}
```

### 📥 Recibir en Response (GET):

```json
{
  "descripcion": "Texto libre con observaciones"  // ← Puede ser null
}
```

---

## ⚠️ Consideraciones Importantes

### 1. **Campo Opcional**
- ✅ Puedes enviar `descripcion: null`
- ✅ Puedes omitir el campo completamente
- ✅ Si no envías nada, se guarda como `null`

### 2. **Compatibilidad hacia atrás**
- ✅ Las órdenes existentes tendrán `descripcion: null`
- ✅ No rompe código existente si no envías el campo
- ✅ El frontend puede ignorar el campo si no lo necesita

### 3. **Base de Datos**
- ✅ La columna se crea automáticamente al ejecutar la aplicación
- ✅ Tipo: `TEXT` (permite texto largo)
- ✅ Permite valores `NULL`

---

## 🔍 Ejemplos de Uso

### Ejemplo 1: Orden Simple (sin descripción)
```json
POST /api/ordenes/venta
{
  "clienteId": 1,
  "sedeId": 2,
  "obra": "Casa nueva",
  "items": [
    {
      "productoId": 10,
      "cantidad": 5,
      "precioUnitario": 30000
    }
  ]
}
```
✅ Funciona perfectamente (descripción será `null`)

### Ejemplo 2: Orden con Descripción
```json
POST /api/ordenes/venta
{
  "clienteId": 1,
  "sedeId": 2,
  "obra": "Casa nueva",
  "descripcion": "Cliente solicita entrega antes del viernes. Llamar al 3001234567 para coordinar.",
  "items": [
    {
      "productoId": 10,
      "cantidad": 5,
      "precioUnitario": 30000
    }
  ]
}
```
✅ Guarda la descripción correctamente

### Ejemplo 3: Actualizar Solo Descripción
```json
PUT /api/ordenes/venta/456
{
  "clienteId": 1,
  "sedeId": 2,
  "obra": "Casa nueva",
  "descripcion": "Descripción actualizada con nueva información",
  "items": [
    {
      "productoId": 10,
      "cantidad": 5,
      "precioUnitario": 30000
    }
  ]
}
```
✅ Actualiza la descripción

---

## 🎯 Checklist para el Frontend

### ✅ Pasos a seguir:

1. **Agregar campo en formularios:**
   - [ ] Agregar `<textarea>` o `<input>` para `descripcion` en formulario de crear orden
   - [ ] Agregar campo en formulario de editar orden
   - [ ] Campo opcional (no requerido)

2. **Actualizar tipos/interfaces:**
   ```typescript
   // TypeScript
   interface OrdenVenta {
     clienteId: number;
     sedeId: number;
     obra?: string;
     descripcion?: string;  // ← Agregar
     credito: boolean;
     items: OrdenItem[];
   }
   ```

3. **Incluir en payloads:**
   - [ ] Incluir `descripcion` en POST /api/ordenes/venta
   - [ ] Incluir `descripcion` en PUT /api/ordenes/venta/{id}
   - [ ] Incluir `descripcion` en PUT /api/ordenes/tabla/{id}

4. **Mostrar en UI:**
   - [ ] Mostrar `descripcion` en detalle de orden
   - [ ] Mostrar `descripcion` en tabla de órdenes (opcional)
   - [ ] Manejar caso cuando `descripcion` es `null` o vacío

5. **Validación (opcional):**
   - [ ] Limitar longitud si es necesario (backend no tiene límite)
   - [ ] Validar formato si es necesario

---

## 📝 Notas Finales

- ✅ **No hay breaking changes**: El campo es opcional
- ✅ **Compatibilidad total**: Funciona con código existente
- ✅ **Flexible**: Puedes usarlo o ignorarlo según necesites
- ✅ **Sin límite de texto**: Puedes escribir tanto como necesites

---

**Fecha de implementación**: 2025-01-XX  
**Versión del backend**: Compatible con todas las versiones actuales


