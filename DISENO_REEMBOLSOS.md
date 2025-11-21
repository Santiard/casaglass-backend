# DISEÑO DE SISTEMA DE REEMBOLSOS

## 📋 RESUMEN EJECUTIVO

Este documento propone el diseño de un sistema de reembolsos para manejar:
1. **Reembolsos de Ingresos**: Devolver productos al proveedor (restar inventario)
2. **Reembolsos de Ventas**: Devolver productos del cliente (sumar inventario)

---

## 🎯 OBJETIVOS

- Rastrear devoluciones de productos tanto de proveedores como de clientes
- Mantener consistencia en el inventario
- Registrar el impacto financiero de los reembolsos
- Mantener trazabilidad completa (qué se devolvió, cuándo, por qué)
- Manejar reembolsos parciales (no necesariamente devolver todo)

---

## 📊 DISEÑO DE ENTIDADES

### 1. REEMBOLSO INGRESO (Devolución al Proveedor)

```java
@Entity
@Table(name = "reembolsos_ingreso")
public class ReembolsoIngreso {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(nullable = false)
    private LocalDate fecha;
    
    // Ingreso original que se está reembolsando
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "ingreso_id", nullable = false)
    private Ingreso ingresoOriginal;
    
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;
    
    @Column(length = 100)
    private String numeroFacturaDevolucion; // Factura de devolución del proveedor
    
    @Column(length = 500)
    private String motivo; // Razón del reembolso
    
    @OneToMany(mappedBy = "reembolsoIngreso", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReembolsoIngresoDetalle> detalles = new ArrayList<>();
    
    @Column(nullable = false)
    private Double totalReembolso = 0.0; // Total a reembolsar al proveedor
    
    @Column(nullable = false)
    private Boolean procesado = false; // Si ya se actualizó el inventario
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReembolso estado = EstadoReembolso.PENDIENTE;
    
    public enum EstadoReembolso {
        PENDIENTE,    // Creado pero no procesado
        PROCESADO,    // Inventario actualizado
        ANULADO       // Reembolso cancelado
    }
}
```

### 2. REEMBOLSO INGRESO DETALLE

```java
@Entity
@Table(name = "reembolso_ingreso_detalles")
public class ReembolsoIngresoDetalle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reembolso_ingreso_id", nullable = false)
    private ReembolsoIngreso reembolsoIngreso;
    
    // Detalle original del ingreso que se está reembolsando
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "ingreso_detalle_id", nullable = false)
    private IngresoDetalle ingresoDetalleOriginal;
    
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    
    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer cantidad; // Cantidad a devolver (puede ser parcial)
    
    @NotNull
    @Column(nullable = false)
    private Double costoUnitario; // Costo unitario al momento del reembolso
    
    @Column(nullable = false)
    private Double totalLinea; // cantidad * costoUnitario
}
```

### 3. REEMBOLSO VENTA (Devolución del Cliente)

```java
@Entity
@Table(name = "reembolsos_venta")
public class ReembolsoVenta {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(nullable = false)
    private LocalDate fecha;
    
    // Orden original que se está reembolsando
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden ordenOriginal;
    
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
    
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;
    
    @Column(length = 500)
    private String motivo; // Razón del reembolso
    
    @OneToMany(mappedBy = "reembolsoVenta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReembolsoVentaDetalle> detalles = new ArrayList<>();
    
    @Column(nullable = false)
    private Double subtotal = 0.0;
    
    @Column(nullable = false)
    private Double descuentos = 0.0; // Descuentos proporcionales
    
    @Column(nullable = false)
    private Double totalReembolso = 0.0; // Total a reembolsar al cliente
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormaReembolso formaReembolso; // Cómo se devuelve el dinero
    
    public enum FormaReembolso {
        EFECTIVO,
        TRANSFERENCIA,
        NOTA_CREDITO,      // Para aplicar a futuras compras
        AJUSTE_CREDITO     // Si la venta original fue a crédito, ajustar el saldo
    }
    
    @Column(nullable = false)
    private Boolean procesado = false; // Si ya se actualizó inventario y créditos
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReembolso estado = EstadoReembolso.PENDIENTE;
}
```

