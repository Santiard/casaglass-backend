# 📋 FLUJO DE CREACIÓN DE FACTURA - PASO A PASO

## FECHA: 2025-01-XX
## ANÁLISIS DEL PROBLEMA ACTUAL

---

## 🔍 PROBLEMA IDENTIFICADO

Los valores de la tabla de facturas **NO coinciden** con los valores de la orden porque:

1. **El frontend calcula los valores** y los envía en el payload
2. **El backend acepta esos valores** del frontend y los guarda directamente
3. **No hay validación** de que los valores coincidan con los de la orden
4. **No hay recálculo** desde los valores de la orden

---

## 📥 PASO 1: EL FRONTEND ENVÍA EL PAYLOAD

### Endpoint llamado:
```
POST /api/facturas
```

### Payload que envía el frontend (`FacturaCreateDTO`):
```json
{
  "ordenId": 125,
  "clienteId": 5,              // Opcional
  "fecha": "2025-01-15",        // Opcional (default: hoy)
  "subtotal": 1827731.09,       // ⚠️ Calculado en el frontend
  "descuentos": 0.0,
  "iva": 291823.0,              // ⚠️ Calculado en el frontend (INCORRECTO)
  "retencionFuente": 38398.0,   // ⚠️ Calculado en el frontend (INCORRECTO)
  "total": 1789333.0,           // ⚠️ Calculado en el frontend (INCORRECTO)
  "formaPago": "EFECTIVO",
  "observaciones": "...",
  "numeroFactura": null         // Opcional (se genera automáticamente)
}
```

**⚠️ PROBLEMA:** El frontend está calculando estos valores con fórmulas propias que pueden diferir del backend.

---

## 🔄 PASO 2: EL BACKEND RECIBE EL PAYLOAD

### Archivo: `FacturaController.java`
```java
@PostMapping
public ResponseEntity<?> crearFactura(@RequestBody FacturaCreateDTO facturaDTO) {
    Factura factura = facturaService.crearFactura(facturaDTO);
    return ResponseEntity.ok(...);
}
```

**Acción:** El controlador simplemente pasa el DTO al servicio sin validar.

---

## 🧮 PASO 3: EL SERVICIO PROCESA EL DTO

### Archivo: `FacturaService.java` → Método `crearFactura()`

#### 3.1. Validaciones iniciales:
```java
// ✅ Validar que no exista ya una factura para esta orden
Optional<Factura> facturaExistente = facturaRepo.findByOrdenId(facturaDTO.getOrdenId());
if (facturaExistente.isPresent()) {
    throw new IllegalArgumentException("Ya existe una factura para la orden " + facturaDTO.getOrdenId());
}

// ✅ Buscar orden existente
Orden orden = ordenRepository.findById(facturaDTO.getOrdenId())
    .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada con ID: " + facturaDTO.getOrdenId()));

// ✅ Verificar que la orden esté activa
if (orden.getEstado() == Orden.EstadoOrden.ANULADA) {
    throw new IllegalArgumentException("No se puede facturar una orden anulada");
}
```

#### 3.2. Buscar cliente (opcional):
```java
Cliente cliente = null;
if (facturaDTO.getClienteId() != null) {
    cliente = clienteRepository.findById(facturaDTO.getClienteId())
        .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + facturaDTO.getClienteId()));
}
```

#### 3.3. Crear la entidad Factura:
```java
Factura factura = new Factura();
factura.setOrden(orden);
factura.setCliente(cliente);
factura.setFecha(facturaDTO.getFecha() != null ? facturaDTO.getFecha() : LocalDate.now());
```

#### 3.4. ⚠️ **ASIGNAR VALORES DEL DTO DIRECTAMENTE** (SIN VALIDAR CON LA ORDEN):

```java
// ⚠️ PROBLEMA: Se usa directamente el subtotal del DTO sin validar con la orden
factura.setSubtotal(facturaDTO.getSubtotal());

// ⚠️ PROBLEMA: Se usa directamente el descuento del DTO sin validar con la orden
factura.setDescuentos(facturaDTO.getDescuentos() != null ? facturaDTO.getDescuentos() : 0.0);
```

