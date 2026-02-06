package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.*;
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
    private boolean tieneRetencionIca; // Indica si la orden tiene retención ICA
    private Double porcentajeIca; // Porcentaje de retención ICA (configurable desde frontend)
    private Double retencionIca; // Valor monetario de la retención ICA
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
            if (cliente != null) {
            this.id = cliente.getId();
            this.nombre = cliente.getNombre();
            this.nit = cliente.getNit();
            this.direccion = cliente.getDireccion();
            this.telefono = cliente.getTelefono();
            }
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
            if (producto != null) {
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
    
    /**
     * Método helper para obtener la Factura de forma segura
     * Maneja el caso cuando la Factura fue eliminada (referencia huérfana)
     */
    private Factura obtenerFacturaSegura(Orden orden) {
        if (orden == null || orden.getFactura() == null) {
            return null;
        }
        try {
            Factura factura = orden.getFactura();
            factura.getId(); // Forzar carga del proxy
            return factura;
        } catch (Exception e) {
            // Captura cualquier excepción (EntityNotFoundException, LazyInitializationException, etc.)
            return null;
        }
    }
    
    /**
     * Método helper para obtener el Producto de forma segura
     * Maneja el caso cuando el Producto fue eliminado (referencia huérfana)
     */
    private Producto obtenerProductoSeguro(OrdenItem item) {
        if (item == null || item.getProducto() == null) {
            return null;
        }
        try {
            Producto producto = item.getProducto();
            producto.getId(); // Forzar carga del proxy
            return producto;
        } catch (Exception e) {
            // Captura cualquier excepción (EntityNotFoundException, LazyInitializationException, etc.)
            return null;
        }
    }
    
    /**
     * Método helper para obtener el Cliente de forma segura
     */
    private Cliente obtenerClienteSeguro(Orden orden) {
        if (orden == null || orden.getCliente() == null) {
            return null;
        }
        try {
            Cliente cliente = orden.getCliente();
            cliente.getId(); // Forzar carga del proxy
            return cliente;
        } catch (Exception e) {
            // Captura cualquier excepción (EntityNotFoundException, LazyInitializationException, etc.)
            return null;
        }
    }
    
    /**
     * Método helper para obtener la Sede de forma segura
     */
    private Sede obtenerSedeSegura(Orden orden) {
        if (orden == null || orden.getSede() == null) {
            return null;
        }
        try {
            Sede sede = orden.getSede();
            sede.getId(); // Forzar carga del proxy
            return sede;
        } catch (Exception e) {
            // Captura cualquier excepción (EntityNotFoundException, LazyInitializationException, etc.)
            return null;
        }
    }
    
    /**
     * Método helper para obtener el Trabajador de forma segura
     */
    private Trabajador obtenerTrabajadorSeguro(Orden orden) {
        if (orden == null || orden.getTrabajador() == null) {
            return null;
        }
        try {
            Trabajador trabajador = orden.getTrabajador();
            trabajador.getId(); // Forzar carga del proxy
            return trabajador;
        } catch (Exception e) {
            // Captura cualquier excepción (EntityNotFoundException, LazyInitializationException, etc.)
            return null;
        }
    }
    
    // Constructor desde entidad
    public OrdenDetalleDTO(Orden orden) {
        // ✅ Envolver todo el constructor en try-catch para manejar cualquier excepción de Hibernate
        try {
        this.id = orden.getId();
        this.numero = orden.getNumero();
        this.fecha = orden.getFecha();
        this.obra = orden.getObra();
        this.descripcion = orden.getDescripcion();
            
            // ✅ Obtener Factura de forma segura
            Factura factura = obtenerFacturaSegura(orden);
            this.numeroFactura = (factura != null && factura.getNumeroFactura() != null) 
                ? factura.getNumeroFactura() 
                : "-";
            
        this.venta = orden.isVenta();
        this.credito = orden.isCredito();
        this.tieneRetencionFuente = orden.isTieneRetencionFuente();
        this.subtotal = orden.getSubtotal();
        this.iva = orden.getIva();
        this.retencionFuente = orden.getRetencionFuente();
            this.tieneRetencionIca = orden.isTieneRetencionIca();
            this.porcentajeIca = orden.getPorcentajeIca();
            this.retencionIca = orden.getRetencionIca();
        this.total = orden.getTotal();
        this.estado = orden.getEstado() != null ? orden.getEstado().name() : "ACTIVA";
            
            // ✅ Obtener Sede de forma segura
            Sede sede = obtenerSedeSegura(orden);
            this.sede = sede != null ? new SedeSimpleDTO(sede) : null;
            
            // ✅ Obtener Cliente de forma segura
            Cliente cliente = obtenerClienteSeguro(orden);
            this.cliente = cliente != null ? new ClienteDetalleDTO(cliente) : null;
            
            // ✅ Obtener Trabajador de forma segura
            Trabajador trabajador = obtenerTrabajadorSeguro(orden);
            this.trabajador = trabajador != null ? new TrabajadorSimpleDTO(trabajador) : null;
        
            // ✅ Convertir items con manejo seguro de productos
        this.items = new ArrayList<>();
        if (orden.getItems() != null) {
            for (OrdenItem item : orden.getItems()) {
                    try {
                        // Verificar que el producto existe antes de crear el DTO
                        Producto producto = obtenerProductoSeguro(item);
                        if (producto != null) {
                this.items.add(new ItemDetalleDTO(item));
            }
                        // Si el producto no existe, simplemente no agregamos el item
                    } catch (Exception e) {
                        // Si hay algún error al procesar un item, continuar con el siguiente
                        continue;
                    }
                }
            }
            
        } catch (jakarta.persistence.EntityNotFoundException e) {
            // Si ocurre una excepción durante la construcción, inicializar con valores por defecto
            inicializarConValoresPorDefecto(orden);
        } catch (Exception e) {
            // Cualquier otra excepción: inicializar con valores por defecto
            inicializarConValoresPorDefecto(orden);
        }
    }
    
    /**
     * Método helper para inicializar el DTO con valores por defecto en caso de error
     */
    private void inicializarConValoresPorDefecto(Orden orden) {
        this.id = orden != null ? orden.getId() : null;
        this.numero = orden != null ? orden.getNumero() : null;
        this.fecha = orden != null ? orden.getFecha() : null;
        this.obra = orden != null ? orden.getObra() : null;
        this.descripcion = orden != null ? orden.getDescripcion() : null;
        this.numeroFactura = "-";
        this.venta = orden != null && orden.isVenta();
        this.credito = orden != null && orden.isCredito();
        this.tieneRetencionFuente = orden != null && orden.isTieneRetencionFuente();
        this.subtotal = orden != null ? orden.getSubtotal() : null;
        this.iva = orden != null ? orden.getIva() : null;
        this.retencionFuente = orden != null ? orden.getRetencionFuente() : null;
        this.tieneRetencionIca = orden != null && orden.isTieneRetencionIca();
        this.porcentajeIca = orden != null ? orden.getPorcentajeIca() : null;
        this.retencionIca = orden != null ? orden.getRetencionIca() : null;
        this.total = orden != null ? orden.getTotal() : null;
        this.estado = "ACTIVA";
        this.sede = null;
        this.cliente = null;
        this.trabajador = null;
        this.items = new ArrayList<>();
    }
}

