package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.ReembolsoVenta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO para respuesta de reembolso de venta
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReembolsoVentaResponseDTO {
    
    private Long id;
    private LocalDate fecha;
    private OrdenSimpleDTO ordenOriginal;
    private ClienteSimpleDTO cliente;
    private SedeSimpleDTO sede;
    private String motivo;
    private List<ReembolsoVentaDetalleResponseDTO> detalles;
    private Double subtotal;
    private Double descuentos;
    private Double totalReembolso;
    private String formaReembolso;
    private Boolean procesado;
    private String estado;
    
    // Constructor desde entidad
    public ReembolsoVentaResponseDTO(ReembolsoVenta reembolso) {
        this.id = reembolso.getId();
        this.fecha = reembolso.getFecha();
        this.ordenOriginal = reembolso.getOrdenOriginal() != null ?
            new OrdenSimpleDTO(reembolso.getOrdenOriginal()) : null;
        this.cliente = reembolso.getCliente() != null ?
            new ClienteSimpleDTO(reembolso.getCliente()) : null;
        this.sede = reembolso.getSede() != null ?
            new SedeSimpleDTO(reembolso.getSede()) : null;
        this.motivo = reembolso.getMotivo();
        this.detalles = reembolso.getDetalles() != null ?
            reembolso.getDetalles().stream()
                .map(ReembolsoVentaDetalleResponseDTO::new)
                .collect(Collectors.toList()) : List.of();
        this.subtotal = reembolso.getSubtotal();
        this.descuentos = reembolso.getDescuentos();
        this.totalReembolso = reembolso.getTotalReembolso();
        this.formaReembolso = reembolso.getFormaReembolso() != null ? 
            reembolso.getFormaReembolso().name() : null;
        this.procesado = reembolso.getProcesado();
        this.estado = reembolso.getEstado() != null ? reembolso.getEstado().name() : null;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReembolsoVentaDetalleResponseDTO {
        private Long id;
        private OrdenItemSimpleDTO ordenItemOriginal;
        private ProductoSimpleDTO producto;
        private Double cantidad;
        private Double precioUnitario;
        private Double totalLinea;
        
        public ReembolsoVentaDetalleResponseDTO(com.casaglass.casaglass_backend.model.ReembolsoVentaDetalle detalle) {
            this.id = detalle.getId();
            this.ordenItemOriginal = detalle.getOrdenItemOriginal() != null ?
                new OrdenItemSimpleDTO(detalle.getOrdenItemOriginal()) : null;
            this.producto = detalle.getProducto() != null ?
                new ProductoSimpleDTO(detalle.getProducto()) : null;
            this.cantidad = detalle.getCantidad();
            this.precioUnitario = detalle.getPrecioUnitario();
            this.totalLinea = detalle.getTotalLinea();
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrdenItemSimpleDTO {
        private Long id;
        private Integer cantidad;
        private Double precioUnitario;
        
        public OrdenItemSimpleDTO(com.casaglass.casaglass_backend.model.OrdenItem item) {
            this.id = item.getId();
            this.cantidad = item.getCantidad();
            this.precioUnitario = item.getPrecioUnitario();
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