#### 3.5. Cálculo del IVA (CON LÓGICA CONDICIONAL):

```java
// Calcular IVA: si viene en el DTO se usa, si no se calcula desde el subtotal
if (facturaDTO.getIva() != null && facturaDTO.getIva() > 0) {
    // ⚠️ PROBLEMA: Si el frontend envía IVA, se usa directamente SIN VALIDAR
    factura.setIva(facturaDTO.getIva());
} else {
    // ✅ Si NO viene IVA, se calcula correctamente desde el subtotal
    Double ivaCalculado = calcularIvaDesdeSubtotal(facturaDTO.getSubtotal());
    factura.setIva(ivaCalculado);
}
```

**⚠️ PROBLEMA:** Si el frontend envía `iva > 0`, se acepta sin validar si es correcto.

**Fórmula usada cuando se calcula:**
```java
private Double calcularIvaDesdeSubtotal(Double subtotal) {
    Double ivaRate = obtenerIvaRate(); // Obtiene desde BusinessSettings (default: 19%)
    // Fórmula: IVA = Subtotal * (tasa / (100 + tasa))
    // Ejemplo: Si subtotal = 2.175.000 y tasa = 19%, entonces:
    // IVA = 2.175.000 * (19 / 119) = 2.175.000 * 0.159663... = 347.268,91
    Double iva = subtotal * (ivaRate / (100.0 + ivaRate));
    return Math.round(iva * 100.0) / 100.0;
}
```

#### 3.6. ⚠️ **ASIGNAR RETENCIÓN DE FUENTE DEL DTO** (SIN VALIDAR):

```java
// ⚠️ PROBLEMA: Se usa directamente la retención del DTO sin validar con la orden
factura.setRetencionFuente(facturaDTO.getRetencionFuente() != null ? facturaDTO.getRetencionFuente() : 0.0);
```

#### 3.7. Otros campos:
```java
factura.setFormaPago(facturaDTO.getFormaPago());
factura.setObservaciones(facturaDTO.getObservaciones());
factura.setEstado(Factura.EstadoFactura.PENDIENTE);
```

#### 3.8. Cálculo del Total (CON LÓGICA CONDICIONAL):

```java
// Calcular total automáticamente
if (facturaDTO.getTotal() != null) {
    // ⚠️ PROBLEMA: Si el frontend envía total, se usa directamente SIN VALIDAR
    factura.setTotal(facturaDTO.getTotal());
} else {
    // ✅ Si NO viene total, se calcula correctamente
    factura.calcularTotal();
}
```

**Fórmula usada cuando se calcula (`Factura.calcularTotal()`):**
```java
public void calcularTotal() {
    double baseImponible = subtotal - descuentos;
    // El subtotal ya incluye IVA, solo se resta la retención de fuente
    double totalCalculado = baseImponible - retencionFuente;
    // Redondear a 2 decimales
    this.total = Math.round(totalCalculado * 100.0) / 100.0;
}
```

**Ejemplo de cálculo correcto:**
- Subtotal: $2.175.000 (ya incluye IVA)
- Descuentos: $0
- Retención: $45.693,28
- **Total = $2.175.000 - $0 - $45.693,28 = $2.129.306,72**

#### 3.9. Generar número de factura:
```java
if (facturaDTO.getNumeroFactura() != null && !facturaDTO.getNumeroFactura().isEmpty()) {
    factura.setNumeroFactura(facturaDTO.getNumeroFactura());
} else {
    Long siguienteNumero = generarNumeroFactura();
    factura.setNumeroFactura(String.valueOf(siguienteNumero));
}
```

#### 3.10. Guardar la factura:
```java
Factura facturaGuardada = facturaRepo.save(factura);

// Enlazar factura en la orden
try {
    orden.setFactura(facturaGuardada);
    ordenRepository.save(orden);
} catch (Exception ignore) {}
```

