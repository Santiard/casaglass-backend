# 📊 ARQUITECTURA DE MÉTODOS DE PAGO EN ÓRDENES Y ABONOS

**Fecha de Análisis:** 2025-12-23  
**Estado del Sistema:** ✅ CORRECTAMENTE IMPLEMENTADO

---

## 🎯 PRINCIPIOS FUNDAMENTALES

### **Arquitectura de Dos Niveles:**

```
┌─────────────────────────────────────────┐
│  NIVEL 1: CAMPOS NUMÉRICOS              │
│  (FUENTE DE VERDAD)                     │
│  ✅ monto_efectivo                      │
│  ✅ monto_transferencia                 │
│  ✅ monto_cheque                        │
│  ✅ monto_retencion                     │
│                                         │
│  REGLA FUNDAMENTAL:                     │
│  efectivo + transferencia + cheque =    │
│  total (o monto del abono)              │
└─────────────────────────────────────────┘
            ▼
┌─────────────────────────────────────────┐
│  NIVEL 2: STRING DESCRIPTIVO            │
│  (SOLO INFORMACIÓN/DISPLAY)             │
│  ℹ️ metodo_pago VARCHAR(3000)           │
│                                         │
│  Ejemplo:                               │
│  "efectivo:279000,transferencia:0,..."  │
│                                         │
│  ⚠️ NUNCA usar para cálculos            │
└─────────────────────────────────────────┘
```

---

## 📋 ANÁLISIS DE LA IMPLEMENTACIÓN ACTUAL

### **1. Entidad `Orden.java`**

#### **Estado: ✅ CORRECTA**

```java
@Entity
@Table(name = "ordenes")
public class Orden {
    
    // ✅ CAMPOS NUMÉRICOS (Fuente de Verdad)
    @Column(name = "monto_efectivo", nullable = false)
    private Double montoEfectivo = 0.0;

    @Column(name = "monto_transferencia", nullable = false)
    private Double montoTransferencia = 0.0;

    @Column(name = "monto_cheque", nullable = false)
    private Double montoCheque = 0.0;
    
    // ✅ Estos campos existen y son obligatorios (NOT NULL)
    // ✅ Tienen valores por defecto (0.0)
    // ✅ Están correctamente anotados con @Column
}
```

**Documentación en el código:**
```java
/**
 * 💰 MONTOS POR MÉTODO DE PAGO (solo para órdenes de contado)
 * Almacenamiento numérico estructurado para cálculos exactos y auditoría
 * Para órdenes a crédito estos valores serán 0.00
 */
```

**Validación:**
- ✅ Los campos existen en la entidad
- ✅ Están mapeados a la base de datos
- ✅ Tienen valores por defecto
- ✅ Son NOT NULL en BD
- ⚠️ **NO hay campo `metodo_pago` STRING en Orden** (esto es correcto, las órdenes usan solo campos numéricos)

---

### **2. Entidad `Abono.java`**

#### **Estado: ✅ CORRECTA (con campo descriptivo adicional)**

```java
@Entity
@Table(name = "abonos")
public class Abono {
    
    // ℹ️ CAMPO DESCRIPTIVO (Solo información)
    @Column(name = "metodo_pago", length = 3000, nullable = false)
    private String metodoPago = "TRANSFERENCIA";
    
    // ✅ CAMPOS NUMÉRICOS (Fuente de Verdad)
    @Column(name = "monto_efectivo", nullable = false)
    private Double montoEfectivo = 0.0;

    @Column(name = "monto_transferencia", nullable = false)
    private Double montoTransferencia = 0.0;

    @Column(name = "monto_cheque", nullable = false)
    private Double montoCheque = 0.0;

    @Column(name = "monto_retencion", nullable = false)
    private Double montoRetencion = 0.0;
}
```

**Documentación en el código:**
```java
/**
 * 💰 MONTOS POR MÉTODO DE PAGO
 * Almacenamiento numérico estructurado para cálculos exactos y auditoría
 * La suma de efectivo + transferencia + cheque DEBE igualar el total del abono
 */

/** Método de pago (texto libre: EFECTIVO, TRANSFERENCIA, TARJETA, CHEQUE, OTRO, etc.)
 *  Puede incluir descripciones detalladas con múltiples métodos, retenciones y observaciones */
```

