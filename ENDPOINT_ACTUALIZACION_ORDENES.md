# 🔄 ENDPOINT DE ACTUALIZACIÓN DE ÓRDENES

## 📋 **URL y Método**
```
PUT /api/ordenes/tabla/{id}
Content-Type: application/json
```

## 🎯 **Funcionalidades**
- ✅ Actualizar datos principales de la orden (fecha, obra, venta, credito)
- ✅ Cambiar cliente, trabajador, sede
- ✅ Agregar nuevos items a la orden
- ✅ Actualizar items existentes
- ✅ Eliminar items específicos
- ✅ Retorna estructura optimizada para tabla

## 📝 **Estructura del Body (JSON)**

```json
{
  "id": 1,
  "fecha": "2025-10-16",
  "obra": "Construcción Lote B - Actualizada",
  "venta": true,
  "credito": false,
  "clienteId": 2,      // Cambiar cliente
  "trabajadorId": 3,   // Cambiar trabajador  
  "sedeId": 1,         // Mantener sede
  "items": [
    {
      "id": 1,           // Item existente - ACTUALIZAR
      "productoId": 1,
      "descripcion": "Descripción actualizada",
      "cantidad": 15,
      "precioUnitario": 2.5,
      "totalLinea": 37.5,
      "eliminar": false
    },
    {
      "id": null,        // Item nuevo - CREAR
      "productoId": 5,
      "descripcion": "Producto completamente nuevo",
      "cantidad": 8,
      "precioUnitario": 12.0,
      "totalLinea": 96.0,
      "eliminar": false
    },
    {
      "id": 2,           // Item existente - ELIMINAR
      "eliminar": true   // Solo necesita id y eliminar=true
    }
  ]
}
```

## 🔄 **Lógica de Items**

| **Campo id** | **Campo eliminar** | **Acción** |
|-------------|-------------------|------------|
| `null` | `false` | **CREAR** nuevo item |
| `número` | `false` | **ACTUALIZAR** item existente |
| `número` | `true` | **ELIMINAR** item existente |

## 📊 **Respuesta (Success 200)**

```json
{
  "id": 1,
  "numero": 1001,
  "fecha": "2025-10-16",
  "obra": "Construcción Lote B - Actualizada",
  "venta": true,
  "credito": false,
  "cliente": {
    "nombre": "Cliente Actualizado"
  },
  "trabajador": {
    "nombre": "Nuevo Trabajador"
  },
  "sede": {
    "nombre": "La insula"
  },
  "items": [
    {
      "id": 1,
      "producto": {
        "codigo": "P001",
        "nombre": "Vidrio templado 6mm"
      },
      "descripcion": "Descripción actualizada",
      "cantidad": 15,
      "precioUnitario": 2.5,
      "totalLinea": 37.5
    },
    {
      "id": 3,
      "producto": {
        "codigo": "P005", 
        "nombre": "Producto Nuevo"
      },
      "descripcion": "Producto completamente nuevo",
      "cantidad": 8,
      "precioUnitario": 12.0,
      "totalLinea": 96.0
    }
  ]
}
```

## ⚠️ **Códigos de Error**

| **Código** | **Descripción** |
|------------|-----------------|
| `404` | Orden no encontrada |
| `400` | Datos inválidos |
| `500` | Error interno del servidor |

## 💡 **Ejemplos de Uso**

### 1. Solo actualizar datos básicos
```json
{
  "id": 1,
  "fecha": "2025-10-17",
  "obra": "Obra modificada",
  "venta": false,
  "credito": true,
  "clienteId": 1,
  "trabajadorId": 1,
  "sedeId": 1
}
```

### 2. Solo agregar un item nuevo
```json
{
  "id": 1,
  "items": [
    {
      "id": null,
      "productoId": 10,
      "descripcion": "Item adicional",
      "cantidad": 5,
      "precioUnitario": 20.0,
      "totalLinea": 100.0,
      "eliminar": false
    }
  ]
}
```

### 3. Solo eliminar items específicos
```json
{
  "id": 1,
  "items": [
    {
      "id": 2,
      "eliminar": true
    },
    {
      "id": 3,
      "eliminar": true
    }
  ]
}
```

## 🚀 **Curl de Ejemplo**

```bash
curl -X PUT \
  'http://localhost:8080/api/ordenes/tabla/1' \
  -H 'Content-Type: application/json' \
  -d '{
    "id": 1,
    "fecha": "2025-10-16",
    "obra": "Obra actualizada",
    "venta": true,
    "credito": false,
    "clienteId": 1,
    "trabajadorId": 2,
    "sedeId": 1,
    "items": [
      {
        "id": 1,
        "productoId": 1,
        "descripcion": "Descripción nueva",
        "cantidad": 20,
        "precioUnitario": 3.0,
        "totalLinea": 60.0,
        "eliminar": false
      }
    ]
  }'
```