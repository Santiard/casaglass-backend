# 🔧 Solución: Endpoint POST /api/ingresos con IDs

## 🎯 Problema Resuelto

El frontend enviaba solo los IDs de proveedor y productos en el payload, pero el backend necesitaba las entidades completas para evitar problemas de serialización/deserialización.

### Payload del Frontend:
```json
{
  "fecha": "2025-10-20",
  "proveedor": { "id": 2 },
  "numeroFactura": "444444",
  "observaciones": "DFDFDF",
  "detalles": [
    {
      "producto": { "id": 123 },
      "cantidad": 1,
      "costoUnitario": 1000,
      "totalLinea": 1000
    }
  ],
  "totalCosto": 1000,
  "procesado": true
}
```

## 🛠️ Solución Implementada

### 1. **Nuevo DTO: IngresoCreateDTO**

**Archivo:** `src/main/java/com/casaglass/casaglass_backend/dto/IngresoCreateDTO.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngresoCreateDTO {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;
    
    private ProveedorIdDTO proveedor;
    private String numeroFactura;
    private String observaciones;
    private List<IngresoDetalleCreateDTO> detalles;
    private Double totalCosto;
    private Boolean procesado;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProveedorIdDTO {
        private Long id;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngresoDetalleCreateDTO {
        private ProductoIdDTO producto;
        private Integer cantidad;
        private Double costoUnitario;
        private Double totalLinea;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ProductoIdDTO {
            private Long id;
        }
    }
}
```

### 2. **Nuevo Método en IngresoService**

**Método:** `crearIngresoDesdeDTO(IngresoCreateDTO ingresoDTO)`

```java
public Ingreso crearIngresoDesdeDTO(IngresoCreateDTO ingresoDTO) {
    // Crear la entidad Ingreso
    Ingreso ingreso = new Ingreso();
    ingreso.setFecha(ingresoDTO.getFecha() != null ? ingresoDTO.getFecha() : LocalDate.now());
    ingreso.setNumeroFactura(ingresoDTO.getNumeroFactura());
    ingreso.setObservaciones(ingresoDTO.getObservaciones());
    ingreso.setTotalCosto(ingresoDTO.getTotalCosto() != null ? ingresoDTO.getTotalCosto() : 0.0);
    ingreso.setProcesado(ingresoDTO.getProcesado() != null ? ingresoDTO.getProcesado() : false);

    // Buscar el proveedor completo por ID
    Proveedor proveedorCompleto = proveedorRepository.findById(ingresoDTO.getProveedor().getId())
        .orElseThrow(() -> new RuntimeException("Proveedor con ID " + ingresoDTO.getProveedor().getId() + " no encontrado"));
    ingreso.setProveedor(proveedorCompleto);

    // Procesar los detalles de productos
    for (IngresoCreateDTO.IngresoDetalleCreateDTO detalleDTO : ingresoDTO.getDetalles()) {
        // Buscar el producto completo por ID
        Producto productoCompleto = productoRepository.findById(detalleDTO.getProducto().getId())
            .orElseThrow(() -> new RuntimeException("Producto con ID " + detalleDTO.getProducto().getId() + " no encontrado"));
        
        // Crear el detalle del ingreso
        IngresoDetalle detalle = new IngresoDetalle();
        detalle.setProducto(productoCompleto);
        detalle.setCantidad(detalleDTO.getCantidad());
        detalle.setCostoUnitario(detalleDTO.getCostoUnitario());
        detalle.setTotalLinea(detalleDTO.getTotalLinea());
        detalle.setIngreso(ingreso);
        
        ingreso.getDetalles().add(detalle);
    }

    // Calcular totales y guardar
    ingreso.calcularTotal();
    Ingreso ingresoGuardado = ingresoRepository.save(ingreso);
    procesarInventario(ingresoGuardado);
    
    return ingresoGuardado;
}
```

### 3. **Controlador Actualizado**

**Endpoint:** `POST /api/ingresos`

```java
@PostMapping
public ResponseEntity<?> crearIngreso(@RequestBody IngresoCreateDTO ingresoDTO) {
    try {
        System.out.println("🔄 POST /api/ingresos - Creando ingreso desde DTO");
        System.out.println("📥 Datos recibidos - Proveedor ID: " + 
            (ingresoDTO.getProveedor() != null ? ingresoDTO.getProveedor().getId() : "null"));
        System.out.println("📥 Detalles: " + 
            (ingresoDTO.getDetalles() != null ? ingresoDTO.getDetalles().size() : 0) + " items");
        
        Ingreso resultado = ingresoService.crearIngresoDesdeDTO(ingresoDTO);
        
        System.out.println("✅ Ingreso creado exitosamente - ID: " + resultado.getId());
        
        return ResponseEntity.ok(resultado);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    } catch (RuntimeException e) {
        return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Error inesperado: " + e.getMessage());
    }
}
```

## ✅ Ventajas de la Solución

1. **Deserialización Correcta**: El DTO maneja específicamente la estructura de IDs del frontend
2. **Entidades Completas**: Busca las entidades completas en la base de datos
3. **Validaciones Robustas**: Verifica que existan proveedor y productos
4. **Logging Detallado**: Facilita el debugging
5. **Manejo de Errores**: Mensajes específicos para cada tipo de error
6. **Compatibilidad**: Mantiene la funcionalidad existente

## 🧪 Prueba la Solución

### Request desde Frontend:
```javascript
const payload = {
  fecha: "2025-10-20",
  proveedor: { id: 2 },
  numeroFactura: "444444",
  observaciones: "DFDFDF",
  detalles: [
    {
      producto: { id: 123 },
      cantidad: 1,
      costoUnitario: 1000,
      totalLinea: 1000
    }
  ],
  totalCosto: 1000,
  procesado: true
};

fetch('/api/ingresos', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify(payload)
})
.then(response => response.json())
.then(data => console.log('Ingreso creado:', data))
.catch(error => console.error('Error:', error));
```

### cURL para Testing:
```bash
curl -X POST http://localhost:8080/api/ingresos \
  -H "Content-Type: application/json" \
  -d '{
    "fecha": "2025-10-20",
    "proveedor": {"id": 2},
    "numeroFactura": "444444",
    "observaciones": "DFDFDF",
    "detalles": [
      {
        "producto": {"id": 123},
        "cantidad": 1,
        "costoUnitario": 1000,
        "totalLinea": 1000
      }
    ],
    "totalCosto": 1000,
    "procesado": true
  }'
```

## 🎉 Resultado

- ✅ **Frontend**: Puede enviar solo IDs sin problemas
- ✅ **Backend**: Busca entidades completas automáticamente
- ✅ **Validaciones**: Verifica existencia de proveedor y productos
- ✅ **Inventario**: Se actualiza automáticamente al procesar
- ✅ **Logging**: Facilita debugging y monitoreo

¡La solución está lista para usar! 🚀