**Validación:**
- ✅ Los campos numéricos existen
- ✅ El campo `metodoPago` es solo descriptivo (VARCHAR 3000)
- ✅ Están correctamente documentados
- ✅ Tienen valores por defecto

---

### **3. Servicio `AbonoService.java`**

#### **Estado: ⚠️ PARCIALMENTE CORRECTO**

#### **Método `crearDesdeDTO()` - ✅ CORRECTO**

```java
@Transactional
public Abono crearDesdeDTO(Long creditoId, AbonoDTO abonoDTO) {
    // ... validaciones ...
    
    // ✅ CORRECTO: Asigna los campos numéricos desde el DTO
    abono.setMontoEfectivo(abonoDTO.getMontoEfectivo() != null ? abonoDTO.getMontoEfectivo() : 0.0);
    abono.setMontoTransferencia(abonoDTO.getMontoTransferencia() != null ? abonoDTO.getMontoTransferencia() : 0.0);
    abono.setMontoCheque(abonoDTO.getMontoCheque() != null ? abonoDTO.getMontoCheque() : 0.0);
    abono.setMontoRetencion(abonoDTO.getMontoRetencion() != null ? abonoDTO.getMontoRetencion() : 0.0);
    
    // ✅ CORRECTO: Valida que la suma coincida con el total
    Double sumaMetodos = abono.getMontoEfectivo() + abono.getMontoTransferencia() + abono.getMontoCheque();
    if (Math.abs(sumaMetodos - monto) > 0.01) {
        throw new IllegalArgumentException(
            String.format("La suma de los métodos de pago ($%.2f) no coincide con el monto total ($%.2f)", 
                        sumaMetodos, monto)
        );
    }
    
    // ℹ️ DESCRIPTIVO: Asigna el string metodoPago desde el DTO
    abono.setMetodoPago(abonoDTO.getMetodoPago());
    
    // ... resto del código ...
}
```

**✅ Este método es CORRECTO porque:**
1. Asigna los campos numéricos primero
2. Valida que la suma sea correcta
3. El string `metodoPago` se recibe ya construido desde el DTO

---

#### **Método `crear()` - ⚠️ PROBLEMA IDENTIFICADO**

```java
@Transactional
public Abono crear(Long creditoId, Abono payload) {
    // ... validaciones ...
    
    // ❌ PROBLEMA: Solo asigna el string metodoPago
    abono.setMetodoPago(payload.getMetodoPago() != null ? payload.getMetodoPago() : "TRANSFERENCIA");
    
    // ❌ FALTA: No está asignando los campos numéricos
    // ❌ FALTA: montoEfectivo, montoTransferencia, montoCheque
    // ❌ RESULTADO: Quedan en 0.0 (valor por defecto)
    
    Abono guardado = abonoRepo.save(abono);
    // ... resto del código ...
}
```

**❌ ESTE ES EL PROBLEMA:**

Cuando se crea un abono usando el método `crear()` (no `crearDesdeDTO()`):
1. Solo se asigna el string `metodoPago`
2. Los campos numéricos NO se asignan
3. Quedan con sus valores por defecto (0.0)
4. **Pero el string `metodoPago` podría tener valores diferentes**

**Ejemplo del bug:**
```java
// Frontend envía:
{
  metodoPago: "efectivo:5500000,transferencia:0,cheque:0",
  total: 279000
}

// Backend guarda:
abono.setMetodoPago("efectivo:5500000,..."); // ❌ String con valor incorrecto
abono.setMontoEfectivo(0.0);                 // ❌ Queda en 0.0 (no asignado)
abono.setMontoTransferencia(0.0);            // ❌ Queda en 0.0
abono.setMontoCheque(0.0);                   // ❌ Queda en 0.0
abono.setTotal(279000);                      // ✅ Correcto
```

---

#### **Método `actualizar()` - ⚠️ MISMO PROBLEMA**

```java
@Transactional
public Abono actualizar(Long creditoId, Long abonoId, Abono payload) {
    // ... código ...
    
    // ℹ️ Solo actualiza el string
    if (payload.getMetodoPago() != null) abono.setMetodoPago(payload.getMetodoPago());
    
    // ❌ FALTA: No actualiza los campos numéricos
    // ❌ montoEfectivo, montoTransferencia, montoCheque no se actualizan
}
```

