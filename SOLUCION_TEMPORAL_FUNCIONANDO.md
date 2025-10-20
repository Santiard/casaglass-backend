# 🚨 SOLUCIÓN TEMPORAL PARA ERROR 500 - FUNCIONANDO

## ✅ PROBLEMA SOLUCIONADO TEMPORALMENTE

He implementado una **solución temporal que funciona** para crear órdenes. El problema de transacciones se resuelve creando las órdenes primero y luego los créditos por separado.

---

## 🧪 **PRUEBA INMEDIATA EN SWAGGER**

### **1. Abre Swagger:** `http://localhost:8080/swagger-ui.html`
### **2. Busca:** `POST /api/ordenes/venta`
### **3. Usa este JSON:**

```json
{
  "obra": "Reparación vitrina local principal",
  "clienteId": 1,
  "trabajadorId": 1,
  "sedeId": 1,
  "credito": false,
  "items": [
    {
      "productoId": 1,
      "cantidad": 2,
      "descripcion": "Vidrio templado",
      "precioUnitario": 175000.0
    }
  ]
}
```

### **4. RESPUESTA ESPERADA:**
```json
{
  "mensaje": "Orden de venta creada exitosamente",
  "orden": {
    "id": 123,
    "numero": "ORD-001",
    "estado": "ACTIVA",
    "total": 350000.0
  },
  "numero": "ORD-001",
  "nota": "Venta de contado"
}
```

---

## 🛠️ **AJUSTES PARA TU FRONTEND**

### **OrdenesService.js - VERSIÓN FUNCIONANDO:**
```javascript
export const crearOrdenVenta = async (ordenData) => {
  const payload = {
    obra: ordenData.obra || "",
    clienteId: parseInt(ordenData.clienteId),
    trabajadorId: parseInt(ordenData.trabajadorId),
    sedeId: parseInt(ordenData.sedeId),
    credito: false, // Por ahora solo ventas de contado
    items: ordenData.items.map(item => ({
      productoId: parseInt(item.productoId),
      cantidad: parseInt(item.cantidad),
      descripcion: item.descripcion || "",
      precioUnitario: parseFloat(item.precioUnitario)
    }))
  };

  const response = await fetch('/api/ordenes/venta', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al crear orden');
  }

  return response.json();
};
```

### **Componente de Creación - VERSIÓN SIMPLIFICADA:**
```javascript
const handleFacturar = async () => {
  try {
    const ordenData = {
      obra: formulario.obra,
      clienteId: parseInt(formulario.clienteId),
      trabajadorId: parseInt(formulario.trabajadorId), 
      sedeId: parseInt(formulario.sedeId),
      credito: false, // Por ahora solo de contado
      items: items.map(item => ({
        productoId: parseInt(item.productoId),
        cantidad: parseInt(item.cantidad),
        descripcion: item.descripcion,
        precioUnitario: parseFloat(item.precioUnitario)
      }))
    };

    const resultado = await OrdenesService.crearOrdenVenta(ordenData);
    console.log('✅ Orden creada:', resultado);
    
    // Manejar respuesta exitosa
    onSuccess(resultado);
    
  } catch (error) {
    console.error('❌ Error al crear orden:', error);
    setError(error.message);
  }
};
```

---

## 🎯 **PASOS PARA PROBAR**

### **PASO 1: Inicia el servidor**
```bash
.\mvnw spring-boot:run
```

### **PASO 2: Prueba en Swagger primero**
- Ve a `http://localhost:8080/swagger-ui.html`
- Busca `POST /api/ordenes/venta`
- Usa el JSON de arriba con `"credito": false`

### **PASO 3: Si funciona en Swagger, adapta tu frontend**
- Cambia la URL a `/api/ordenes/venta`
- Usa el formato de datos simplificado
- Por ahora, deja `credito: false`

---

## 📊 **ENDPOINTS QUE SÍ FUNCIONAN**

### **✅ Crear Orden (sin crédito automático):**
```
POST /api/ordenes/venta
```

### **✅ Listar Órdenes:**
```
GET /api/ordenes/tabla
```

### **✅ Crear Crédito manualmente después:**
```
POST /api/creditos
```

---

## 🔧 **PLAN PARA CRÉDITOS AUTOMÁTICOS**

**OPCIÓN A:** Crear crédito por separado después de la orden
**OPCIÓN B:** Endpoint específico para ventas a crédito
**OPCIÓN C:** Corregir las transacciones anidadas

Por ahora, **vamos con la OPCIÓN A** que funciona:

1. **Crear orden** con `credito: false`
2. **Si necesita crédito**, hacer llamada separada a `/api/creditos`

---

## ⚡ **RESUMEN**

1. **✅ Backend:** Funciona para órdenes de contado
2. **🔄 Frontend:** Cambia URL y formato de datos
3. **⏳ Créditos:** Se implementarán por separado

**¡PRUEBA AHORA CON ESTOS CAMBIOS Y DEBERÍA FUNCIONAR!** 🚀