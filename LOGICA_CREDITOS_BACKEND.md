# 💳 Lógica Completa de Créditos en el Backend

## 📋 Índice
1. [Conceptos Básicos](#conceptos-básicos)
2. [Atributo `credito` del Cliente](#atributo-credito-del-cliente)
3. [Flujo de Creación de Venta a Crédito](#flujo-de-creación-de-venta-a-crédito)
4. [Validaciones](#validaciones)
5. [Modelo de Datos](#modelo-de-datos)
6. [Gestión de Créditos](#gestión-de-créditos)
7. [Abonos](#abonos)
8. [Estados de Crédito](#estados-de-crédito)
9. [Endpoints Disponibles](#endpoints-disponibles)

---

## 🎯 Conceptos Básicos

### ¿Qué es un Crédito?
Un **crédito** es un registro que se crea automáticamente cuando se realiza una **venta a crédito** (no contado). Representa una deuda del cliente que debe ser pagada posteriormente mediante **abonos**.

### Relaciones:
- **1 Cliente** → **N Créditos** (un cliente puede tener múltiples créditos)
- **1 Orden** → **1 Crédito** (cada orden de venta a crédito genera un crédito único)
- **1 Crédito** → **N Abonos** (un crédito se paga con múltiples abonos)

---

## 👤 Atributo `credito` del Cliente

### Definición en el Modelo:
```java
// Cliente.java
private Boolean credito;  // true = tiene crédito, false = no
```

### Significado:
- **`credito = true`**: El cliente **está autorizado** para realizar compras a crédito
- **`credito = false`**: El cliente **NO está autorizado** para compras a crédito
- **`credito = null`**: Se trata como `false` (sin crédito)

### ⚠️ IMPORTANTE: Validación Actual

**El backend NO valida automáticamente** si el cliente tiene `credito = true` antes de permitir una venta a crédito.

**Esto significa:**
- ✅ Puedes crear una venta a crédito para un cliente con `credito = false`
- ✅ El sistema **no bloquea** la operación
- ⚠️ Es responsabilidad del **frontend** validar esto antes de enviar la orden

### Recomendación:
Si quieres que el backend valide esto, deberías agregar la validación en `validarDatosVenta()`:

```java
// En OrdenService.validarDatosVenta()
if (ventaDTO.isCredito()) {
    Cliente cliente = clienteRepository.findById(ventaDTO.getClienteId())
        .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    
    if (cliente.getCredito() == null || !cliente.getCredito()) {
        throw new IllegalArgumentException(
            "El cliente no está autorizado para realizar compras a crédito"
        );
    }
}
```

---

## 🔄 Flujo de Creación de Venta a Crédito

### 1. Frontend Envía Orden con `credito: true`

```json
POST /api/ordenes/venta
{
  "clienteId": 1,
  "sedeId": 2,
  "credito": true,  // ← Flag que indica venta a crédito
  "items": [...],
  ...
}
```

### 2. Backend Detecta el Flag

En `OrdenController.crearOrdenVenta()`:

```java
if (ventaDTO.isCredito()) {
    ordenCreada = service.crearOrdenVentaConCredito(ventaDTO);
} else {
    ordenCreada = service.crearOrdenVenta(ventaDTO);
}
```

### 3. Proceso de Creación (Método: `crearOrdenVentaConCredito`)

**Paso 1: Validaciones Básicas**
```java
validarDatosVenta(ventaDTO);  // Valida cliente, sede, items, etc.
// ⚠️ NO valida si cliente.credito == true
```

**Paso 2: Crear Orden**
```java
Orden orden = new Orden();
orden.setCredito(true);  // Marca la orden como crédito
orden.setVenta(true);
// ... establecer relaciones y items
```

**Paso 3: Guardar Orden**
```java
Orden ordenGuardada = repo.save(orden);
```

**Paso 4: Crear Crédito Automáticamente**
```java
if (ventaDTO.isCredito()) {
    creditoService.crearCreditoParaOrden(
        ordenGuardada.getId(), 
        ventaDTO.getClienteId(), 
        ordenGuardada.getTotal()  // Monto total de la orden
    );
}
```

**Paso 5: Actualizar Inventario**
```java
actualizarInventarioPorVenta(ordenGuardada);
```

### 4. Creación del Crédito (Método: `crearCreditoParaOrden`)

```java
Credito credito = new Credito();
credito.setCliente(cliente);
credito.setOrden(orden);
credito.setFechaInicio(LocalDate.now());
credito.setTotalCredito(totalOrden);      // Ej: 150000
credito.setTotalAbonado(0.0);            // Inicialmente 0
credito.setSaldoPendiente(totalOrden);    // Ej: 150000
credito.setEstado(EstadoCredito.ABIERTO);
```

**Relación Bidireccional:**
```java
orden.setCreditoDetalle(credito);  // La orden apunta al crédito
```

---

## ✅ Validaciones

### Validaciones Actuales en `validarDatosVenta()`:

1. ✅ **Cliente obligatorio**: `clienteId != null`
2. ✅ **Sede obligatoria**: `sedeId != null`
3. ✅ **Items obligatorios**: Debe tener al menos 1 item
4. ✅ **Cantidad > 0**: Cada item debe tener cantidad > 0
5. ✅ **Precio > 0**: Cada item debe tener precio > 0

### ❌ Validaciones que NO existen:

1. ❌ **Cliente autorizado para crédito**: No se verifica `cliente.credito == true`
2. ❌ **Límite de crédito**: No hay límite máximo de crédito por cliente
3. ❌ **Créditos pendientes**: No se verifica si el cliente tiene créditos sin pagar

### 🔧 Si quieres agregar validaciones:

```java
private void validarDatosVenta(OrdenVentaDTO ventaDTO) {
    // ... validaciones existentes ...
    
    // NUEVA: Validar si cliente puede tener crédito
    if (ventaDTO.isCredito()) {
        Cliente cliente = clienteRepository.findById(ventaDTO.getClienteId())
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        
        if (cliente.getCredito() == null || !cliente.getCredito()) {
            throw new IllegalArgumentException(
                "El cliente no está autorizado para realizar compras a crédito. " +
                "Contacte al administrador para habilitar crédito."
            );
        }
        
        // OPCIONAL: Validar límite de crédito
        List<Credito> creditosAbiertos = creditoService.listarPorCliente(cliente.getId())
            .stream()
            .filter(c -> c.getEstado() == Credito.EstadoCredito.ABIERTO)
            .collect(Collectors.toList());
        
        Double totalPendiente = creditosAbiertos.stream()
            .mapToDouble(Credito::getSaldoPendiente)
            .sum();
        
        // Ejemplo: Límite de 1,000,000
        if (totalPendiente + ventaDTO.getTotal() > 1000000) {
            throw new IllegalArgumentException(
                "El cliente excede el límite de crédito permitido. " +
                "Saldo pendiente: " + totalPendiente
            );
        }
    }
}
```

---

## 📊 Modelo de Datos

### Entidad `Credito`

```java
@Entity
@Table(name = "creditos")
public class Credito {
    @Id
    private Long id;
    
    @ManyToOne
    private Cliente cliente;           // Cliente que debe
    
    @ManyToOne
    private Orden orden;               // Orden que originó el crédito
    
    private LocalDate fechaInicio;     // Fecha de creación
    private LocalDate fechaCierre;     // Fecha de cierre (cuando se paga)
    
    private Double totalCredito;       // Monto total del crédito
    private Double totalAbonado;      // Monto total pagado
    private Double saldoPendiente;     // totalCredito - totalAbonado
    
    private EstadoCredito estado;      // ABIERTO, CERRADO, VENCIDO, ANULADO
    
    @OneToMany
    private List<Abono> abonos;       // Lista de pagos realizados
}
```

### Relación con Orden

```java
// Orden.java
@OneToOne(mappedBy = "orden")
private Credito creditoDetalle;  // Crédito asociado (si existe)
```

### Relación con Cliente

```java
// Cliente.java
private Boolean credito;  // true = autorizado para crédito
```

---

## 🔧 Gestión de Créditos

### 1. Crear Crédito

**Automático**: Se crea cuando se hace una venta con `credito: true`

**Manual**: 
```java
POST /api/creditos/orden/{ordenId}?clienteId=1&totalOrden=150000
```

### 2. Actualizar Crédito

**Automático**: Cuando se actualiza una orden con crédito, se actualiza el monto del crédito:

```java
// Si cambia el total de la orden
creditoService.actualizarCreditoParaOrden(creditoId, nuevoTotal);
```

**Lógica:**
- Si la orden aumenta: `totalCredito` aumenta, `saldoPendiente` aumenta
- Si la orden disminuye: `totalCredito` disminuye, `saldoPendiente` disminuye
- `totalAbonado` se mantiene (los abonos ya pagados no cambian)

### 3. Anular Crédito

**Automático**: Cuando se anula una orden con crédito:

```java
// En anularOrden()
if (orden.getCreditoDetalle() != null) {
    creditoService.anularCredito(orden.getCreditoDetalle().getId());
}
```

**Manual**:
```java
PUT /api/creditos/{creditoId}/anular
```

### 4. Cambiar de Crédito a Contado

Si actualizas una orden y cambias `credito: false`:

```java
if (!ventaDTO.isCredito() && ordenActualizada.getCreditoDetalle() != null) {
    // Anular el crédito existente
    creditoService.anularCredito(ordenActualizada.getCreditoDetalle().getId());
}
```

---

## 💰 Abonos

### ¿Qué es un Abono?
Un **abono** es un pago parcial o total que se realiza sobre un crédito. Un crédito puede tener múltiples abonos hasta quedar completamente pagado.

### Modelo de Abono

```java
@Entity
public class Abono {
    @Id
    private Long id;
    
    @ManyToOne
    private Credito credito;        // Crédito al que aplica
    
    @ManyToOne
    private Orden orden;            // Orden relacionada (opcional)
    
    @ManyToOne
    private Cliente cliente;         // Cliente que paga
    
    private LocalDate fecha;        // Fecha del abono
    private MetodoPago metodoPago;  // EFECTIVO, TRANSFERENCIA, etc.
    private String factura;         // Número de factura/recibo
    private Double total;           // Monto del abono
    private Double saldo;           // Saldo después del abono (snapshot)
}
```

### Registrar Abono

**Endpoint:**
```java
POST /api/creditos/{creditoId}/abono?monto=50000
```

**Lógica:**
```java
// 1. Validar que el crédito esté ABIERTO
if (credito.getEstado() == EstadoCredito.CERRADO) {
    throw new IllegalArgumentException("No se pueden agregar abonos a un crédito cerrado");
}

// 2. Actualizar totales
credito.setTotalAbonado(credito.getTotalAbonado() + montoAbono);
credito.actualizarSaldo();  // Recalcula saldoPendiente y estado

// 3. Si saldoPendiente <= 0, el crédito se cierra automáticamente
if (credito.getSaldoPendiente() <= 0) {
    credito.setEstado(EstadoCredito.CERRADO);
    credito.setFechaCierre(LocalDate.now());
}
```

### Ejemplo de Abonos

```
Crédito Inicial:
- totalCredito: 150000
- totalAbonado: 0
- saldoPendiente: 150000
- estado: ABIERTO

Abono 1 (50000):
- totalAbonado: 50000
- saldoPendiente: 100000
- estado: ABIERTO

Abono 2 (100000):
- totalAbonado: 150000
- saldoPendiente: 0
- estado: CERRADO ✅
- fechaCierre: 2025-01-15
```

---

## 📈 Estados de Crédito

### Enum `EstadoCredito`

```java
public enum EstadoCredito {
    ABIERTO,    // Crédito activo con saldo pendiente
    CERRADO,    // Crédito completamente pagado
    VENCIDO,    // Crédito con pagos atrasados (no implementado aún)
    ANULADO     // Crédito cancelado (por anulación de orden)
}
```

### Transiciones de Estado

```
ABIERTO → CERRADO: Automático cuando saldoPendiente <= 0
ABIERTO → ANULADO: Cuando se anula la orden asociada
CERRADO → ABIERTO: Si se modifica el crédito y queda saldo pendiente
```

### Métodos Helper en el Modelo

```java
// Actualizar saldo y estado automáticamente
credito.actualizarSaldo();

// Verificar si está pagado
boolean pagado = credito.estaPagado();

// Obtener porcentaje pagado
double porcentaje = credito.getPorcentajePagado();  // 0-100
```

---

## 🌐 Endpoints Disponibles

### Créditos

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/creditos` | Listar todos los créditos |
| GET | `/api/creditos/{id}` | Obtener crédito por ID |
| GET | `/api/creditos/orden/{ordenId}` | Obtener crédito por orden |
| GET | `/api/creditos/cliente/{clienteId}` | Listar créditos de un cliente |
| GET | `/api/creditos/estado/{estado}` | Listar créditos por estado |
| POST | `/api/creditos/orden/{ordenId}` | Crear crédito para orden |
| POST | `/api/creditos/{creditoId}/abono` | Registrar abono |
| POST | `/api/creditos/{creditoId}/recalcular` | Recalcular totales |
| PUT | `/api/creditos/{creditoId}/anular` | Anular crédito |
| PUT | `/api/creditos/{creditoId}/cerrar` | Cerrar crédito manualmente |
| DELETE | `/api/creditos/{id}` | Eliminar crédito |

### Órdenes (con crédito)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/ordenes/venta` | Crear orden (si `credito: true`, crea crédito automáticamente) |
| PUT | `/api/ordenes/venta/{id}` | Actualizar orden (actualiza crédito si existe) |

### Abonos

Ver `AbonoController` para endpoints específicos de abonos.

---

## 🔍 Flujo Completo: Ejemplo Real

### Escenario: Cliente compra a crédito

**1. Cliente tiene `credito: true`**
```json
GET /api/clientes/1
{
  "id": 1,
  "nombre": "Juan Pérez",
  "credito": true  // ✅ Autorizado
}
```

**2. Frontend crea orden con `credito: true`**
```json
POST /api/ordenes/venta
{
  "clienteId": 1,
  "sedeId": 2,
  "credito": true,  // ← Flag de crédito
  "items": [
    {
      "productoId": 10,
      "cantidad": 5,
      "precioUnitario": 30000
    }
  ]
}
```

**3. Backend procesa:**
- ✅ Valida datos básicos
- ✅ Crea orden con `credito: true`
- ✅ Crea crédito automáticamente:
  ```json
  {
    "id": 100,
    "cliente": { "id": 1 },
    "orden": { "id": 456 },
    "totalCredito": 150000,
    "totalAbonado": 0,
    "saldoPendiente": 150000,
    "estado": "ABIERTO"
  }
  ```
- ✅ Actualiza inventario

**4. Cliente realiza abono parcial**
```json
POST /api/creditos/100/abono?monto=50000
```

**5. Crédito actualizado:**
```json
{
  "id": 100,
  "totalCredito": 150000,
  "totalAbonado": 50000,
  "saldoPendiente": 100000,
  "estado": "ABIERTO"
}
```

**6. Cliente completa el pago**
```json
POST /api/creditos/100/abono?monto=100000
```

**7. Crédito cerrado automáticamente:**
```json
{
  "id": 100,
  "totalCredito": 150000,
  "totalAbonado": 150000,
  "saldoPendiente": 0,
  "estado": "CERRADO",
  "fechaCierre": "2025-01-15"
}
```

---

## ⚠️ Puntos Importantes

### 1. Validación de Cliente
- **Actual**: El backend NO valida si `cliente.credito == true`
- **Recomendación**: Agregar validación en `validarDatosVenta()`

### 2. Unicidad
- **1 Orden = 1 Crédito**: No puede haber múltiples créditos para la misma orden
- Si intentas crear un crédito para una orden que ya tiene uno, devuelve el existente

### 3. Actualización Automática
- Si actualizas una orden con crédito, el crédito se actualiza automáticamente
- Si cambias de crédito a contado, el crédito se anula

### 4. Anulación
- Si anulas una orden con crédito, el crédito se anula automáticamente
- Un crédito anulado no puede recibir abonos

### 5. Cierre Automático
- El crédito se cierra automáticamente cuando `saldoPendiente <= 0`
- No necesitas cerrarlo manualmente

---

## 🛠️ Mejoras Sugeridas

### 1. Validar Cliente Autorizado
```java
if (ventaDTO.isCredito() && !cliente.getCredito()) {
    throw new IllegalArgumentException("Cliente no autorizado para crédito");
}
```

### 2. Límite de Crédito
```java
Double limiteCredito = 1000000.0;
Double totalPendiente = calcularTotalPendiente(clienteId);
if (totalPendiente + totalOrden > limiteCredito) {
    throw new IllegalArgumentException("Excede límite de crédito");
}
```

### 3. Historial de Créditos
- Agregar endpoint para ver historial completo de créditos de un cliente
- Incluir créditos cerrados y anulados

### 4. Reportes
- Total de créditos abiertos
- Total de saldo pendiente por cliente
- Créditos próximos a vencer (si implementas fechas límite)

---

**Fecha de documentación**: 2025-01-XX  
**Versión del backend**: Compatible con todas las versiones actuales