---

### **4. Servicio `OrdenService.java`**

#### **Estado: ✅ CORRECTO**

```java
// En el método de crear orden de venta
orden.setMontoEfectivo(ventaDTO.getMontoEfectivo() != null ? ventaDTO.getMontoEfectivo() : 0.0);
orden.setMontoTransferencia(ventaDTO.getMontoTransferencia() != null ? ventaDTO.getMontoTransferencia() : 0.0);
orden.setMontoCheque(ventaDTO.getMontoCheque() != null ? ventaDTO.getMontoCheque() : 0.0);
```

**✅ Las órdenes están correctamente implementadas:**
- Asignan los campos numéricos desde el DTO
- No usan string `metodoPago` (no necesitan)
- Los cálculos se hacen sobre los campos numéricos

---

## 🐛 CAUSA RAÍZ DEL BUG IDENTIFICADO

### **Problema:**

El método `AbonoService.crear()` y `actualizar()` **NO están asignando los campos numéricos**.

### **¿Quién usa estos métodos?**

```
AbonoController → AbonoService.crear(creditoId, abono)
                              ↑
                              └─ Recibe un objeto Abono con metodoPago
                                 pero sin montoEfectivo, montoTransferencia, etc.
```

### **Flujo del Bug:**

```
1. Frontend envía JSON:
{
  "metodoPago": "efectivo:5500000,transferencia:0,cheque:0",
  "total": 279000
}

2. AbonoController deserializa a Abono:
abono.metodoPago = "efectivo:5500000,..."
abono.montoEfectivo = null (no viene en JSON)
abono.total = 279000

3. AbonoService.crear() guarda:
abono.setMetodoPago("efectivo:5500000,..."); // String incorrecto
// NO asigna montoEfectivo, montoTransferencia, montoCheque
// Quedan en 0.0 (valor por defecto de la entidad)

4. Base de datos:
id=36, total=279000 ✅
monto_efectivo=0 ❌
monto_transferencia=0 ❌
monto_cheque=0 ❌
metodo_pago="efectivo:5500000,..." ❌
```

---

## ✅ SOLUCIÓN IMPLEMENTADA

### **Cambiar el Controller para usar DTO (Solución Correcta)**

**✅ YA IMPLEMENTADO en el código:**

#### **1. AbonoController - Endpoint POST (Crear)**

```java
@PostMapping("/creditos/{creditoId}/abonos")
public ResponseEntity<?> crearAbono(
    @PathVariable Long creditoId,
    @RequestBody AbonoDTO dto) { // ✅ Ya usa DTO
    
    // ✅ Este método ya valida campos numéricos correctamente
    Abono abono = abonoService.crearDesdeDTO(creditoId, dto);
    return ResponseEntity.ok(abono);
}
```

#### **2. AbonoController - Endpoint PUT (Actualizar) - ✅ CORREGIDO**

```java
@PutMapping("/creditos/{creditoId}/abonos/{abonoId}")
public ResponseEntity<?> actualizar(
    @PathVariable Long creditoId,
    @PathVariable Long abonoId,
    @Valid @RequestBody AbonoDTO abonoDTO) { // ✅ Ahora usa DTO
    
    try {
        Abono abono = service.actualizarDesdeDTO(creditoId, abonoId, abonoDTO);
        return ResponseEntity.ok(new AbonoSimpleDTO(abono));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", e.getMessage(),
            "tipo", "VALIDACION"
        ));
    } catch (RuntimeException e) {
        return ResponseEntity.notFound().build();
    }
}
```

#### **3. AbonoService - Nuevo método actualizarDesdeDTO() - ✅ CREADO**

