# 📋 DOCUMENTACIÓN COMPLETA: POST /api/ordenes/venta

**Fecha:** 08 de enero de 2026  
**Estado:** ✅ ACTIVO EN PRODUCCIÓN  
**Endpoint:** `POST /api/ordenes/venta`

---

## 🎯 PROPÓSITO

Este endpoint crea órdenes de venta (ventas de productos de vidrio) con:
- ✅ Manejo automático de inventario (decrementos por venta)
- ✅ Soporte para cortes de productos PERFIL
- ✅ Cálculo automático de IVA y retención en la fuente
- ✅ Generación automática de número de orden
- ✅ Soporte para ventas de contado y a crédito
- ✅ Control de concurrencia con lock pesimista
- ✅ Reutilización de cortes sobrantes

---

## 📡 ESPECIFICACIÓN DEL ENDPOINT

### URL
```
POST https://casa-glass.com/api/ordenes/venta
```

### Headers Requeridos
```http
Content-Type: application/json
```

### Autenticación
El sistema requiere autenticación basada en sesión (cookies).

---

## 📥 REQUEST BODY (OrdenVentaDTO)

### Campos Principales

| Campo | Tipo | Requerido | Descripción | Valor por defecto |
|-------|------|-----------|-------------|-------------------|
| `clienteId` | Long | ✅ Sí | ID del cliente que compra | - |
| `sedeId` | Long | ✅ Sí | ID de la sede donde se realiza la venta | - |
| `items` | Array | ✅ Sí | Lista de productos a vender (mínimo 1) | - |
| `fecha` | LocalDate | ❌ No | Fecha de la orden (formato: YYYY-MM-DD) | Fecha actual |
| `obra` | String | ❌ No | Nombre del proyecto/obra | null |
| `descripcion` | String | ❌ No | Observaciones adicionales | null |
| `trabajadorId` | Long | ❌ No | ID del vendedor encargado | null |
| `venta` | boolean | ❌ No | Si es una venta | true |
| `credito` | boolean | ❌ No | Si es venta a crédito | false |
| `incluidaEntrega` | boolean | ❌ No | Si incluye entrega | false |
| `tieneRetencionFuente` | boolean | ❌ No | Si aplica retención en la fuente | false |
| `descuentos` | Double | ❌ No | Descuentos aplicados a la orden | 0.0 |
| `montoEfectivo` | Double | ❌ No | Monto pagado en efectivo (solo contado) | 0.0 |
| `montoTransferencia` | Double | ❌ No | Monto pagado por transferencia (solo contado) | 0.0 |
| `montoCheque` | Double | ❌ No | Monto pagado con cheque (solo contado) | 0.0 |
| `cortes` | Array | ❌ No | Lista de cortes de productos PERFIL | null |

### Estructura de `items[]` (OrdenItemVentaDTO)

Cada item representa un producto a vender:

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `productoId` | Long | ✅ Sí | ID del producto a vender |
| `cantidad` | Integer | ✅ Sí | Cantidad a vender (mínimo 1) |
| `precioUnitario` | Double | ✅ Sí | Precio unitario del producto (mayor a 0) |
| `descripcion` | String | ❌ No | Descripción personalizada del item |
| `reutilizarCorteSolicitadoId` | Long | ❌ No | ID de un corte existente a vender (si aplica) |

### Estructura de `cortes[]` (CorteSolicitadoDTO)

Para ventas con cortes de productos PERFIL:

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `productoId` | Long | ✅ Sí | ID del producto PERFIL a cortar |
| `medidaSolicitada` | Integer | ✅ Sí | Medida del corte a vender (en cm) |
| `cantidad` | Integer | ✅ Sí | Cantidad de cortes |
| `precioUnitarioSolicitado` | Double | ✅ Sí | Precio del corte a vender |
| `precioUnitarioSobrante` | Double | ✅ Sí | Precio del corte sobrante |
| `medidaSobrante` | Integer | ❌ No | Medida del corte sobrante (en cm) |
| `reutilizarCorteId` | Long | ❌ No | ID de un corte sobrante a reutilizar |
| `esSobrante` | Boolean | ❌ No | Si este corte es sobrante (false = vendido) | false |
| `cantidadesPorSede` | Array | ❌ No | Distribución del sobrante por sedes | null |

