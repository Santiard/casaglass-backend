package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Credito;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 💰 DTO PARA LISTAR CRÉDITOS PENDIENTES EN LA PÁGINA DE ABONOS
 * 
 * Contiene toda la información necesaria para:
 * - Mostrar créditos con saldo pendiente
 * - Calcular retención de fuente y retención ICA
 * - Registrar abonos
 * 
 * Endpoint: GET /api/creditos/cliente/{clienteId}/pendientes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditoPendienteDTO {
    
    // ========== DATOS DEL CRÉDITO ==========
    
    /** ID del crédito */
    private Long creditoId;
    
    /** Monto total del crédito original */
    private Double totalCredito;
    
    /** Monto total abonado hasta el momento */
    private Double totalAbonado;
    
    /** Saldo pendiente (totalCredito - totalAbonado - retencionFuente - retencionIca si aplican) */
    private Double saldoPendiente;
    
    /** Estado del crédito: ABIERTO, CERRADO, VENCIDO, ANULADO */
    private String estado;
    
    // ========== DATOS DE LA ORDEN ==========
    
    /** ID de la orden que originó el crédito */
    private Long ordenId;
    
    /** Número de orden único */
    private Long ordenNumero;
    
    /** Fecha de la orden */
    private LocalDate ordenFecha;
    
    /** Obra/proyecto de la orden */
    private String ordenObra;
    
    /** Número de factura asociada a la orden (null o "-" si no tiene) */
    private String numeroFactura;
    
    // ========== MONTOS DE LA ORDEN ==========
    
    /** Total facturado (subtotal + IVA) */
    private Double total;
    
    /** Subtotal sin IVA - IMPORTANTE para calcular retención de fuente */
    private Double subtotal;
    
    /** IVA calculado */
    private Double iva;
    
    // ========== RETENCIÓN DE FUENTE ==========
    
    /** Indica si la orden tiene retención de fuente - CRÍTICO */
    private Boolean tieneRetencionFuente;
    
    /** Valor de la retención en la fuente */
    private Double retencionFuente;
    
    // ========== RETENCIÓN ICA ==========
    
    /** Indica si la orden tiene retención ICA */
    private Boolean tieneRetencionIca;
    
    /** Valor de la retención ICA */
    private Double retencionIca;
    
    /** Porcentaje de retención ICA aplicado (opcional) */
    private Double porcentajeIca;
    
    // ========== DATOS ADICIONALES ==========
    
    /** Fecha del último abono realizado (opcional) */
    private LocalDate fechaUltimoAbono;
    
    /** Cantidad total de abonos realizados */
    private Integer cantidadAbonos;
    
    // ========== RELACIONES ==========
    
    /** Información de la sede */
    private SedeSimpleDTO sede;
    
    /** Información del cliente (opcional, normalmente ya se tiene) */
    private ClienteSimpleDTO cliente;
    
    // ========== DTOs INTERNOS ==========
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SedeSimpleDTO {
        private Long id;
        private String nombre;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteSimpleDTO {
        private Long id;
        private String nombre;
        private String nit;
    }
    
    // ========== CONSTRUCTOR DESDE ENTIDAD ==========
    
    /**
     * Constructor que mapea desde la entidad Credito
     * Extrae todos los datos necesarios de la orden asociada
     */
    public CreditoPendienteDTO(Credito credito) {
        // Datos del crédito
        this.creditoId = credito.getId();
        this.totalCredito = credito.getTotalCredito();
        this.totalAbonado = credito.getTotalAbonado();
        this.saldoPendiente = credito.getSaldoPendiente();
        this.estado = credito.getEstado().name();
        
        // Datos de la orden
        if (credito.getOrden() != null) {
            this.ordenId = credito.getOrden().getId();
            this.ordenNumero = credito.getOrden().getNumero();
            this.ordenFecha = credito.getOrden().getFecha();
            this.ordenObra = credito.getOrden().getObra();
            
            // Montos de la orden
            this.total = credito.getOrden().getTotal();
            this.subtotal = credito.getOrden().getSubtotal();
            this.iva = credito.getOrden().getIva();
            
            // Retención de fuente
            this.tieneRetencionFuente = credito.getOrden().isTieneRetencionFuente();
            this.retencionFuente = credito.getOrden().getRetencionFuente();
            
            // Retención ICA
            this.tieneRetencionIca = credito.getOrden().isTieneRetencionIca();
            this.retencionIca = credito.getOrden().getRetencionIca();
            this.porcentajeIca = credito.getOrden().getPorcentajeIca();
            
            // Sede
            if (credito.getOrden().getSede() != null) {
                this.sede = new SedeSimpleDTO(
                    credito.getOrden().getSede().getId(),
                    credito.getOrden().getSede().getNombre()
                );
            }
        }
        
        // Cliente
        if (credito.getCliente() != null) {
            this.cliente = new ClienteSimpleDTO(
                credito.getCliente().getId(),
                credito.getCliente().getNombre(),
                credito.getCliente().getNit()
            );
        }
        
        // Datos adicionales de abonos
        if (credito.getAbonos() != null && !credito.getAbonos().isEmpty()) {
            this.cantidadAbonos = credito.getAbonos().size();
            
            // Encontrar fecha del último abono
            this.fechaUltimoAbono = credito.getAbonos().stream()
                .map(abono -> abono.getFecha())
                .max(LocalDate::compareTo)
                .orElse(null);
        } else {
            this.cantidadAbonos = 0;
            this.fechaUltimoAbono = null;
        }
    }
}