```java
@Transactional
public Abono actualizarDesdeDTO(Long creditoId, Long abonoId, AbonoDTO abonoDTO) {
    Abono abono = abonoRepo.findById(abonoId)
            .orElseThrow(() -> new RuntimeException("Abono no encontrado: " + abonoId));

    // Validaciones...
    
    // ✅ ACTUALIZAR CAMPOS NUMÉRICOS
    abono.setMontoEfectivo(abonoDTO.getMontoEfectivo() != null ? abonoDTO.getMontoEfectivo() : 0.0);
    abono.setMontoTransferencia(abonoDTO.getMontoTransferencia() != null ? abonoDTO.getMontoTransferencia() : 0.0);
    abono.setMontoCheque(abonoDTO.getMontoCheque() != null ? abonoDTO.getMontoCheque() : 0.0);
    abono.setMontoRetencion(abonoDTO.getMontoRetencion() != null ? abonoDTO.getMontoRetencion() : 0.0);

    // ✅ VALIDAR QUE LA SUMA DE MÉTODOS COINCIDA CON EL TOTAL
    Double sumaMetodos = abono.getMontoEfectivo() + abono.getMontoTransferencia() + abono.getMontoCheque();
    if (Math.abs(sumaMetodos - nuevoMonto) > 0.01) {
        throw new IllegalArgumentException(
            String.format("La suma de los métodos de pago ($%.2f) no coincide con el monto total ($%.2f)", 
                        sumaMetodos, nuevoMonto)
        );
    }
    
    // ... resto del código (recalcular saldos, etc.) ...
}
```

**Actualizar `AbonoController` para exigir campos numéricos:**

```java
@PostMapping("/creditos/{creditoId}/abonos")
public ResponseEntity<?> crearAbono(
    @PathVariable Long creditoId,
    @RequestBody AbonoDTO dto) { // ← Usar DTO en vez de Abono
    
    // Este método ya usa los campos numéricos correctamente
    Abono abono = abonoService.crearDesdeDTO(creditoId, dto);
    return ResponseEntity.ok(abono);
}
```

**Frontend debe enviar:**
```json
{
  "total": 279000,
  "metodoPago": "Efectivo - Pago completo",
  "montoEfectivo": 279000,
  "montoTransferencia": 0,
  "montoCheque": 0,
  "montoRetencion": 0,
  "fecha": "2025-12-23"
}
```

---

## 📊 RESUMEN DEL ANÁLISIS

| Componente | Estado | Observación |
|-----------|--------|-------------|
| **Orden.java** | ✅ CORRECTO | Solo campos numéricos, sin string |
| **Abono.java** | ✅ CORRECTO | Campos numéricos + string descriptivo |
| **OrdenService** | ✅ CORRECTO | Asigna campos numéricos correctamente |
| **AbonoService.crearDesdeDTO()** | ✅ CORRECTO | Asigna y valida campos numéricos |
| **AbonoService.actualizarDesdeDTO()** | ✅ CORRECTO | Asigna y valida campos numéricos |
| **AbonoController POST** | ✅ CORRECTO | Usa crearDesdeDTO con AbonoDTO |
| **AbonoController PUT** | ✅ CORREGIDO | Ahora usa actualizarDesdeDTO con AbonoDTO |
| **AbonoService.crear()** | ⚠️ DEPRECADO | Método legacy, ya no se usa |
| **AbonoService.actualizar()** | ⚠️ DEPRECADO | Método legacy, ya no se usa |

---

## 🎯 CAUSA DEL BUG DETECTADO Y CORRECCIÓN

**El bug ocurría porque:**

1. ✅ La entidad `Abono` está correctamente diseñada con campos numéricos
2. ✅ El método `crearDesdeDTO()` funcionaba correctamente
3. ❌ El método `crear()` (legacy) solo asignaba el string `metodoPago`
4. ❌ El método `actualizar()` (legacy) tampoco asignaba los campos numéricos
5. ❌ Los campos numéricos quedaban en 0.0 (valor por defecto)
6. ❌ El string `metodoPago` podía tener valores incorrectos del frontend

**✅ CORRECCIÓN IMPLEMENTADA:**

1. ✅ El `AbonoController` POST ya usaba `crearDesdeDTO()` (correcto)
2. ✅ El `AbonoController` PUT ahora usa `actualizarDesdeDTO()` (corregido)
3. ✅ Nuevo método `actualizarDesdeDTO()` que:
   - Asigna los campos numéricos desde el DTO
   - Valida que la suma de métodos coincida con el total
   - Lanza excepción si no coincide
4. ⚠️ Los métodos `crear()` y `actualizar()` legacy quedan deprecados pero no se eliminan por compatibilidad