#### Estructura de `cantidadesPorSede[]`

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `sedeId` | Long | ✅ Sí | ID de la sede |
| `cantidad` | Integer | ✅ Sí | Cantidad a agregar en esa sede |

---

## 📤 RESPUESTA EXITOSA (200 OK)

### Estructura de la Respuesta

```json
{
  "mensaje": "Orden de venta creada exitosamente",
  "orden": {
    "id": 1234,
    "numero": 5678,
    "fecha": "2026-01-08",
    "obra": "Proyecto Casa Blanca",
    "descripcion": "Venta de vidrios para ventanas",
    "venta": true,
    "credito": false,
    "incluidaEntrega": false,
    "tieneRetencionFuente": false,
    "estado": "ACTIVA",
    "subtotal": 100000.0,
    "iva": 19000.0,
    "retencionFuente": 2500.0,
    "descuentos": 0.0,
    "total": 116500.0,
    "cliente": {
      "id": 10,
      "nombre": "Juan Pérez",
      "documento": "1234567890"
    },
    "sede": {
      "id": 1,
      "nombre": "Sede Principal"
    },
    "trabajador": {
      "id": 5,
      "nombre": "María González"
    },
    "items": [
      {
        "id": 4567,
        "cantidad": 2,
        "precioUnitario": 50000.0,
        "totalLinea": 100000.0,
        "descripcion": "Vidrio templado 6mm",
        "producto": {
          "id": 20,
          "nombre": "Vidrio Templado",
          "codigo": "VT-6MM"
        }
      }
    ]
  },
  "numero": 5678
}
```

### Campos de la Respuesta

| Campo | Descripción |
|-------|-------------|
| `mensaje` | Mensaje de confirmación |
| `orden` | Objeto completo de la orden creada |
| `orden.id` | ID único de la orden en la base de datos |
| `orden.numero` | Número de orden generado automáticamente (secuencial) |
| `orden.subtotal` | Base imponible sin IVA |
| `orden.iva` | IVA calculado (19%) |
| `orden.retencionFuente` | Retención calculada (2.5% si aplica) |
| `orden.total` | Total a pagar (subtotal + IVA - retención - descuentos) |
| `numero` | Duplicado del número de orden (por compatibilidad) |

---

## ❌ RESPUESTAS DE ERROR

### 1. Error 400 - Validación (STOCK_INSUFICIENTE)

**Causa:** Datos inválidos o faltantes en la solicitud.

```json
{
  "error": "El cliente es obligatorio para realizar una venta",
  "tipo": "VALIDACION",
  "codigo": "STOCK_INSUFICIENTE"
}
```

**Validaciones que causan error 400:**

- ❌ `clienteId` es null
- ❌ `sedeId` es null
- ❌ `items` está vacío o null
- ❌ Algún item tiene `productoId` null
- ❌ Algún item tiene `cantidad` <= 0
- ❌ Algún item tiene `precioUnitario` <= 0
- ❌ Cliente no existe en la base de datos
- ❌ Sede no existe en la base de datos
- ❌ Trabajador no existe en la base de datos (si se envía)
- ❌ Producto no existe en la base de datos

### 2. Error 400 - Entidad No Encontrada (NOT_FOUND)

**Causa:** Uno de los IDs enviados no existe en la base de datos.

```json
{
  "error": "Cliente no encontrado con ID: 123",
  "tipo": "ENTIDAD_NO_ENCONTRADA",
  "codigo": "NOT_FOUND"
}
```

**Posibles mensajes:**
- "Cliente no encontrado con ID: X"
- "Sede no encontrada con ID: X"  
- "Trabajador no encontrado con ID: X"
- "Producto no encontrado con ID: X"
- "Corte no encontrado con ID: X"

