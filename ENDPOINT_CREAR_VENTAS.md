# 🛒 **ENDPOINT PARA CREAR ÓRDENES DE VENTA**

## 📋 **RESUMEN**

He creado un endpoint optimizado específicamente para realizar ventas reales desde tu frontend. Este endpoint incluye todas las validaciones necesarias, manejo de inventario automático, y respuestas detalladas para el usuario.

## 🎯 **NUEVO ENDPOINT DE VENTA**

### **URL y Método:**
```
POST /api/ordenes/venta
```

### **Estructura del JSON a enviar:**
```json
{
    "fecha": "2024-01-15",           // OPCIONAL - si no se envía, usa fecha actual
    "obra": "Casa Familia Pérez",    // OPCIONAL - descripción del proyecto
    "venta": true,                   // Por defecto true para ventas
    "credito": false,                // true si es venta a crédito
    "incluidaEntrega": false,        // true si incluye servicio de entrega
    
    "clienteId": 123,                // OBLIGATORIO - ID del cliente
    "sedeId": 1,                     // OBLIGATORIO - sede donde se realiza la venta
    "trabajadorId": 456,             // OPCIONAL - vendedor encargado
    
    "items": [                       // OBLIGATORIO - mínimo 1 producto
        {
            "productoId": 10,        // OBLIGATORIO - ID del producto
            "descripcion": "Vidrio templado 6mm personalizado", // OPCIONAL
            "cantidad": 2,           // OBLIGATORIO - cantidad a vender (min: 1)
            "precioUnitario": 150.50 // OBLIGATORIO - precio unitario (min: 0.01)
        },
        {
            "productoId": 11,
            "descripcion": "Marco de aluminio blanco",
            "cantidad": 1,
            "precioUnitario": 89.99
        }
    ]
}
```

## ✅ **RESPUESTAS DEL ENDPOINT**

### **✅ ÉXITO (200)**
```json
{
    "mensaje": "Orden de venta creada exitosamente",
    "numero": 1047,
    "orden": {
        "id": 156,
        "numero": 1047,
        "fecha": "2024-01-15",
        "obra": "Casa Familia Pérez",
        "venta": true,
        "credito": false,
        "incluidaEntrega": false,
        "estado": "ACTIVA",
        "subtotal": 390.99,
        "total": 390.99,
        "cliente": {
            "id": 123,
            "nombre": "Juan Pérez"
        },
        "sede": {
            "id": 1,
            "nombre": "Sede Principal"
        },
        "trabajador": {
            "id": 456,
            "nombre": "María García"
        },
        "items": [
            {
                "id": 234,
                "producto": {
                    "id": 10,
                    "codigo": "VT001",
                    "nombre": "Vidrio Templado"
                },
                "descripcion": "Vidrio templado 6mm personalizado",
                "cantidad": 2,
                "precioUnitario": 150.50,
                "totalLinea": 301.00
            },
            {
                "id": 235,
                "producto": {
                    "id": 11,
                    "codigo": "MA001",
                    "nombre": "Marco Aluminio"
                },
                "descripcion": "Marco de aluminio blanco",
                "cantidad": 1,
                "precioUnitario": 89.99,
                "totalLinea": 89.99
            }
        ]
    }
}
```

### **❌ ERROR DE VALIDACIÓN (400)**
```json
{
    "error": "El cliente es obligatorio para realizar una venta",
    "tipo": "VALIDACION"
}
```

### **❌ ERROR DE INVENTARIO (400)**
```json
{
    "error": "Stock insuficiente para producto ID 10 en sede ID 1. Disponible: 1, Requerido: 2",
    "tipo": "VALIDACION"
}
```

### **❌ ERROR DEL SERVIDOR (500)**
```json
{
    "error": "Error interno del servidor: ...",
    "tipo": "SERVIDOR"
}
```

## 🔧 **FUNCIONALIDADES AUTOMÁTICAS**

### **✅ Lo que hace automáticamente:**
1. **Genera número de orden único** - Thread-safe para múltiples usuarios
2. **Calcula totales** - Subtotal y total de cada línea
3. **Actualiza inventario** - Resta automáticamente del stock de la sede
4. **Valida stock** - Verifica que haya suficiente inventario antes de vender
5. **Establece estado ACTIVA** - Todas las ventas inician como activas
6. **Maneja fecha** - Si no envías fecha, usa la actual

### **🔍 Validaciones que realiza:**
- Cliente obligatorio
- Sede obligatoria  
- Al menos 1 producto en la venta
- Cantidad > 0 para cada producto
- Precio unitario > 0 para cada producto
- Existencia del producto en el inventario de la sede
- Stock suficiente para la cantidad solicitada

## 🔗 **EJEMPLO DE CÓDIGO FRONTEND**

### **JavaScript/React:**
```javascript
// OrdenesService.js
export const crearOrdenVenta = async (ventaData) => {
    const response = await fetch(`${API_BASE}/ordenes/venta`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(ventaData)
    });
    
    const result = await response.json();
    
    if (!response.ok) {
        throw new Error(result.error || 'Error al crear la venta');
    }
    
    return result;
};

// Usar en componente
const realizarVenta = async () => {
    try {
        const ventaData = {
            clienteId: selectedCliente.id,
            sedeId: selectedSede.id,
            trabajadorId: currentUser.id, // vendedor actual
            obra: "Casa ejemplo",
            credito: esCredito,
            incluidaEntrega: incluyeEntrega,
            items: productosCarrito.map(p => ({
                productoId: p.id,
                descripcion: p.descripcionPersonalizada,
                cantidad: p.cantidad,
                precioUnitario: p.precio
            }))
        };
        
        const resultado = await crearOrdenVenta(ventaData);
        
        alert(`¡Venta exitosa! Número de orden: ${resultado.numero}`);
        
        // Limpiar carrito, redirigir, etc.
        limpiarCarrito();
        
    } catch (error) {
        alert(`Error: ${error.message}`);
    }
};
```

## 🆚 **COMPARACIÓN CON ENDPOINT ANTERIOR**

| Aspecto | Endpoint Anterior | Nuevo Endpoint de Venta |
|---------|------------------|------------------------|
| **URL** | `POST /api/ordenes` | `POST /api/ordenes/venta` |
| **DTO** | Entidad completa | DTO optimizado para ventas |
| **Validaciones** | Básicas | Completas para ventas reales |
| **Manejo de errores** | Genérico | Detallado con tipos de error |
| **Inventario** | Sí (automático) | Sí (con validaciones mejoradas) |
| **Respuesta** | Solo entidad | Mensaje + entidad + número |

## 🔄 **COMPATIBILIDAD**

- ✅ **Endpoint anterior mantiene funcionando** - `POST /api/ordenes`
- ✅ **Sin cambios en endpoints existentes** - Todo sigue funcionando
- ✅ **Nuevo endpoint es adicional** - No rompe código existente

¡El endpoint está listo para que implementes ventas reales en tu frontend! 🚀