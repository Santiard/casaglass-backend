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
    private Double total;
    private ClienteDetalleDTO cliente;
    private List<ItemDetalleDTO> items;
    
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
    public static class ProductoItemDTO {
        private Long id;
        private String nombre;
        
        public ProductoItemDTO(Producto producto) {
            this.id = producto.getId();
            this.nombre = producto.getNombre();
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDetalleDTO {
        private Long id;
        private ProductoItemDTO producto;
        private String descripcion;
        private Integer cantidad;
        private Double precioUnitario;
        private Double totalLinea;
        
        public ItemDetalleDTO(OrdenItem item) {
            this.id = item.getId();
            this.producto = item.getProducto() != null ? new ProductoItemDTO(item.getProducto()) : null;
            this.descripcion = item.getDescripcion();
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
        this.total = orden.getTotal();
        this.cliente = new ClienteDetalleDTO(orden.getCliente());
        
        // Convertir items
        this.items = new ArrayList<>();
        if (orden.getItems() != null) {
            for (OrdenItem item : orden.getItems()) {
                this.items.add(new ItemDetalleDTO(item));
            }
        }
    }
}