**Solución:** 
- Verificar que el ID enviado es correcto
- Re4. Error 500 - Error de Procesamiento (ERROR_PROCESAMIENTO)

**Causa:** Error de runtime que no es de entidad no encontrada.

```json
{
  "error": "Error al procesar la orden: [descripción]",
  "tipo": "ERROR_PROCESAMIENTO"
}
```

**Posibles causas:**
- Error en procesamiento de cortes
- Error al generar número de orden
- Error al calcular valores monetarios

---

### 5. Error 500 - Error del Servidor (SERVIDOR)

**Causa:** Error interno del servidor no manejado.

```json
{
  "error": "Error interno del servidor: [descripción del error]",
  "tipo": "SERVIDOR"
}
```

**Posibles causas:**

- Error de conexión a base de datos
- Error inesperado en lógica de negocio
- Excepción no capturada
```

**¿Cuándo ocurre?**

- Dos usuarios venden el mismo producto en la misma sede **exactamente** al mismo tiempo
- Se detecta un `OptimisticLockException` o `ObjectOptimisticLockingFailureException`
- El campo `@Version` detectó que el registro fue modificado entre lectura y escritura

**Solución:** Reintentar la operación automáticamente (recomendado) o mostrar mensaje al usuario.

### 3. Error 500 - Error del Servidor

**Causa:** Error interno del servidor no manejado.

```json
{
  "error": "Error interno del servidor: [descripción del error]",
  "tipo": "SERVIDOR"
}
```

**Posibles causas:**

- Error de conexión a base de datos
- Error en procesamiento de cortes
- Error inesperado en lógica de negocio

---

## 🔄 FLUJO DE PROCESAMIENTO

### Orden de Ejecución

1. **Validaciones de negocio**
   - Validar campos obligatorios
   - Verificar existencia de cliente, sede, trabajador
   - Validar items (producto, cantidad, precio)

2. **Crear entidad Orden**
   - Establecer fecha (actual si no se envía)
   - Asignar cliente, sede, trabajador
   - Establecer flags (venta, crédito, retención)

3. **Procesar items de venta**
   - Crear OrdenItem por cada item del DTO
   - Calcular subtotal bruto (suma de todas las líneas con IVA incluido)

4. **Calcular valores monetarios**
   - **Subtotal sin IVA** = subtotalBruto / 1.19
   - **IVA** = subtotalSinIva × 0.19
   - **Retención** = subtotalSinIva × 0.025 (si aplica)
   - **Total** = subtotalBruto - retención - descuentos

5. **Generar número de orden**
   - Obtener siguiente número secuencial
   - Validar unicidad con reintentos (máx 5)

6. **Guardar orden**
   - Persistir en base de datos con @Transactional

7. **Procesar cortes (si existen)**
   - Decrementar inventario del producto original (si es corte)
   - Crear corte solicitado (el que se vende)
   - Crear/reutilizar corte sobrante
   - Incrementar inventario del sobrante en las sedes indicadas
   - **NO** incrementar inventario del corte vendido (queda en 0)

8. **Incrementar inventario de cortes reutilizados**
   - Si un item tiene `reutilizarCorteSolicitadoId`, incrementar su inventario primero
   - Lógica: El corte se está cortando de nuevo (inventario +1), luego se vende (inventario -1 = 0)

9. **Actualizar inventario por venta**
   - **Excluir** productos que están en `cortes[]` (ya manejados por `procesarCortes()`)
   - Para productos normales: decrementar inventario con lock pesimista
   - Para cortes: decrementar inventario de cortes con lock pesimista
   - Permitir valores negativos (ventas anticipadas)

10. **Crear crédito (si aplica)**
    - Si `credito == true`, crear registro de crédito para el cliente
    - Generar número de crédito automáticamente

---

## 🔒 MANEJO DE CONCURRENCIA

### Lock Pesimista

El endpoint utiliza **lock pesimista (PESSIMISTIC_WRITE)** para evitar conflictos de concurrencia al actualizar inventario:

```sql
SELECT * FROM inventario 
WHERE producto_id = ? AND sede_id = ? 
FOR UPDATE
```

### Ventajas del Lock Pesimista

- ✅ Evita race conditions en ventas simultáneas
- ✅ Garantiza consistencia de datos
- ✅ Operaciones son serializadas automáticamente por la base de datos

### Desventajas del Lock Pesimista

- ⚠️ Puede causar timeouts si la transacción es larga
- ⚠️ Bloquea operaciones concurrentes en el mismo inventario

### Manejo de Errores de Concurrencia

Si ocurre un conflicto, el sistema captura:

1. `jakarta.persistence.OptimisticLockException`
2. `org.springframework.orm.ObjectOptimisticLockingFailureException`

Y devuelve **HTTP 409 Conflict** con el código `CONFLICTO_STOCK`.

**Recomendación:** El frontend debe reintentar automáticamente (máximo 3 intentos con delay exponencial).

---

## 📦 MANEJO DE INVENTARIO

### Inventarios por Sede

Cada producto tiene inventarios independientes por sede:

```
Producto A - Sede 1: 10 unidades
Producto A - Sede 2: 5 unidades
Producto A - Sede 3: 0 unidades
```

**Ventas en diferentes sedes NO compiten** (diferentes registros, sin conflictos).

### Inventarios Negativos (Ventas Anticipadas)

✅ El sistema **permite inventarios negativos** para manejar ventas anticipadas:

```
Stock inicial: 5 unidades
Venta: 10 unidades
Stock final: -5 unidades (⚠️ venta anticipada)
```

Esto es intencional y permite vender productos antes de tenerlos en tienda.

### Actualización de Inventario

1. **Productos Normales:**
   - Se busca el inventario con lock pesimista
   - Se decrementa la cantidad vendida
   - Se permite valor negativo

2. **Cortes:**
   - Se busca el inventario de cortes con lock pesimista
   - Se decrementa la cantidad vendida
   - Se permite valor negativo

3. **Productos en `cortes[]`:**
   - Se OMITEN de la actualización normal
   - `procesarCortes()` ya manejó su inventario

---

## 🔪 PROCESAMIENTO DE CORTES

### ¿Qué es un Corte?

Un **corte** es dividir un producto PERFIL (ej: barra de aluminio de 600cm) en dos partes:
- **Corte Solicitado:** La parte que el cliente compra (ej: 250cm)
- **Corte Sobrante:** La parte que queda en inventario (ej: 350cm)

### Flujo de Procesamiento de Cortes

1. **Obtener producto original**
   - Buscar el producto PERFIL a cortar

2. **Decrementar inventario del original (si es corte)**
   - Si el producto original es a su vez un corte, decrementar su inventario
   - Ejemplo: Cortar un corte de 350cm en 200cm + 150cm

3. **Crear corte solicitado**
   - Crear nuevo producto tipo `Corte` con la medida solicitada
   - Heredar características del producto original (categoría, espesor, color, etc.)
   - Generar código único (ej: `ALU-25MM-350CM-C001`)
   - Asignar precio unitario del corte

4. **Crear/reutilizar corte sobrante**
   - Si viene `reutilizarCorteId`: usar ese corte existente
   - Si no: crear nuevo corte con la medida sobrante
   - Asignar precio unitario del sobrante

5. **Actualizar inventario de cortes**
   - **Corte solicitado:** NO incrementar inventario (se vende, queda en 0)
   - **Corte sobrante:** Incrementar inventario según `cantidadesPorSede[]`
   - Si ambos cortes son iguales (corte por la mitad), ajustar lógica

### Ejemplo de Corte

**Request:**
```json
{
  "clienteId": 10,
  "sedeId": 1,
  "items": [
    {
      "productoId": 999,  // ID del corte solicitado (se creará automáticamente)
      "cantidad": 1,
      "precioUnitario": 25000.0,
      "descripcion": "Aluminio 25mm x 250cm"
    }
  ],
  "cortes": [
    {
      "productoId": 20,  // ID del producto PERFIL original (600cm)
      "medidaSolicitada": 250,  // Corte a vender: 250cm
      "medidaSobrante": 350,    // Corte sobrante: 350cm
      "cantidad": 1,
      "precioUnitarioSolicitado": 25000.0,  // Precio del corte de 250cm
      "precioUnitarioSobrante": 35000.0,    // Precio del corte de 350cm
      "cantidadesPorSede": [
        { "sedeId": 1, "cantidad": 1 },  // Sobrante va a Sede 1
        { "sedeId": 2, "cantidad": 0 },
        { "sedeId": 3, "cantidad": 0 }
      ]
    }
  ]
}
```

**Resultado:**
- ✅ Inventario del producto PERFIL original (600cm) disminuye en 1
- ✅ Se crea corte solicitado de 250cm (stock = 0, se vende)
- ✅ Se crea corte sobrante de 350cm (stock = 1 en Sede 1)
- ✅ El cliente recibe el corte de 250cm

---

## 💰 CÁLCULO DE VALORES MONETARIOS

### Fórmulas

```javascript
// 1. Subtotal bruto (con IVA incluido)
subtotalBruto = suma de (cantidad × precioUnitario) de todos los items