### 4. REEMBOLSO VENTA DETALLE

```java
@Entity
@Table(name = "reembolso_venta_detalles")
public class ReembolsoVentaDetalle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reembolso_venta_id", nullable = false)
    private ReembolsoVenta reembolsoVenta;
    
    // Item original de la orden que se está reembolsando
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "orden_item_id", nullable = false)
    private OrdenItem ordenItemOriginal;
    
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    
    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer cantidad; // Cantidad a devolver (puede ser parcial)
    
    @NotNull
    @Column(nullable = false)
    private Double precioUnitario; // Precio unitario al momento del reembolso
    
    @Column(nullable = false)
    private Double totalLinea; // cantidad * precioUnitario
}
```

---

## 🔄 FLUJO DE PROCESAMIENTO

### REEMBOLSO DE INGRESO

1. **Crear ReembolsoIngreso**
   - Seleccionar `Ingreso` original
   - Seleccionar productos y cantidades a devolver
   - Ingresar motivo y número de factura de devolución
   - Calcular total del reembolso

2. **Procesar ReembolsoIngreso**
   - Validar que las cantidades no excedan lo recibido
   - Restar productos del inventario (en la sede donde estaban)
   - Marcar como `PROCESADO`
   - Opcional: Ajustar costo del producto si es necesario

### REEMBOLSO DE VENTA

1. **Crear ReembolsoVenta**
   - Seleccionar `Orden` original
   - Seleccionar productos y cantidades a devolver
   - Ingresar motivo
   - Seleccionar forma de reembolso (efectivo, transferencia, nota crédito, ajuste crédito)
   - Calcular total del reembolso (con descuentos proporcionales)

2. **Procesar ReembolsoVenta**
   - Validar que las cantidades no excedan lo vendido
   - Sumar productos al inventario (en la sede de la venta)
   - Si la venta original fue a crédito:
     - Ajustar el saldo del crédito (reducir deuda)
   - Si la venta original fue facturada:
     - Opcional: Crear nota de crédito o ajuste de factura
   - Marcar como `PROCESADO`

---

## 📝 DTOs PROPUESTOS

### ReembolsoIngresoCreateDTO

```java
public class ReembolsoIngresoCreateDTO {
    private Long ingresoId; // Ingreso original
    private LocalDate fecha;
    private String numeroFacturaDevolucion;
    private String motivo;
    private List<ReembolsoIngresoDetalleDTO> detalles;
    
    public static class ReembolsoIngresoDetalleDTO {
        private Long ingresoDetalleId; // Detalle original
        private Integer cantidad; // Cantidad a devolver
    }
}
```

### ReembolsoVentaCreateDTO

```java
public class ReembolsoVentaCreateDTO {
    private Long ordenId; // Orden original
    private LocalDate fecha;
    private String motivo;
    private FormaReembolso formaReembolso;
    private List<ReembolsoVentaDetalleDTO> detalles;
    
    public static class ReembolsoVentaDetalleDTO {
        private Long ordenItemId; // Item original
        private Integer cantidad; // Cantidad a devolver
    }
}
```

---

## 🔧 SERVICIOS PROPUESTOS

### ReembolsoIngresoService

```java
@Service
@Transactional
public class ReembolsoIngresoService {
    
    // Crear reembolso (sin procesar)
    public ReembolsoIngreso crearReembolso(ReembolsoIngresoCreateDTO dto);
    
    // Procesar reembolso (actualizar inventario)
    public void procesarReembolso(Long reembolsoId);
    
    // Listar reembolsos
    public List<ReembolsoIngreso> listarReembolsos();
    
    // Obtener reembolso por ID
    public Optional<ReembolsoIngreso> obtenerPorId(Long id);
    
    // Anular reembolso
    public void anularReembolso(Long reembolsoId);
}
```

