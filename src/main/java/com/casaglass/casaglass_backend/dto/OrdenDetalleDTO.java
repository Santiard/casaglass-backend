package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.model.OrdenItem;
import com.casaglass.casaglass_backend.model.Producto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenDetalleDTO {
    
    private Long id;
    private Long numero;
    private LocalDate fecha;
    private String obra;
    private String descripcion; // Descripción/observaciones adicionales
    private String numeroFactura; // Número de la factura asociada (null o "-" si no tiene)
    private boolean venta; // Indica si es una venta (true) o solo una orden (false)
    private boolean credito; // Indica si la venta es a crédito (true) o contado (false)
    private boolean tieneRetencionFuente; // Indica si la orden tiene retención de fuente
    private Double subtotal; // Subtotal de la orden (base imponible SIN IVA)
    private Double iva; // Valor del IVA calculado
    private Double retencionFuente; // Valor monetario de la retención en la fuente
    private Double total; // Total facturado (subtotal facturado, sin restar retención)
    private String estado; // Estado de la orden: ACTIVA, ENTREGADA, ANULADA
    private SedeSimpleDTO sede; // Sede donde se realizó la orden
    private ClienteDetalleDTO cliente;
    private TrabajadorSimpleDTO trabajador; // Trabajador que realizó la venta
    private List<ItemDetalleDTO> items;
    
    // Clase anidada para Sede
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SedeSimpleDTO {
        private Long id;
        private String nombre;
        private String direccion;
        private String ciudad;
        
        public SedeSimpleDTO(com.casaglass.casaglass_backend.model.Sede sede) {
            if (sede != null) {
                this.id = sede.getId();
                this.nombre = sede.getNombre();
                this.direccion = sede.getDireccion();
                this.ciudad = sede.getCiudad();
            }
        }
    }
    
    // Clases anidadas para la estructura
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteDetalleDTO {
        private Long id;
        private String nombre;
        private String nit;
        private String direccion;
        private String telefono;
        
        public ClienteDetalleDTO(com.casaglass.casaglass_backend.model.Cliente cliente) {
            this.id = cliente.getId();
            this.nombre = cliente.getNombre();
            this.nit = cliente.getNit();
            this.direccion = cliente.getDireccion();
            this.telefono = cliente.getTelefono();
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrabajadorSimpleDTO {
        private Long id;
        private String nombre;
        
        public TrabajadorSimpleDTO(com.casaglass.casaglass_backend.model.Trabajador trabajador) {
            if (trabajador != null) {
                this.id = trabajador.getId();
                this.nombre = trabajador.getNombre();
            }
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoItemDTO {
        private Long id;
        private String codigo;
        private String nombre;
        private String color;  // Color del producto (enum serializado como String)
        private String tipo;   // Tipo del producto (enum serializado como String)
        
        public ProductoItemDTO(Producto producto) {
            this.id = producto.getId();
            this.codigo = producto.getCodigo();
            // ✅ Mostrar el nombre completo del producto (incluyendo "Corte de X CMS" si es un corte)
            // Tanto cliente como trabajador necesitan ver el nombre completo
            this.nombre = producto.getNombre();
            // Convertir enums a String (pueden ser null)
            this.color = producto.getColor() != null ? producto.getColor().name() : null;
            this.tipo = producto.getTipo() != null ? producto.getTipo().name() : null;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDetalleDTO {
        private Long id;
        private ProductoItemDTO producto;
        private Double cantidad;
        private Double precioUnitario;
        private Double totalLinea;
        
        public ItemDetalleDTO(OrdenItem item) {
            this.id = item.getId();
            this.producto = item.getProducto() != null ? new ProductoItemDTO(item.getProducto()) : null;
            // ✅ Campo descripcion eliminado - los datos del producto se obtienen mediante la relación
            this.cantidad = item.getCantidad();
            this.precioUnitario = item.getPrecioUnitario();
            this.totalLinea = item.getTotalLinea();
        }
    }
    
    // Constructor desde entidad
    public OrdenDetalleDTO(Orden orden) {
        this.id = orden.getId();
        this.numero = orden.getNumero();
        this.fecha = orden.getFecha();
        this.obra = orden.getObra();
        this.descripcion = orden.getDescripcion();
        this.numeroFactura = (orden.getFactura() != null) ? orden.getFactura().getNumeroFactura() : "-";
        this.venta = orden.isVenta();
        this.credito = orden.isCredito();
        this.tieneRetencionFuente = orden.isTieneRetencionFuente();
        this.subtotal = orden.getSubtotal();
        this.iva = orden.getIva();
        this.retencionFuente = orden.getRetencionFuente();
        this.total = orden.getTotal();
        this.estado = orden.getEstado() != null ? orden.getEstado().name() : "ACTIVA";
        this.sede = orden.getSede() != null ? new SedeSimpleDTO(orden.getSede()) : null;
        this.cliente = new ClienteDetalleDTO(orden.getCliente());
        this.trabajador = orden.getTrabajador() != null ? new TrabajadorSimpleDTO(orden.getTrabajador()) : null;
        
        // Convertir items
        this.items = new ArrayList<>();
        if (orden.getItems() != null) {
            for (OrdenItem item : orden.getItems()) {
                this.items.add(new ItemDetalleDTO(item));
            }
        }
    }
}

