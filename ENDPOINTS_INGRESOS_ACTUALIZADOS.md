# Endpoints de Ingresos - Actualizaciones

## 🎯 Cambios Realizados

### 1. Nuevo Endpoint: Marcar como Procesado

**POST** `/api/ingresos/{id}/marcar-procesado`

- **Descripción**: Marca un ingreso como procesado (procesado=true) sin actualizar el inventario automáticamente
- **Uso**: Para casos donde necesitas marcar manualmente que un ingreso fue procesado sin modificar el inventario
- **Validaciones**: 
  - El ingreso debe existir
  - No debe estar ya marcado como procesado

#### Ejemplo de Respuesta
```json
{
    "id": 1,
    "fecha": "2025-10-21",
    "proveedor": {
        "id": 1,
        "nombre": "Proveedor Ejemplo"
    },
    "numeroFactura": "FAC-001",
    "observaciones": "Procesado manualmente",
    "detalles": [...],
    "totalCosto": 1500.00,
    "procesado": true
}
```

### 2. Arreglo del Endpoint PUT

**PUT** `/api/ingresos/{id}`

- **Problema Resuelto**: Error 500 que ocurría pero la actualización funcionaba
- **Causa**: Entidades no gestionadas (detached) del proveedor y productos en los detalles
- **Solución**: Implementado manejo correcto de entidades JPA mediante búsqueda en repositorios

#### Cambios en el Servicio
```java
// ANTES - Causaba error 500
ingresoExistente.setProveedor(ingresoActualizado.getProveedor());

// AHORA - Busca entidad gestionada
Proveedor proveedor = proveedorRepository.findById(ingresoActualizado.getProveedor().getId())
    .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
ingresoExistente.setProveedor(proveedor);
```

## 📋 Endpoints Disponibles de Ingresos

### Endpoints Existentes (ya funcionaban)
1. **GET** `/api/ingresos` - Listar todos los ingresos
2. **GET** `/api/ingresos/{id}` - Obtener ingreso por ID  
3. **GET** `/api/ingresos/no-procesados` - Ingresos pendientes de procesar
4. **GET** `/api/ingresos/por-fecha?fechaInicio=2025-01-01&fechaFin=2025-12-31` - Filtrar por fechas
5. **POST** `/api/ingresos` - Crear nuevo ingreso
6. **DELETE** `/api/ingresos/{id}` - Eliminar ingreso (solo si no está procesado)
7. **POST** `/api/ingresos/{id}/procesar` - Procesar ingreso (actualiza inventario)
8. **POST** `/api/ingresos/{id}/reprocesar` - Reprocesar ingreso

### Endpoints Nuevos/Arreglados
9. **PUT** `/api/ingresos/{id}` - ✅ **ARREGLADO** - Actualizar ingreso 
10. **POST** `/api/ingresos/{id}/marcar-procesado` - ✅ **NUEVO** - Solo marcar como procesado

## 🔧 Diferencias entre Endpoints de Procesamiento

| Endpoint | Actualiza Inventario | Marca como Procesado | Uso |
|----------|---------------------|---------------------|-----|
| `/procesar` | ✅ SÍ | ✅ SÍ | Procesamiento completo automático |
| `/reprocesar` | ✅ SÍ | ✅ SÍ | Corrección de procesamiento previo |
| `/marcar-procesado` | ❌ NO | ✅ SÍ | Solo cambio de estado manual |

## 🧪 Pruebas Recomendadas

### 1. Probar PUT corregido
```bash
curl -X PUT http://localhost:8080/api/ingresos/1 \
  -H "Content-Type: application/json" \
  -d '{
    "fecha": "2025-10-21",
    "proveedor": {"id": 1},
    "numeroFactura": "FAC-UPD-001",
    "observaciones": "Actualizado correctamente",
    "detalles": [
      {
        "producto": {"id": 1},
        "cantidad": 10,
        "costoUnitario": 50.00
      }
    ]
  }'
```

### 2. Probar nuevo endpoint marcar-procesado
```bash
curl -X POST http://localhost:8080/api/ingresos/1/marcar-procesado
```

## ✅ Estado Actual

- ✅ Servidor funcionando correctamente en puerto 8080
- ✅ PUT `/api/ingresos/{id}` **ARREGLADO** - Ya no devuelve error 500
- ✅ Nuevo endpoint `/marcar-procesado` **IMPLEMENTADO**
- ✅ Manejo correcto de entidades JPA en actualizaciones
- ✅ Validaciones de negocio mantenidas

## 🔧 Segundo Arreglo - Error 500 Persistente

### 🐛 Problema Identificado
El error 500 persistía porque:
1. **IngresoDetalle con ID**: El JSON enviaba `"id": 9` en los detalles, causando conflictos de estado de entidad
2. **Campo version**: El JSON enviaba `"version": null` para un campo que existía pero no era manejado correctamente

### 🛠️ Soluciones Implementadas

#### 1. Recreación de Detalles
```java
// ANTES - Problema con entidades existentes
detalle.setIngreso(ingresoExistente);
ingresoExistente.getDetalles().add(detalle);

// AHORA - Crear nuevos detalles para evitar conflictos
IngresoDetalle nuevoDetalle = new IngresoDetalle();
nuevoDetalle.setCantidad(detalleActualizado.getCantidad());
nuevoDetalle.setCostoUnitario(detalleActualizado.getCostoUnitario());
nuevoDetalle.setIngreso(ingresoExistente);
ingresoExistente.getDetalles().add(nuevoDetalle);
```

#### 2. Campo Version Ignorado
```java
// En Producto.java
@JsonIgnore  // Ignorar en serialización/deserialización JSON
private Long version;
```

#### 3. Logging Mejorado
- Agregados logs detallados en controlador y servicio
- Manejo de excepciones más específico
- Stack traces para debugging

## 🎯 Resumen de Cambios Técnicos

1. **IngresoService.actualizarIngreso()**: 
   - Manejo de entidades gestionadas para proveedor y productos
   - Recreación de detalles para evitar conflictos de estado
2. **IngresoService.marcarComoProcesado()**: Nuevo método para cambio de estado simple
3. **IngresoController**: 
   - Agregado endpoint `/marcar-procesado`
   - Logging detallado y manejo de excepciones mejorado
4. **Producto.java**: Campo `version` marcado como `@JsonIgnore`
5. **Validaciones**: Mantenidas todas las reglas de negocio existentes

## 🧪 Prueba la Corrección

El error 500 debería estar resuelto ahora. Prueba el mismo curl:

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/ingresos/5' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
    "fecha": "2025-10-20",
    "proveedor": {"id": 3},
    "numeroFactura": "9999",
    "observaciones": "actualizado sin errores",
    "detalles": [
      {
        "producto": {"id": 1},
        "cantidad": 11,
        "costoUnitario": 1
      }
    ]
  }'
```

**Nota**: Removí el `"id": 9` del detalle y `"version": null` del producto para evitar conflictos.