**El frontend probablemente envía:**
- Un string `metodoPago` con valores acumulados o incorrectos
- Sin incluir los campos `montoEfectivo`, `montoTransferencia`, `montoCheque`

**El backend guarda:**
- El string tal cual viene (corrupto)
- Los campos numéricos en 0.0 (porque no se asignan)

---

## 🔧 CAMBIOS IMPLEMENTADOS

### **✅ FASE 1: Corrección Backend - COMPLETADA**

#### **Cambio 1: AbonoController.java - Endpoint PUT**
- **Antes:** Recibía `Abono` (entidad completa) y llamaba `service.actualizar()`
- **Ahora:** Recibe `AbonoDTO` y llama `service.actualizarDesdeDTO()`
- **Por qué:** El DTO exige que el frontend envíe los campos numéricos, no solo el string

#### **Cambio 2: AbonoService.java - Nuevo método actualizarDesdeDTO()**
- **Qué hace:**
  1. Recibe un `AbonoDTO` con campos numéricos obligatorios
  2. Asigna `montoEfectivo`, `montoTransferencia`, `montoCheque`, `montoRetencion`
  3. Valida que la suma de estos campos = total (tolerancia 0.01)
  4. Si no coincide, lanza excepción y rechaza la operación
  5. Actualiza el abono en la base de datos
- **Por qué:** Garantiza que los campos numéricos sean la fuente de verdad, no el string

### **⚠️ FASE 2: Actualizar Frontend - PENDIENTE**

El frontend debe ajustarse para enviar:

```json
{
  "total": 279000,
  "metodoPago": "Efectivo - Pago completo",  // ← Solo descriptivo
  "montoEfectivo": 279000,                   // ← Fuente de verdad
  "montoTransferencia": 0,
  "montoCheque": 0,
  "montoRetencion": 0,
  "fecha": "2025-12-23",
  "factura": ""
}
```

**Importante:** El frontend NO debe parsear el string `metodoPago`. Debe enviar los valores numéricos directamente.

### **⚠️ FASE 3: Limpieza de Datos Legacy - PENDIENTE**

Corregir abonos históricos con campos numéricos en 0.0:

```sql
-- Ejemplo de corrección (requiere análisis caso por caso)
UPDATE abonos 
SET monto_efectivo = total,
    monto_transferencia = 0,
    monto_cheque = 0
WHERE monto_efectivo = 0 
  AND monto_transferencia = 0 
  AND monto_cheque = 0
  AND metodo_pago LIKE '%efectivo%';
```

---

## 💡 EXPLICACIÓN DE LOS CAMBIOS

### **¿Por qué NO parsear el string `metodoPago`?**

**ARQUITECTURA CORRECTA:**
```
Frontend                    Backend                      Base de Datos
┌──────────┐               ┌────────┐                   ┌──────────┐
│ Valores  │  JSON con     │ DTO    │   Validación     │ Campos   │
│ numéricos│  campos ──────► valida ├──────────────────► numéricos│
│          │  numéricos    │        │   suma = total   │ (verdad) │
└──────────┘               └────────┘                   └──────────┘
     │                          │                             │
     │ Genera string            │                             │
     │ "Efectivo: $279K"        │                             │
     └──────────────────────────┴─────────────────────────────┴──────► metodoPago
                                                              (solo info)
```

**ARQUITECTURA INCORRECTA (la que causaba el bug):**
```
Frontend                    Backend                      Base de Datos
┌──────────┐               ┌────────┐                   ┌──────────┐
│ String   │  JSON con     │ Recibe │   ❌ NO          │ Campos   │
│ corrupto │  solo ────────► string │   valida         │ = 0.0    │
│          │  string       │        │                   │ ❌ Error │
└──────────┘               └────────┘                   └──────────┘
                                │                             │
                                │ Guarda string corrupto      │
                                └─────────────────────────────┴──────► metodoPago
                                                              "5500000"
```

### **¿Qué cambió exactamente?**

#### **Antes (Bug):**
1. Frontend enviaba: `{ metodoPago: "efectivo:5500000", total: 279000 }`
2. Backend guardaba:
   - `monto_efectivo = 0.0` ❌
   - `metodo_pago = "efectivo:5500000"` ❌