### ReembolsoVentaService

```java
@Service
@Transactional
public class ReembolsoVentaService {
    
    // Crear reembolso (sin procesar)
    public ReembolsoVenta crearReembolso(ReembolsoVentaCreateDTO dto);
    
    // Procesar reembolso (actualizar inventario y créditos)
    public void procesarReembolso(Long reembolsoId);
    
    // Listar reembolsos
    public List<ReembolsoVenta> listarReembolsos();
    
    // Obtener reembolsos por orden
    public List<ReembolsoVenta> obtenerReembolsosPorOrden(Long ordenId);
    
    // Anular reembolso
    public void anularReembolso(Long reembolsoId);
}
```

---

## 🌐 ENDPOINTS PROPUESTOS

### Reembolsos de Ingreso

```
POST   /api/reembolsos-ingreso              - Crear reembolso
GET    /api/reembolsos-ingreso              - Listar todos
GET    /api/reembolsos-ingreso/{id}         - Obtener por ID
GET    /api/reembolsos-ingreso/ingreso/{ingresoId} - Reembolsos de un ingreso
PUT    /api/reembolsos-ingreso/{id}/procesar - Procesar reembolso
PUT    /api/reembolsos-ingreso/{id}/anular  - Anular reembolso
DELETE /api/reembolsos-ingreso/{id}         - Eliminar reembolso (solo si no procesado)
```

### Reembolsos de Venta

```
POST   /api/reembolsos-venta                - Crear reembolso
GET    /api/reembolsos-venta                - Listar todos
GET    /api/reembolsos-venta/{id}           - Obtener por ID
GET    /api/reembolsos-venta/orden/{ordenId} - Reembolsos de una orden
PUT    /api/reembolsos-venta/{id}/procesar  - Procesar reembolso
PUT    /api/reembolsos-venta/{id}/anular    - Anular reembolso
DELETE /api/reembolsos-venta/{id}           - Eliminar reembolso (solo si no procesado)
```

---

## ⚠️ VALIDACIONES IMPORTANTES

### Reembolso de Ingreso
- ✅ La cantidad a devolver no puede exceder la cantidad recibida en el ingreso original
- ✅ El ingreso original debe estar procesado
- ✅ No se puede procesar un reembolso ya procesado
- ✅ No se puede anular un reembolso ya procesado (solo crear uno nuevo para revertir)

### Reembolso de Venta
- ✅ La cantidad a devolver no puede exceder la cantidad vendida en la orden original
- ✅ La orden original debe estar activa (no anulada)
- ✅ Si la venta fue a crédito, el reembolso debe ajustar el saldo del crédito
- ✅ Si la venta fue facturada, se debe considerar crear nota de crédito
- ✅ No se puede procesar un reembolso ya procesado

---

## 💡 CONSIDERACIONES ADICIONALES

### 1. **Reembolsos Parciales**
- Permitir devolver solo algunos productos de un ingreso/venta
- Permitir devolver cantidades parciales de un producto

### 2. **Trazabilidad**
- Mantener referencia al ingreso/orden original
- Mantener referencia al detalle original
- Registrar fecha, motivo y usuario que procesa

### 3. **Impacto en Inventario**
- Reembolso de ingreso: RESTA del inventario
- Reembolso de venta: SUMA al inventario
- Considerar la sede correcta en ambos casos

### 4. **Impacto Financiero**
- Reembolso de ingreso: Reduce el costo total de compras
- Reembolso de venta: Reduce ingresos y puede afectar créditos

### 5. **Integración con Facturación**
- Si una venta facturada tiene reembolso, considerar:
  - Crear nota de crédito
  - Ajustar factura original (si es permitido)
  - Reportar a contabilidad