---

## 📊 RESUMEN DEL FLUJO ACTUAL

### Valores que el backend acepta del frontend:

| Campo | Origen | Validación |
|-------|--------|------------|
| `subtotal` | **Frontend** | ❌ **Ninguna** - Se acepta directamente |
| `descuentos` | **Frontend** | ❌ **Ninguna** - Se acepta directamente |
| `iva` | **Frontend** (si > 0) | ❌ **Ninguna** - Se acepta directamente |
| `iva` | **Backend** (si = 0 o null) | ✅ Se calcula correctamente |
| `retencionFuente` | **Frontend** | ❌ **Ninguna** - Se acepta directamente |
| `total` | **Frontend** (si viene) | ❌ **Ninguna** - Se acepta directamente |
| `total` | **Backend** (si no viene) | ✅ Se calcula correctamente |

### ⚠️ PROBLEMAS IDENTIFICADOS:

1. **No se valida que los valores coincidan con la orden**
   - El backend tiene acceso a `orden.getSubtotal()`, `orden.getIva()`, `orden.getRetencionFuente()`, pero **NO los usa**
   - Se confía completamente en los valores del frontend

2. **Inconsistencia en el cálculo del IVA**
   - Si el frontend envía `iva > 0`, se acepta sin validar
   - Si el frontend envía `iva = 0` o `null`, se calcula correctamente
   - Esto causa que diferentes facturas tengan diferentes cálculos

3. **Inconsistencia en el cálculo del total**
   - Si el frontend envía `total`, se acepta sin validar
   - Si el frontend NO envía `total`, se calcula correctamente
   - Esto causa que diferentes facturas tengan diferentes totales

4. **No se usa la retención de fuente de la orden**
   - La orden ya tiene `retencionFuente` calculada correctamente
   - El backend la ignora y usa la del frontend

---

## ✅ VALORES CORRECTOS DE LA ORDEN

Según el usuario, los valores correctos de la orden son:

```
Subtotal sin IVA: $1.827.731,09
IVA 19%:          $347.268,91
Retefuente:       $45.693,28
Total facturado:  $2.175.000,00
```

**Nota:** El "Total facturado" es el subtotal CON IVA incluido (base imponible con IVA).

---

## ❌ VALORES INCORRECTOS QUE SE ESTÁN GUARDANDO

Según el usuario, los valores que se están guardando en la factura son:

```
Subtotal:         $1.827.731    (similar pero no exacto)
IVA:              $291.823     (INCORRECTO - debería ser $347.268,91)
Retefuente:       $38.398      (INCORRECTO - debería ser $45.693,28)
Total:            $1.789.333    (INCORRECTO - debería ser $2.129.306,72)
```

**Análisis:**
- El subtotal del frontend ($1.827.731) es similar al subtotal sin IVA de la orden ($1.827.731,09)
- El IVA del frontend ($291.823) es diferente al IVA correcto ($347.268,91)
- La retención del frontend ($38.398) es diferente a la correcta ($45.693,28)
- El total del frontend ($1.789.333) es completamente diferente

---

## 🎯 CONCLUSIÓN

**El problema está en que el backend acepta valores calculados en el frontend sin validarlos con los valores de la orden.**

**Solución propuesta:**
1. **Ignorar los valores monetarios del DTO** (subtotal, iva, retencionFuente, total)
2. **Usar directamente los valores de la orden** que ya están calculados correctamente
3. **Solo usar del DTO:** ordenId, clienteId (opcional), fecha (opcional), formaPago, observaciones, numeroFactura (opcional)

**O alternativamente:**
1. **Validar que los valores del DTO coincidan** con los de la orden (con tolerancia de redondeo)
2. **Si no coinciden, usar los valores de la orden** y rechazar los del DTO con un error descriptivo

---

**Última actualización:** 2025-01-XX  
**Versión:** 1.0

