# 📋 EXPLICACIÓN DE CAMBIOS: Abono con Sede Propia

## 🎯 CONTEXTO DEL PROBLEMA

**Situación anterior:**
- Los abonos se asociaban a la sede de la orden original
- Si una venta se hacía en Sede A pero el cliente pagaba en Sede B, el abono aparecía en el reporte de Sede A
- Esto causaba problemas en los reportes de entrega de dinero

**Solución implementada:**
- Cada abono ahora tiene su propia sede (donde se registra el pago)
- El abono puede tener una sede diferente a la sede de la orden original
- Los reportes ahora muestran los abonos en la sede donde se recibió el pago

---

## 🔄 QUÉ CAMBIÓ EN EL BACKEND

### 1. **Entidad Abono** - Agregado campo `sede`
```java
// ANTES: No tenía campo sede
// AHORA: Tiene campo sede (ManyToOne con Sede)
private Sede sede; // Sede donde se registra el pago
```

### 2. **DTO AbonoDTO** - Agregado campo `sedeId` OBLIGATORIO
```java
// ANTES: No tenía sedeId
// AHORA: Tiene sedeId obligatorio
@NotNull(message = "El ID de la sede es obligatorio")
private Long sedeId;
```

### 3. **Consultas de Base de Datos** - Cambiaron para usar sede del abono
```sql
-- ANTES: Filtraba por orden.sede.id
WHERE o.sede.id = :sedeId

-- AHORA: Filtra por abono.sede.id
WHERE a.sede.id = :sedeId
```

---

## ⚠️ CAMBIOS OBLIGATORIOS EN EL FRONTEND

### **ENDPOINT: POST /api/creditos/{creditoId}/abonos**

#### ❌ ANTES (Ya no funciona):
```json
{
  "total": 67000.0,
  "fecha": "2026-01-20",
  "metodoPago": "TRANSFERENCIA",
  "factura": "12345",
  "montoEfectivo": 0.0,
  "montoTransferencia": 67000.0,
  "montoCheque": 0.0,
  "montoRetencion": 0.0
}
```

#### ✅ AHORA (Obligatorio):
```json
{
  "total": 67000.0,
  "fecha": "2026-01-20",
  "metodoPago": "TRANSFERENCIA",
  "factura": "12345",
  "montoEfectivo": 0.0,
  "montoTransferencia": 67000.0,
  "montoCheque": 0.0,
  "montoRetencion": 0.0,
  "sedeId": 2  // ✅ NUEVO - OBLIGATORIO
}
```

**⚠️ IMPORTANTE:**
- Si NO envías `sedeId`, el backend retornará error 400: "El ID de la sede es obligatorio"
- El `sedeId` debe ser la sede donde se está registrando el pago (puede ser diferente a la sede de la orden)

---

## 📝 PASO A PASO: QUÉ HACER EN EL FRONTEND

### **PASO 1: Identificar dónde se crea el abono**

Busca en tu código donde haces el POST a `/api/creditos/{creditoId}/abonos`

Ejemplo:
```javascript
// Archivo: services/AbonosService.js o similar
const crearAbono = async (creditoId, abonoData) => {
  const response = await fetch(`/api/creditos/${creditoId}/abonos`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(abonoData) // ← Aquí necesitas agregar sedeId
  });
  return response.json();
};
```

### **PASO 2: Obtener el sedeId**

Tienes 3 opciones para obtener el `sedeId`:

#### **Opción A: Del contexto/usuario actual** (RECOMENDADO)
```javascript
// Si tienes la sede del usuario actual en el contexto
const sedeId = usuarioActual.sedeId;
// o
const sedeId = sessionStorage.getItem('sedeId');
// o
const sedeId = useSelector(state => state.auth.sedeId); // Redux
```

#### **Opción B: Del CreditoPendienteDTO**
```javascript
// Si estás en la página de abonos, el crédito pendiente ya tiene la sede
// PERO RECUERDA: Puedes usar una sede diferente para el abono
const creditoPendiente = await obtenerCreditoPendiente(clienteId);
const sedeId = creditoPendiente.sede.id; // Sede de la orden
// O mejor aún, usa la sede actual del usuario:
const sedeId = usuarioActual.sedeId; // Sede donde se registra el pago
```

#### **Opción C: Selector en el formulario**
```javascript
// Si quieres permitir que el usuario elija la sede
<select 
  name="sedeId" 
  value={formData.sedeId}
  onChange={(e) => setFormData({...formData, sedeId: e.target.value})}
  required
>
  <option value="">Seleccione una sede</option>
  <option value="1">Sede Insula</option>
  <option value="2">Sede Otra</option>
  <option value="3">Sede Tercera</option>
</select>
```