// 2. Aplicar descuentos
subtotalBrutoConDescuento = subtotalBruto - descuentos

// 3. Calcular base sin IVA
subtotalSinIva = subtotalBrutoConDescuento / 1.19

// 4. Calcular IVA (19%)
iva = subtotalSinIva × 0.19

// 5. Calcular retención (2.5% si aplica)
retencionFuente = tieneRetencionFuente ? (subtotalSinIva × 0.025) : 0.0

// 6. Calcular total
total = subtotalBrutoConDescuento - retencionFuente
```

### Ejemplo de Cálculo

**Datos de entrada:**
- Item 1: 2 unidades × $50,000 = $100,000
- Item 2: 1 unidad × $30,000 = $30,000
- Descuentos: $5,000
- Tiene retención: Sí

**Cálculo:**
```
1. subtotalBruto = $100,000 + $30,000 = $130,000
2. subtotalBrutoConDescuento = $130,000 - $5,000 = $125,000
3. subtotalSinIva = $125,000 / 1.19 = $105,042.02
4. iva = $105,042.02 × 0.19 = $19,957.98
5. retencionFuente = $105,042.02 × 0.025 = $2,626.05
6. total = $125,000 - $2,626.05 = $122,373.95
```

**Valores guardados en la orden:**
- `subtotal`: $105,042.02
- `iva`: $19,957.98
- `retencionFuente`: $2,626.05
- `total`: $122,373.95
- `descuentos`: $5,000.00

---

## 🧪 EJEMPLO COMPLETO

### Request Completo

```json
{
  "clienteId": 15,
  "sedeId": 2,
  "trabajadorId": 8,
  "fecha": "2026-01-08",
  "obra": "Remodelación Edificio Central",
  "descripcion": "Venta de vidrios y perfiles para ventanas",
  "venta": true,
  "credito": false,
  "incluidaEntrega": true,
  "tieneRetencionFuente": true,
  "descuentos": 10000.0,
  "montoEfectivo": 100000.0,
  "montoTransferencia": 50000.0,
  "montoCheque": 0.0,
  "items": [
    {
      "productoId": 45,
      "cantidad": 3,
      "precioUnitario": 40000.0,
      "descripcion": "Vidrio templado 8mm transparente"
    },
    {
      "productoId": 67,
      "cantidad": 2,
      "precioUnitario": 25000.0,
      "descripcion": "Marco de aluminio anodizado"
    }
  ],
  "cortes": [
    {
      "productoId": 89,
      "medidaSolicitada": 280,
      "medidaSobrante": 320,
      "cantidad": 1,
      "precioUnitarioSolicitado": 28000.0,
      "precioUnitarioSobrante": 32000.0,
      "cantidadesPorSede": [
        { "sedeId": 2, "cantidad": 1 },
        { "sedeId": 1, "cantidad": 0 },
        { "sedeId": 3, "cantidad": 0 }
      ]
    }
  ]
}
```

### Response Exitosa

```json
{
  "mensaje": "Orden de venta creada exitosamente",
  "orden": {
    "id": 2456,
    "numero": 7890,
    "fecha": "2026-01-08",
    "obra": "Remodelación Edificio Central",
    "descripcion": "Venta de vidrios y perfiles para ventanas",
    "venta": true,
    "credito": false,
    "incluidaEntrega": true,
    "tieneRetencionFuente": true,
    "estado": "ACTIVA",
    "subtotal": 147058.82,
    "iva": 27941.18,
    "retencionFuente": 3676.47,
    "descuentos": 10000.0,
    "total": 161323.53,
    "cliente": {
      "id": 15,
      "nombre": "Constructora XYZ",
      "documento": "900123456-7",
      "telefono": "3001234567",
      "email": "contacto@constructoraxyz.com"
    },
    "sede": {
      "id": 2,
      "nombre": "Sede Norte",
      "direccion": "Calle 100 #15-20"
    },
    "trabajador": {
      "id": 8,
      "nombre": "Carlos Ramírez",
      "cargo": "Vendedor"
    },
    "items": [
      {
        "id": 5678,
        "cantidad": 3,
        "precioUnitario": 40000.0,
        "totalLinea": 120000.0,
        "descripcion": "Vidrio templado 8mm transparente",
        "producto": {
          "id": 45,
          "nombre": "Vidrio Templado 8mm",
          "codigo": "VT-8MM-TRANSP",
          "categoria": "Vidrios"
        }
      },
      {
        "id": 5679,
        "cantidad": 2,
        "precioUnitario": 25000.0,
        "totalLinea": 50000.0,
        "descripcion": "Marco de aluminio anodizado",
        "producto": {
          "id": 67,
          "nombre": "Marco Aluminio Anodizado",
          "codigo": "MA-ANOD",
          "categoria": "Marcos"
        }
      }
    ]
  },
  "numero": 7890
}
```

---

## 🐛 DIAGNÓSTICO DE ERROR 409 EN PRODUCCIÓN

### 🚨 BUG ENCONTRADO Y CORREGIDO (08/01/2026)

**Problema:** El endpoint estaba devolviendo error 409 para **CUALQUIER `RuntimeException`**, no solo conflictos de concurrencia.

**Bug en el código:**
```java
} catch (RuntimeException e) {
    // ❌ INCORRECTO: Esto captura TODOS los RuntimeException
    return ResponseEntity.status(409).body(...);
}
```

**Casos que causaban error 409 INCORRECTAMENTE:**
- ❌ Cliente no encontrado con ID: X
- ❌ Sede no encontrada con ID: X  
- ❌ Trabajador no encontrado con ID: X
- ❌ Producto no encontrado con ID: X
- ❌ Corte no encontrado con ID: X

**Corrección aplicada:**
- Error 409 ahora SOLO para `OptimisticLockException` (conflictos reales de concurrencia)
- Error 400 para entidades no encontradas
- Error 500 para otros errores de runtime

---

### Error Reportado

```
HTTP/2 409
POST https://casa-glass.com/api/ordenes/venta

