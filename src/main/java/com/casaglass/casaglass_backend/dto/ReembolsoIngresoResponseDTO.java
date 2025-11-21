package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.ReembolsoIngreso;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO para respuesta de reembolso de ingreso
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReembolsoIngresoResponseDTO {
    
    private Long id;
    private LocalDate fecha;
    private IngresoSimpleDTO ingresoOriginal;
    private ProveedorSimpleDTO proveedor;
    private String numeroFacturaDevolucion;
    private String motivo;
    private List<ReembolsoIngresoDetalleResponseDTO> detalles;
    private Double totalReembolso;
    private Boolean procesado;
    private String estado;
    
    // Constructor desde entidad
    public ReembolsoIngresoResponseDTO(ReembolsoIngreso reembolso) {
        this.id = reembolso.getId();
        this.fecha = reembolso.getFecha();
        this.ingresoOriginal = reembolso.getIngresoOriginal() != null ? 
            new IngresoSimpleDTO(reembolso.getIngresoOriginal()) : null;
        this.proveedor = reembolso.getProveedor() != null ? 
            new ProveedorSimpleDTO(reembolso.getProveedor()) : null;
        this.numeroFacturaDevolucion = reembolso.getNumeroFacturaDevolucion();
        this.motivo = reembolso.getMotivo();
        this.detalles = reembolso.getDetalles() != null ? 
            reembolso.getDetalles().stream()
                .map(ReembolsoIngresoDetalleResponseDTO::new)
                .collect(Collectors.toList()) : List.of();
        this.totalReembolso = reembolso.getTotalReembolso();
        this.procesado = reembolso.getProcesado();
        this.estado = reembolso.getEstado() != null ? reembolso.getEstado().name() : null;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReembolsoIngresoDetalleResponseDTO {
        private Long id;
        private IngresoDetalleSimpleDTO ingresoDetalleOriginal;
        private ProductoSimpleDTO producto;
        private Integer cantidad;
        private Double costoUnitario;
        private Double totalLinea;
        
        public ReembolsoIngresoDetalleResponseDTO(com.casaglass.casaglass_backend.model.ReembolsoIngresoDetalle detalle) {
            this.id = detalle.getId();
            this.ingresoDetalleOriginal = detalle.getIngresoDetalleOriginal() != null ?
                new IngresoDetalleSimpleDTO(detalle.getIngresoDetalleOriginal()) : null;
            this.producto = detalle.getProducto() != null ?
                new ProductoSimpleDTO(detalle.getProducto()) : null;
            this.cantidad = detalle.getCantidad();
            this.costoUnitario = detalle.getCostoUnitario();
            this.totalLinea = detalle.getTotalLinea();
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngresoSimpleDTO {
        private Long id;
        private LocalDate fecha;
        private String numeroFactura;
        
        public IngresoSimpleDTO(com.casaglass.casaglass_backend.model.Ingreso ingreso) {
            this.id = ingreso.getId();
            this.fecha = ingreso.getFecha();
            this.numeroFactura = ingreso.getNumeroFactura();
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProveedorSimpleDTO {
        private Long id;
        private String nombre;
        
        public ProveedorSimpleDTO(com.casaglass.casaglass_backend.model.Proveedor proveedor) {
            this.id = proveedor.getId();
            this.nombre = proveedor.getNombre();
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngresoDetalleSimpleDTO {
        private Long id;
        private Integer cantidad;
        private Double costoUnitario;
        
        public IngresoDetalleSimpleDTO(com.casaglass.casaglass_backend.model.IngresoDetalle detalle) {
            this.id = detalle.getId();
            this.cantidad = detalle.getCantidad();
            this.costoUnitario = detalle.getCostoUnitario();
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoSimpleDTO {
        private Long id;
        private String codigo;
        private String nombre;
        
        public ProductoSimpleDTO(com.casaglass.casaglass_backend.model.Producto producto) {
            this.id = producto.getId();
            this.codigo = producto.getCodigo();
            this.nombre = producto.getNombre();
        }
    }
}