### **PASO 3: Agregar sedeId al body del POST**

```javascript
// ANTES
const crearAbono = async (creditoId, abonoData) => {
  const body = {
    total: abonoData.total,
    fecha: abonoData.fecha,
    metodoPago: abonoData.metodoPago,
    factura: abonoData.factura,
    montoEfectivo: abonoData.montoEfectivo || 0,
    montoTransferencia: abonoData.montoTransferencia || 0,
    montoCheque: abonoData.montoCheque || 0,
    montoRetencion: abonoData.montoRetencion || 0
  };
  
  const response = await fetch(`/api/creditos/${creditoId}/abonos`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  return response.json();
};

// AHORA (con sedeId)
const crearAbono = async (creditoId, abonoData) => {
  // Obtener sedeId (ejemplo: del contexto/usuario actual)
  const sedeId = obtenerSedeActual(); // Tu función para obtener la sede
  
  const body = {
    total: abonoData.total,
    fecha: abonoData.fecha,
    metodoPago: abonoData.metodoPago,
    factura: abonoData.factura,
    montoEfectivo: abonoData.montoEfectivo || 0,
    montoTransferencia: abonoData.montoTransferencia || 0,
    montoCheque: abonoData.montoCheque || 0,
    montoRetencion: abonoData.montoRetencion || 0,
    sedeId: sedeId // ✅ NUEVO - OBLIGATORIO
  };
  
  const response = await fetch(`/api/creditos/${creditoId}/abonos`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  return response.json();
};
```

### **PASO 4: Actualizar también el PUT (editar abono)**

Si tienes un endpoint para editar abonos, también necesitas agregar `sedeId`:

```javascript
// PUT /api/creditos/{creditoId}/abonos/{abonoId}
const actualizarAbono = async (creditoId, abonoId, abonoData) => {
  const sedeId = obtenerSedeActual(); // O del formulario
  
  const body = {
    total: abonoData.total,
    fecha: abonoData.fecha,
    metodoPago: abonoData.metodoPago,
    factura: abonoData.factura,
    montoEfectivo: abonoData.montoEfectivo || 0,
    montoTransferencia: abonoData.montoTransferencia || 0,
    montoCheque: abonoData.montoCheque || 0,
    montoRetencion: abonoData.montoRetencion || 0,
    sedeId: sedeId // ✅ NUEVO - Opcional al editar, pero recomendado
  };
  
  const response = await fetch(`/api/creditos/${creditoId}/abonos/${abonoId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  return response.json();
};
```

---

## 🔍 ENDPOINTS QUE CAMBIARON SU COMPORTAMIENTO

### **GET /api/entrega-dinero/ordenes-disponibles**

**ANTES:**
- Filtraba abonos por la sede de la orden (donde se vendió)
- Si vendías en Sede 1 pero pagabas en Sede 2, el abono NO aparecía en Sede 2

**AHORA:**
- Filtra abonos por la sede del abono (donde se pagó)
- Si vendes en Sede 1 pero pagas en Sede 2, el abono SÍ aparece en Sede 2

**Ejemplo:**
```javascript
// GET /api/entrega-dinero/ordenes-disponibles?sedeId=2&desde=2026-01-01&hasta=2026-01-31
// Ahora retorna abonos que se registraron en Sede 2, 
// aunque la orden original haya sido de Sede 1
```

---

## ✅ ENDPOINTS QUE SIGUEN IGUAL

Estos endpoints **NO cambian** y siguen funcionando igual:

1. **GET /api/abonos** - Listar abonos
2. **GET /api/creditos/{creditoId}/abonos** - Listar abonos de un crédito
3. **GET /api/abonos/{abonoId}** - Obtener un abono específico
4. **GET /api/abonos/cliente/{clienteId}** - Listar abonos de un cliente
5. **GET /api/abonos/orden/{ordenId}** - Listar abonos de una orden

**Nota:** El filtro `sedeId` en GET /api/abonos ahora filtra por la sede del abono, no por la sede de la orden.

---

## 🚨 ERRORES COMUNES A EVITAR

### Error 1: No enviar sedeId
```
POST /api/creditos/41/abonos
Body: { total: 67000, fecha: "2026-01-20", ... } // Sin sedeId

Respuesta: 400 Bad Request
{
  "error": "El ID de la sede es obligatorio",
  "tipo": "VALIDACION"
}
```

**Solución:** Agregar `sedeId` al body

### Error 2: Enviar sedeId inválido
```
POST /api/creditos/41/abonos
Body: { ..., sedeId: 999 } // Sede que no existe