Error: "Request failed with status code 409"
```

### ✅ CAUSA MÁS PROBABLE (DESPUÉS DEL BUG FIX)

**Entidad no encontrada** - Alguno de los IDs enviados no existe en la base de datos:

**Posibles causas:**
1. **ClienteId no existe** - El cliente fue eliminado o el ID es incorrecto
2. **SedeId no existe** - La sede fue eliminada o el ID es incorrecto
3. **TrabajadorId no existe** - El trabajador fue eliminado o el ID es incorrecto (si se envía)
4. **ProductoId no existe** - Algún producto en `items[]` no existe en la base de datos
5. **ReutilizarCorteSolicitadoId no existe** - El corte que se intenta reutilizar no existe

**Logs del servidor esperados:**
```
❌ ERROR RUNTIME: Cliente no encontrado con ID: 123
❌ ERROR RUNTIME: Sede no encontrada con ID: 456
❌ ERROR RUNTIME: Producto no encontrado con ID: 789
```

**Solución:**
- Verificar que todos los IDs enviados existen en la base de datos
- El frontend debe validar IDs antes de enviar la request
- Después del bug fix, esto devolverá **400 Bad Request** en lugar de 409

---

### Causas Menos Probables

#### 1. **Conflicto de Concurrencia en Inventario (RARO)**

**Síntoma:** Dos usuarios venden el mismo producto simultáneamente (con lock optimista).

**Logs del servidor esperados:**
```
❌ ERROR CONCURRENCIA (Lock Optimista): ...
OptimisticLockException: Row was updated or deleted by another transaction
```

**Solución:**
- El frontend debe reintentar automáticamente (2-3 veces con delay de 500ms)
- Verificar si múltiples usuarios están vendiendo el mismo producto

#### 2. **Lock Pesimista Timeout**

**Síntoma:** La transacción tarda demasiado y el lock expira.

**Logs del servidor esperados:**
```
PessimisticLockingFailureException: could not execute statement
```

**Solución:**
- Verificar logs del servidor para confirmar
- Revisar si hay procesos largos bloqueando inventario
- Aumentar timeout de transacciones (no recomendado)

#### 3. **Problema con Cortes**

**Síntoma:** Error al procesar cortes de productos PERFIL.

**Logs del servidor esperados:**
```
❌ Error al decrementar inventario del corte que se está cortando: ...
Error al guardar orden: ...
```

**Solución:**
- Verificar que el producto a cortar existe
- Verificar que el inventario del producto original es suficiente
- Revisar estructura de `cortes[]` en el request

#### 4. **Error en Generación de Número de Orden**

**Síntoma:** El sistema no puede generar número único después de 5 intentos.

**Logs del servidor esperados:**
```
Error generando número de orden después de 5 intentos
No se pudo generar un número de orden único después de 5 intentos
```

**Solución:**
- Verificar que la base de datos está respondiendo correctamente
- Revisar que no hay conflictos en la secuencia de números

### Pasos para Diagnosticar

1. **Revisar logs del servidor**
   ```bash
   # Ver últimos 100 errores
   tail -n 100 /var/log/casaglass/application.log | grep "ERROR"
   
   # Filtrar errores 409
   grep "409\|CONCURRENCIA\|CONFLICTO_STOCK" /var/log/casaglass/application.log
   ```

2. **Verificar estado del inventario**
   ```sql
   -- Ver inventario del producto que falló
   SELECT i.id, i.cantidad, i.version, p.nombre, s.nombre as sede
   FROM inventario i
   JOIN producto p ON i.producto_id = p.id
   JOIN sede s ON i.sede_id = s.id
   WHERE p.id = [PRODUCTO_ID] AND s.id = [SEDE_ID];
   ```

3. **Verificar transacciones activas**
   ```sql
   -- Ver transacciones bloqueadas (MySQL/MariaDB)
   SHOW PROCESSLIST;
   
   -- Ver locks activos
   SELECT * FROM information_schema.INNODB_LOCKS;
   ```

4. **Revisar request del frontend**
   - Verificar que `clienteId`, `sedeId`, `items[]` están presentes
   - Verificar que todos los productos existen
   - Verificar estructura de `cortes[]` si aplica

### Solución Recomendada para Frontend

Implementar retry logic con exponential backoff:

```javascript
async function crearOrdenVentaConRetry(ordenData, maxRetries = 3) {
  for (let intento = 0; intento < maxRetries; intento++) {
    try {
      const response = await axios.post('/api/ordenes/venta', ordenData);
      return response.data; // Éxito
    } catch (error) {
      if (error.response?.status === 409) {
        // Error 409: Conflicto de concurrencia
        if (intento < maxRetries - 1) {
          // Esperar antes de reintentar (exponential backoff)
          const delay = Math.pow(2, intento) * 500; // 500ms, 1s, 2s
          console.log(`Reintentando en ${delay}ms... (intento ${intento + 1}/${maxRetries})`);
          await new Promise(resolve => setTimeout(resolve, delay));
          continue; // Reintentar
        }
      }
      // Si no es 409 o ya agotamos reintentos, lanzar error
      throw error;
    }
  }
}
```

---

## 📊 MÉTRICAS Y MONITOREO

### Métricas Importantes

- **Tiempo de respuesta promedio:** < 500ms
- **Tasa de éxito:** > 99%
- **Errores 409 (concurrencia):** < 1% de las requests
- **Reintentos exitosos:** > 95%

### Logs Clave

```
🔍 DEBUG: Iniciando creación de orden venta
🔍 DEBUG: Datos recibidos: [OrdenVentaDTO]
🔪 Procesando cortes...
📦 Stock actualizado: [cantidad_anterior] → [cantidad_nueva]
✅ Orden creada exitosamente: ID [orden_id]
```

### Alertas Recomendadas

- ⚠️ Más de 10 errores 409 en 1 minuto
- ⚠️ Tiempo de respuesta > 2 segundos
- ⚠️ Tasa de error > 5%
- ⚠️ Locks pesimistas activos > 10 segundos

---

## 🔗 REFERENCIAS

### Archivos Relacionados

- **Controller:** [OrdenController.java](src/main/java/com/casaglass/casaglass_backend/controller/OrdenController.java) líneas 139-207
- **Service:** [OrdenService.java](src/main/java/com/casaglass/casaglass_backend/service/OrdenService.java) líneas 159-256
- **DTO:** [OrdenVentaDTO.java](src/main/java/com/casaglass/casaglass_backend/dto/OrdenVentaDTO.java)
- **Manejo de Concurrencia:** [DOCUMENTACION_MANEJO_CONCURRENCIA_INVENTARIO.md](DOCUMENTACION_MANEJO_CONCURRENCIA_INVENTARIO.md)
- **Cálculo IVA/Retención:** [DOCUMENTACION_CALCULO_IVA_RETENCION_ORDENES.md](DOCUMENTACION_CALCULO_IVA_RETENCION_ORDENES.md)

### Otros Endpoints Relacionados

- `PUT /api/ordenes/venta/{id}` - Actualizar orden de venta
- `GET /api/ordenes/{id}` - Obtener orden por ID
- `DELETE /api/ordenes/{id}` - Anular orden (restaura inventario)
- `POST /api/creditos/orden/{ordenId}` - Crear crédito para orden

---

## 📝 NOTAS ADICIONALES

### Consideraciones de Seguridad

- ✅ El endpoint requiere autenticación
- ✅ Se valida existencia de entidades relacionadas (cliente, sede, productos)
- ✅ Los precios se reciben del frontend (calculados previamente)
- ⚠️ No hay validación de roles/permisos en el endpoint

### Limitaciones Conocidas

- Los inventarios negativos están permitidos (ventas anticipadas)
- No hay validación de stock mínimo
- El lock pesimista puede causar timeouts en transacciones largas
- No hay auditoría de cambios en inventario (solo logs)

### Mejoras Futuras

- [ ] Implementar validación de stock mínimo (opcional)
- [ ] Agregar auditoría de cambios en inventario
- [ ] Optimizar procesamiento de cortes para reducir tiempo de transacción
- [ ] Implementar caché para productos más vendidos
- [ ] Agregar notificaciones cuando el stock llega a 0 o valores negativos

---

## 📞 SOPORTE

Para reportar problemas o sugerencias:

- **Desarrollador:** [Tu Nombre]
- **Email:** soporte@casa-glass.com
- **Fecha de última actualización:** 08 de enero de 2026

---

**FIN DE LA DOCUMENTACIÓN**