### 6. **Integración con Créditos**
- Si una venta a crédito tiene reembolso:
  - Reducir el saldo del crédito proporcionalmente
  - Ajustar abonos si es necesario
  - Cerrar crédito si el saldo llega a 0

---

## 📊 EJEMPLOS DE USO

### Ejemplo 1: Reembolso de Ingreso Parcial

**Escenario**: Se recibieron 100 unidades de un producto, pero 10 están defectuosas.

1. Crear `ReembolsoIngreso` para el ingreso original
2. Agregar detalle: 10 unidades del producto defectuoso
3. Procesar reembolso → Resta 10 unidades del inventario
4. El proveedor reembolsa el costo de las 10 unidades

### Ejemplo 2: Reembolso de Venta Completa

**Escenario**: Cliente devuelve toda una orden porque los productos no cumplen especificaciones.

1. Crear `ReembolsoVenta` para la orden original
2. Agregar todos los items de la orden
3. Seleccionar forma de reembolso: EFECTIVO
4. Procesar reembolso:
   - Suma productos al inventario
   - Si fue a crédito, ajusta el saldo del crédito
   - Si fue facturada, crear nota de crédito

### Ejemplo 3: Reembolso Parcial de Venta a Crédito

**Escenario**: Cliente devuelve 2 de 5 productos de una venta a crédito.

1. Crear `ReembolsoVenta` para la orden original
2. Agregar solo los 2 productos a devolver
3. Seleccionar forma de reembolso: AJUSTE_CREDITO
4. Procesar reembolso:
   - Suma 2 productos al inventario
   - Reduce el saldo del crédito proporcionalmente
   - El cliente ahora debe menos dinero

---

## 🚀 PLAN DE IMPLEMENTACIÓN

### Fase 1: Entidades y Repositorios
1. Crear entidades `ReembolsoIngreso`, `ReembolsoIngresoDetalle`
2. Crear entidades `ReembolsoVenta`, `ReembolsoVentaDetalle`
3. Crear repositorios para todas las entidades
4. Crear migraciones de base de datos

### Fase 2: Servicios
1. Implementar `ReembolsoIngresoService`
2. Implementar `ReembolsoVentaService`
3. Integrar con `InventarioService`
4. Integrar con `CreditoService` (para reembolsos de venta)

### Fase 3: Controladores y DTOs
1. Crear DTOs de creación y respuesta
2. Implementar `ReembolsoIngresoController`
3. Implementar `ReembolsoVentaController`
4. Agregar validaciones

### Fase 4: Testing
1. Tests unitarios de servicios
2. Tests de integración de endpoints
3. Validar impacto en inventario
4. Validar impacto en créditos

---

## ❓ PREGUNTAS PARA DEFINIR

1. **¿Se pueden reembolsar ingresos/ventas ya procesados?**
   - Respuesta propuesta: SÍ, pero con validaciones estrictas

2. **¿Qué pasa con el costo del producto cuando se reembolsa un ingreso?**
   - Opción A: Mantener el costo original
   - Opción B: Recalcular costo promedio
   - Respuesta propuesta: Mantener costo original (más simple)

3. **¿Se pueden reembolsar ventas facturadas?**
   - Respuesta propuesta: SÍ, pero requiere crear nota de crédito

4. **¿Se pueden reembolsar ventas a crédito ya saldadas?**
   - Respuesta propuesta: SÍ, pero el ajuste se aplica como crédito a favor del cliente

5. **¿Se pueden anular reembolsos ya procesados?**
   - Respuesta propuesta: NO, solo crear un reembolso inverso

---

## 📝 NOTAS FINALES

- Este diseño es **modular** y **extensible**
- Mantiene **trazabilidad completa** de todas las operaciones
- Es **consistente** con el diseño actual del sistema
- Permite **reembolsos parciales** y **completos**
- Considera **impacto financiero** y **de inventario**

¿Quieres que implemente alguna parte específica de este diseño?