Respuesta: 400 Bad Request
{
  "error": "Sede no encontrada con ID: 999",
  "tipo": "VALIDACION"
}
```

**Solución:** Verificar que el `sedeId` existe antes de enviarlo

### Error 3: Asumir que debe coincidir con la sede de la orden
```javascript
// ❌ INCORRECTO: Asumir que deben coincidir
const sedeId = creditoPendiente.sede.id; // Sede de la orden

// ✅ CORRECTO: Usar la sede donde se registra el pago
const sedeId = usuarioActual.sedeId; // Sede actual del usuario
```

---

## 📋 CHECKLIST DE IMPLEMENTACIÓN

- [ ] **Identificar dónde se crea el abono** (POST /api/creditos/{creditoId}/abonos)
- [ ] **Obtener sedeId** del contexto/usuario actual
- [ ] **Agregar sedeId al body** del POST
- [ ] **Probar crear un abono** y verificar que funciona
- [ ] **Actualizar el PUT** (si existe) para editar abonos
- [ ] **Verificar reportes de entrega de dinero** funcionan correctamente
- [ ] **Probar escenario:** Crear abono en sede diferente a la sede de la orden
- [ ] **Verificar** que el abono aparece en el reporte de la sede correcta

---

## 💡 EJEMPLO COMPLETO DE IMPLEMENTACIÓN

```javascript
// services/AbonosService.js

// Función helper para obtener la sede actual
const obtenerSedeActual = () => {
  // Opción 1: Del contexto/estado global
  const usuario = getUsuarioActual(); // Tu función
  return usuario?.sedeId;
  
  // Opción 2: Del sessionStorage
  // return parseInt(sessionStorage.getItem('sedeId'));
  
  // Opción 3: De Redux/Context
  // return useSelector(state => state.auth.sedeId);
};

// Crear abono
export const crearAbono = async (creditoId, abonoData) => {
  const sedeId = obtenerSedeActual();
  
  if (!sedeId) {
    throw new Error('No se pudo obtener la sede actual');
  }
  
  const body = {
    total: abonoData.total,
    fecha: abonoData.fecha,
    metodoPago: abonoData.metodoPago,
    factura: abonoData.factura || '',
    montoEfectivo: abonoData.montoEfectivo || 0,
    montoTransferencia: abonoData.montoTransferencia || 0,
    montoCheque: abonoData.montoCheque || 0,
    montoRetencion: abonoData.montoRetencion || 0,
    sedeId: sedeId // ✅ OBLIGATORIO
  };
  
  const response = await fetch(`/api/creditos/${creditoId}/abonos`, {
    method: 'POST',
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${getToken()}` // Si usas autenticación
    },
    body: JSON.stringify(body)
  });
  
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al crear abono');
  }
  
  return response.json();
};

// Actualizar abono (si existe)
export const actualizarAbono = async (creditoId, abonoId, abonoData) => {
  const sedeId = obtenerSedeActual();
  
  const body = {
    total: abonoData.total,
    fecha: abonoData.fecha,
    metodoPago: abonoData.metodoPago,
    factura: abonoData.factura || '',
    montoEfectivo: abonoData.montoEfectivo || 0,
    montoTransferencia: abonoData.montoTransferencia || 0,
    montoCheque: abonoData.montoCheque || 0,
    montoRetencion: abonoData.montoRetencion || 0,
    sedeId: sedeId // ✅ Recomendado al editar
  };
  
  const response = await fetch(`/api/creditos/${creditoId}/abonos/${abonoId}`, {
    method: 'PUT',
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${getToken()}`
    },
    body: JSON.stringify(body)
  });
  
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al actualizar abono');
  }
  
  return response.json();
};
```

---

## 🎯 RESUMEN PARA LA IA DEL FRONTEND

**CAMBIO PRINCIPAL:**
El endpoint `POST /api/creditos/{creditoId}/abonos` ahora requiere un campo adicional `sedeId` en el body.

**QUÉ HACER:**
1. Obtener el `sedeId` de la sede actual del usuario/contexto
2. Agregar `sedeId` al objeto que se envía en el body del POST
3. El `sedeId` representa la sede donde se registra el pago (puede ser diferente a la sede de la orden)

**EJEMPLO MÍNIMO:**
```javascript
const body = {
  ...abonoData,
  sedeId: usuarioActual.sedeId // Agregar esto
};
```

**ERRORES A EVITAR:**
- No enviar `sedeId` → Error 400
- Enviar `sedeId` null o inválido → Error 400

