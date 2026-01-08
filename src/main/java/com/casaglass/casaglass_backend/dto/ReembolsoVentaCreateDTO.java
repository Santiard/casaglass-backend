package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.ReembolsoVenta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO para crear un reembolso de venta (devolución del cliente)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReembolsoVentaCreateDTO {
    
    /**
     * ID de la orden original que se está reembolsando
     */
    private Long ordenId;
    
    /**
     * Fecha del retorno (cuándo el cliente devuelve)
     * Si no se envía, se usa la fecha actual
     */
    private LocalDate fecha;
    
    /**
     * Motivo o razón del reembolso
     */
    private String motivo;
    
    /**
     * Forma en que se devuelve el dinero al cliente
     */
    private ReembolsoVenta.FormaReembolso formaReembolso;
    
    /**
     * Descuentos aplicados al reembolso (opcional, por defecto 0.0)
     */
    private Double descuentos = 0.0;
    
    /**
     * Lista de detalles (productos a devolver)
     */
    private List<ReembolsoVentaDetalleDTO> detalles;
    
    /**
     * DTO para el detalle de reembolso de venta
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReembolsoVentaDetalleDTO {
        
        /**
         * ID del item de la orden original que se está reembolsando
         * Este es el OrdenItem.id de la orden original
         */
        private Long ordenItemId;
        
        /**
         * Cantidad de productos a devolver
         * Debe ser menor o igual a la cantidad vendida en la orden original
         */
        private Double cantidad;
        
        /**
         * Precio unitario al momento del reembolso
         * Si no se envía, se usa el precio del OrdenItem original
         */
        private Double precioUnitario;
    }
}

