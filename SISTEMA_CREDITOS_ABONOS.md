# 💳 **SISTEMA DE CRÉDITOS Y ABONOS - NUEVO MODELO**

## 🎯 **ARQUITECTURA MEJORADA**

He implementado un sistema de créditos completamente renovado que separa la lógica de créditos como entidad independiente, siguiendo las mejores prácticas de modelado de datos.

### **🔄 CAMBIO FUNDAMENTAL**
- **Antes**: Campo boolean `credito` en `Orden`
- **Ahora**: Entidad `Credito` independiente con relación 1:1 con `Orden`

## 📊 **MODELO DE ENTIDADES**

### **💳 Entidad `Credito`**
```java
@Entity
public class Credito {
    private Long id;
    private Cliente cliente;          // Cliente del crédito
    private Orden orden;             // Orden que originó el crédito (1:1)
    private LocalDate fechaInicio;   // Fecha de creación del crédito
    private LocalDate fechaCierre;   // Fecha de cierre (cuando se completa)
    private Double totalCredito;     // Monto total inicial (= orden.total)
    private Double totalAbonado;     // Suma de todos los abonos
    private Double saldoPendiente;   // totalCredito - totalAbonado
    private EstadoCredito estado;    // ABIERTO, CERRADO, VENCIDO, ANULADO
    private String observaciones;    // Notas adicionales
    private List<Abono> abonos;     // Lista de abonos realizados
}
```

### **💰 Entidad `Abono`**
```java
@Entity
public class Abono {
    private Long id;
    private Credito credito;         // Crédito al que aplica
    private Cliente cliente;         // Cliente (consistencia)
    private Orden orden;            // Orden específica (= credito.orden)
    private Long numeroOrden;       // Snapshot del número de orden
    private LocalDate fecha;        // Fecha del abono
    private MetodoPago metodoPago;  // EFECTIVO, TRANSFERENCIA, etc.
    private String factura;         // Número de comprobante
    private Double total;           // Monto del abono
    private Double saldo;           // Saldo después del abono (snapshot)
}
```

### **📋 Entidad `Orden` (Actualizada)**
```java
@Entity
public class Orden {
    // ... campos existentes ...
    private boolean credito = false;         // Mantiene compatibilidad
    private Credito creditoDetalle;         // Relación con entidad Credito
}
```

## 🔧 **SERVICIOS DISPONIBLES**

### **🏪 CreditoService**
- `crearCreditoParaOrden()` - Crear crédito para una orden
- `registrarAbono()` - Registrar abono y actualizar totales
- `recalcularTotales()` - Sincronizar totales después de cambios
- `anularCredito()` - Anular crédito (por anulación de orden)
- `cerrarCredito()` - Cerrar crédito manualmente

### **💰 AbonoService**
- `crear()` - Crear abono con validaciones completas
- `actualizar()` - Modificar abono existente
- `eliminar()` - Eliminar abono y recalcular

## 🌐 **ENDPOINTS DISPONIBLES**

### **💳 CRÉDITOS**
```
POST   /api/creditos/orden/{ordenId}     - Crear crédito para orden
GET    /api/creditos                     - Listar todos los créditos
GET    /api/creditos/{id}                - Obtener crédito por ID
GET    /api/creditos/orden/{ordenId}     - Obtener crédito de una orden
GET    /api/creditos/cliente/{clienteId} - Listar créditos de un cliente
GET    /api/creditos/estado/{estado}     - Listar créditos por estado
POST   /api/creditos/{id}/abono          - Registrar abono
POST   /api/creditos/{id}/recalcular     - Recalcular totales
PUT    /api/creditos/{id}/anular         - Anular crédito
PUT    /api/creditos/{id}/cerrar         - Cerrar crédito
DELETE /api/creditos/{id}                - Eliminar crédito
```

### **💰 ABONOS**
```
GET    /api/abonos/credito/{creditoId}   - Listar abonos de un crédito
GET    /api/abonos/cliente/{clienteId}   - Listar abonos de un cliente
POST   /api/abonos/credito/{creditoId}   - Crear abono
PUT    /api/abonos/credito/{creditoId}/abono/{abonoId} - Actualizar abono
DELETE /api/abonos/credito/{creditoId}/abono/{abonoId} - Eliminar abono
```

## 📝 **EJEMPLOS DE USO**

### **1. Crear crédito para una orden**
```bash
POST /api/creditos/orden/123?clienteId=456&totalOrden=1500.50
```

**Respuesta:**
```json
{
    "mensaje": "Crédito creado exitosamente",
    "credito": {
        "id": 78,
        "fechaInicio": "2024-01-15",
        "totalCredito": 1500.50,
        "totalAbonado": 0.0,
        "saldoPendiente": 1500.50,
        "estado": "ABIERTO",
        "cliente": { "id": 456, "nombre": "Juan Pérez" },
        "orden": { "id": 123, "numero": 1047 }
    }
}
```

### **2. Registrar abono**
```bash
POST /api/creditos/78/abono?monto=500.00
```

**Respuesta:**
```json
{
    "mensaje": "Abono registrado exitosamente",
    "credito": {
        "id": 78,
        "totalAbonado": 500.00,
        "saldoPendiente": 1000.50,
        "estado": "ABIERTO"
    },
    "nuevoSaldo": 1000.50
}
```

### **3. Crear abono detallado**
```bash
POST /api/abonos/credito/78
Content-Type: application/json

{
    "fecha": "2024-01-20",
    "total": 500.00,
    "metodoPago": "TRANSFERENCIA",
    "factura": "TF-2024-001"
}
```

## 🔄 **FLUJO COMPLETO DE VENTA A CRÉDITO**

1. **Crear orden de venta** con `credito: true`
2. **Sistema crea crédito automáticamente** usando el nuevo endpoint
3. **Cliente realiza abonos** periódicos
4. **Sistema actualiza totales** automáticamente
5. **Crédito se cierra** cuando `saldoPendiente = 0`

## ✨ **FUNCIONALIDADES AUTOMÁTICAS**

### **🔄 Cálculos Automáticos**
- `saldoPendiente = totalCredito - totalAbonado`
- Estado se actualiza automáticamente a CERRADO cuando saldo = 0
- Fecha de cierre se establece automáticamente

### **🔒 Validaciones**
- No se pueden agregar abonos a créditos cerrados o anulados
- Los abonos no pueden exceder el saldo pendiente
- Consistencia de cliente entre orden, crédito y abonos

### **📊 Métodos Helper**
- `estaPagado()` - Verifica si está completamente pagado
- `getPorcentajePagado()` - Calcula porcentaje pagado
- `actualizarSaldo()` - Recalcula y actualiza estado

## 🎯 **VENTAJAS DEL NUEVO MODELO**

1. **✅ Trazabilidad completa** - Cada orden tiene su propio crédito
2. **✅ Historial detallado** - Todos los abonos quedan registrados
3. **✅ Estados claros** - ABIERTO, CERRADO, VENCIDO, ANULADO
4. **✅ Cálculos automáticos** - Sin errores manuales
5. **✅ Escalabilidad** - Soporte para múltiples créditos por cliente
6. **✅ Auditoría** - Snapshots de saldos en cada abono
7. **✅ Compatibilidad** - Mantiene campo boolean para transición gradual

## 🚀 **¡SISTEMA LISTO PARA PRODUCCIÓN!**

El nuevo sistema de créditos está completamente implementado y listo para manejar ventas a crédito de manera profesional y escalable.