package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Factura;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para devolver los detalles completos de una factura incluyendo la orden relacionada
 * Usado en GET /api/facturas/{id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacturaDetalleDTO {
    
    // Información de la factura
    private Long id;
    private String numeroFactura;
    private LocalDate fecha;
    private Double subtotal;
    private Double iva;
    private Double retencionFuente;
    private Double retencionIca;
    private Double total;
    private String formaPago;
    private String observaciones;
    private String estado;
    private LocalDate fechaPago;
    
    // Cliente al que se factura (puede ser diferente al cliente de la orden)
    private ClienteSimpleDTO cliente;
    
    // Detalles completos de la orden relacionada
    private OrdenDetalleDTO orden;
    
    /**
     * Constructor desde entidad Factura
     * Incluye los detalles completos de la orden relacionada
     */
    public FacturaDetalleDTO(Factura factura) {
        // ✅ Envolver en try-catch para manejar excepciones durante la inicialización
        try {
            // Información básica de la factura
            this.id = factura.getId();
            this.numeroFactura = factura.getNumeroFactura();
            this.fecha = factura.getFecha();
            this.subtotal = factura.getSubtotal();
            this.iva = factura.getIva();
            this.retencionFuente = factura.getRetencionFuente();
            this.retencionIca = factura.getRetencionIca();
            this.total = factura.getTotal();
            this.formaPago = factura.getFormaPago();
            this.observaciones = factura.getObservaciones();
            this.estado = factura.getEstado() != null ? factura.getEstado().name() : "PENDIENTE";
            this.fechaPago = factura.getFechaPago();
            
            // ✅ Cliente de la factura (puede ser diferente al cliente de la orden)
            if (factura.getCliente() != null) {
                try {
                    this.cliente = new ClienteSimpleDTO(factura.getCliente());
                } catch (Exception e) {
                    // Si hay error al obtener el cliente, dejarlo como null
                    this.cliente = null;
                }
            } else {
                this.cliente = null;
            }
            
            // ✅ Obtener orden de forma segura (maneja referencias huérfanas)
            if (factura.getOrden() != null) {
                try {
                    // Usar OrdenDetalleDTO que ya tiene todas las protecciones implementadas
                    this.orden = new OrdenDetalleDTO(factura.getOrden());
                    // ✅ Eliminar el cliente de la orden - solo se muestra el cliente de la factura
                    if (this.orden != null) {
                        this.orden.setCliente(null);
                    }
                } catch (Exception e) {
                    // Si hay error al construir OrdenDetalleDTO, dejar orden como null
                    this.orden = null;
                }
            } else {
                this.orden = null;
            }
            
        } catch (jakarta.persistence.EntityNotFoundException e) {
            // Si ocurre una excepción durante la construcción, inicializar con valores por defecto
            inicializarConValoresPorDefecto(factura);
        } catch (Exception e) {
            // Cualquier otra excepción: inicializar con valores por defecto
            inicializarConValoresPorDefecto(factura);
        }
    }
    
    /**
     * Método helper para inicializar el DTO con valores por defecto en caso de error
     */
    private void inicializarConValoresPorDefecto(Factura factura) {
        this.id = factura != null ? factura.getId() : null;
        this.numeroFactura = factura != null ? factura.getNumeroFactura() : null;
        this.fecha = factura != null ? factura.getFecha() : null;
        this.subtotal = factura != null ? factura.getSubtotal() : null;
        this.iva = factura != null ? factura.getIva() : null;
        this.retencionFuente = factura != null ? factura.getRetencionFuente() : null;
        this.retencionIca = factura != null ? factura.getRetencionIca() : null;
        this.total = factura != null ? factura.getTotal() : null;
        this.formaPago = factura != null ? factura.getFormaPago() : null;
        this.observaciones = factura != null ? factura.getObservaciones() : null;
        this.estado = "PENDIENTE";
        this.fechaPago = factura != null ? factura.getFechaPago() : null;
        this.cliente = null;
        this.orden = null;
    }
}