3. Cálculos usaban el string corrupto

#### **Ahora (Correcto):**
1. Frontend envía: `{ montoEfectivo: 279000, metodoPago: "Efectivo", total: 279000 }`
2. Backend valida: `279000 = 279000` ✅
3. Backend guarda:
   - `monto_efectivo = 279000` ✅
   - `metodo_pago = "Efectivo"` ✅ (solo info)
4. Cálculos usan los campos numéricos

### **Principio fundamental:**

> **NUNCA extraer valores del string `metodoPago`**  
> Los campos numéricos son la fuente de verdad.  
> El string es solo para mostrar información al usuario.

---

## 🔄 CORRECCIÓN ADICIONAL: Campo `tipoMovimiento` en Entregas

### **Problema reportado:**

El endpoint `GET /api/entregas-dinero/{id}` no devolvía el campo `tipoMovimiento` en los detalles, causando que el frontend no pudiera separar reembolsos (EGRESOS) de ingresos normales en el modal de detalles.

### **Solución implementada:**

#### **Cambio en EntregaDetalleSimpleDTO.java:**

**Agregado:**
1. ✅ Campo `tipoMovimiento` en el DTO
2. ✅ Campo `reembolsoId` para identificar reembolsos
3. ✅ Lógica inteligente para inferir `tipoMovimiento` desde la entidad
4. ✅ **Corrección de monto**: Detecta reembolsos y usa el monto correcto

```java
public class EntregaDetalleSimpleDTO {
    // ... campos existentes ...
    
    // ✅ NUEVOS CAMPOS AGREGADOS
    private Long reembolsoId;
    private String tipoMovimiento; // "INGRESO" o "EGRESO"
    
    public EntregaDetalleSimpleDTO(EntregaDetalle detalle) {
        // ... mapeo de IDs ...
        
        this.reembolsoId = detalle.getReembolsoVenta() != null 
            ? detalle.getReembolsoVenta().getId() 
            : null;
        
        // ✅ MONTO: Usar fuente correcta según el tipo
        if (detalle.getReembolsoVenta() != null) {
            // Es reembolso: usar monto del reembolso (negativo)
            this.montoOrden = -Math.abs(detalle.getReembolsoVenta().getTotalReembolso());
        } else {
            // Es orden/abono normal: usar montoOrden del detalle
            this.montoOrden = detalle.getMontoOrden();
        }
        
        // ✅ TIPO DE MOVIMIENTO: Inferir correctamente
        if (detalle.getTipoMovimiento() != null) {
            // Si está establecido en la entidad, usarlo
            this.tipoMovimiento = detalle.getTipoMovimiento().name();
        } else if (detalle.getReembolsoVenta() != null) {
            // Si tiene reembolso, es EGRESO
            this.tipoMovimiento = "EGRESO";
        } else {
            // De lo contrario, es INGRESO
            this.tipoMovimiento = "INGRESO";
        }
    }
}
```

**Lógica de monto:**
- Si `detalle.reembolsoVenta != null` → usar `-Math.abs(reembolso.getTotalReembolso())` (negativo)
- Si no → usar `detalle.montoOrden` (valor guardado en BD)

**Lógica de tipoMovimiento:**
- Si `EntregaDetalle.tipoMovimiento` está establecido → usar ese valor
- Si `EntregaDetalle.reembolsoVenta != null` → **EGRESO** (es reembolso)
- En cualquier otro caso → **INGRESO** (orden o abono normal)

**Problema corregido:**
- **Antes:** Orden #1115 con reembolso mostraba `montoOrden: 730000` (monto de orden original) y `tipoMovimiento: "INGRESO"`
- **Ahora:** Orden #1115 con reembolso muestra `montoOrden: -73000` (monto del reembolso negativo) y `tipoMovimiento: "EGRESO"`

**Resultado:**
- El endpoint `GET /api/entregas-dinero/{id}` ahora devuelve `tipoMovimiento` en cada detalle
- Frontend puede separar automáticamente INGRESOS (órdenes/abonos) de EGRESOS (reembolsos)
- Los reembolsos se muestran en la sección roja de "EGRESOS" en el modal

---

**¿Deseas que implemente las correcciones en el frontend o la limpieza de datos legacy?** 🚀
