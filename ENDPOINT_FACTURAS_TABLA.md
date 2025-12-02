# 📋 Endpoint: GET /api/facturas/tabla

## 🔗 Información del Endpoint

**URL:** `/api/facturas/tabla`  
**Método:** `GET`  
**Controlador:** `FacturaController.java` (línea 76-79)  
**Servicio:** `FacturaService.listarParaTabla()` (línea 181-185)

---

## 📤 Estructura de la Respuesta

El endpoint retorna un **array de objetos `FacturaTablaDTO`**.

### Tipo de Respuesta
```java
ResponseEntity<List<FacturaTablaDTO>>
```

### Código de Estado
- **200 OK**: Lista de facturas obtenida exitosamente
- **500 Internal Server Error**: Error interno del servidor

---

## 📊 Estructura del DTO: `FacturaTablaDTO`

```json
{
  "id": 50,
  "numeroFactura": "FAC-001",
  "fecha": "2025-01-15",
  "obra": "Casa nueva",
  "subtotal": 135000.0,
  "descuentos": 50000.0,
  "iva": 0.0,
  "retencionFuente": 0.0,
  "total": 85000.0,
  "formaPago": "EFECTIVO",
  "estado": "PENDIENTE",
  "fechaPago": null,
  "observaciones": "Factura con descuento",
  "cliente": {
    "nombre": "Juan Pérez",
    "nit": "1234567-8"
  },
  "orden": {
    "numero": 1001
  }
}
```

---

## 📝 Campos del DTO

### Campos Principales

| Campo | Tipo | Descripción | Ejemplo |
|-------|------|-------------|---------|
| `id` | `Long` | ID único de la factura | `50` |
| `numeroFactura` | `String` | Número de factura | `"FAC-001"` |
| `fecha` | `LocalDate` | Fecha de la factura | `"2025-01-15"` |
| `obra` | `String` | Nombre de la obra (de la orden) | `"Casa nueva"` |
| `subtotal` | `Double` | Subtotal de la factura | `135000.0` |
| `descuentos` | `Double` | Descuentos aplicados | `50000.0` |
| `iva` | `Double` | IVA aplicado | `0.0` |
| `retencionFuente` | `Double` | Retención en la fuente | `0.0` |
| `total` | `Double` | Total de la factura | `85000.0` |
| `formaPago` | `String` | Forma de pago | `"EFECTIVO"` |
| `estado` | `EstadoFactura` | Estado de la factura (enum) | `"PENDIENTE"` |
| `fechaPago` | `LocalDate` | Fecha de pago (null si no está pagada) | `null` o `"2025-01-20"` |
| `observaciones` | `String` | Observaciones de la factura | `"Factura con descuento"` |

### Campos Anidados

#### `cliente` (ClienteTabla)
| Campo | Tipo | Descripción | Ejemplo |
|-------|------|-------------|---------|
| `nombre` | `String` | Nombre del cliente | `"Juan Pérez"` |
| `nit` | `String` | NIT del cliente | `"1234567-8"` |

**Lógica de asignación:**
- ✅ Si la factura tiene cliente (`factura.cliente`), usa ese cliente
- ✅ Si no, usa el cliente de la orden (`factura.orden.cliente`)
- ⚠️ Si no hay cliente en ninguno, `cliente` será `null`

#### `orden` (OrdenTabla)
| Campo | Tipo | Descripción | Ejemplo |
|-------|------|-------------|---------|
| `numero` | `Long` | Número de la orden | `1001` |

**Nota:** Solo se incluye si la factura tiene una orden asociada.

---

## 🔢 Enum: EstadoFactura

Los valores posibles del campo `estado` son:

- `PENDIENTE`: Factura pendiente de pago
- `PAGADA`: Factura pagada
- `ANULADA`: Factura anulada
- `EN_PROCESO`: Factura en proceso

---

## 📋 Ejemplo de Respuesta Completa

```json
[
  {
    "id": 50,
    "numeroFactura": "FAC-001",
    "fecha": "2025-01-15",
    "obra": "Casa nueva",
    "subtotal": 135000.0,
    "descuentos": 50000.0,
    "iva": 0.0,
    "retencionFuente": 0.0,
    "total": 85000.0,
    "formaPago": "EFECTIVO",
    "estado": "PENDIENTE",
    "fechaPago": null,
    "observaciones": "Factura con descuento",
    "cliente": {
      "nombre": "Juan Pérez",
      "nit": "1234567-8"
    },
    "orden": {
      "numero": 1001
    }
  },
  {
    "id": 51,
    "numeroFactura": "FAC-002",
    "fecha": "2025-01-16",
    "obra": "Edificio comercial",
    "subtotal": 200000.0,
    "descuentos": 0.0,
    "iva": 38000.0,
    "retencionFuente": 0.0,
    "total": 238000.0,
    "formaPago": "TRANSFERENCIA",
    "estado": "PAGADA",
    "fechaPago": "2025-01-20",
    "observaciones": null,
    "cliente": {
      "nombre": "María González",
      "nit": "9876543-2"
    },
    "orden": {
      "numero": 1002
    }
  }
]
```

---

## 🔍 Lógica de Conversión

El método `convertirAFacturaTablaDTO()` en `FacturaService.java` (línea 311-343) realiza la siguiente conversión:

1. **Campos básicos:** Se copian directamente de la entidad `Factura`
2. **Obra:** Se obtiene de `factura.getOrden().getObra()`
3. **Cliente:** 
   - Prioridad 1: `factura.getCliente()` (si existe)
   - Prioridad 2: `factura.getOrden().getCliente()` (si la factura no tiene cliente)
4. **Orden:** Se obtiene el número de `factura.getOrden().getNumero()`
5. **Estado:** Se convierte de `Factura.EstadoFactura` a `FacturaTablaDTO.EstadoFactura`

---

## ⚠️ Notas Importantes

1. **Cliente de la Factura vs Cliente de la Orden:**
   - La factura puede tener un cliente diferente al de la orden
   - Si la factura tiene `cliente_id`, se usa ese cliente
   - Si no, se usa el cliente de la orden

2. **Campos Opcionales:**
   - `obra`: Puede ser `null` si la orden no tiene obra
   - `cliente`: Puede ser `null` si no hay cliente en factura ni orden
   - `orden`: Puede ser `null` si la factura no tiene orden asociada
   - `fechaPago`: Es `null` si la factura no está pagada
   - `observaciones`: Puede ser `null`

3. **Optimización:**
   - Este endpoint está optimizado para mostrar facturas en tablas
   - Solo incluye los campos necesarios para la visualización
   - No incluye relaciones completas (solo datos básicos)

---

## 🔗 Archivos Relacionados

- **Controlador:** `src/main/java/com/casaglass/casaglass_backend/controller/FacturaController.java`
- **Servicio:** `src/main/java/com/casaglass/casaglass_backend/service/FacturaService.java`
- **DTO:** `src/main/java/com/casaglass/casaglass_backend/dto/FacturaTablaDTO.java`
- **Modelo:** `src/main/java/com/casaglass/casaglass_backend/model/Factura.java`

---

**Última actualización:** 2025-01-XX

