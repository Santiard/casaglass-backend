package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SedeDashboardDTO {
    
    private SedeInfo sede;
    private VentasHoyInfo ventasHoy;
    private FaltanteEntregaInfo faltanteEntrega;
    private CreditosPendientesInfo creditosPendientes;
    private TrasladosPendientesInfo trasladosPendientes;
    private AlertasStockInfo alertasStock;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SedeInfo {
        private Long id;
        private String nombre;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VentasHoyInfo {
        private Integer cantidad;
        private Double total;
        private Integer ventasContado;
        private Integer ventasCredito;
        private Double totalContado;
        private Double totalCredito;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaltanteEntregaInfo {
        private Double montoFaltante;
        private LocalDateTime ultimaEntrega;
        private Double montoUltimaEntrega;
        private String estadoUltimaEntrega;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditosPendientesInfo {
        private Integer totalCreditos;
        private Double montoTotal;
        private Double montoPendiente;
        private List<CreditoResumenDTO> creditosVencidos;
        private List<CreditoResumenDTO> creditosProximoVencimiento;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditoResumenDTO {
        private Long creditoId;
        private String clienteNombre;
        private Long ordenNumero;
        private Double saldoPendiente;
        private LocalDate fechaInicio;
        private LocalDate fechaVencimiento;
        private String estado;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrasladosPendientesInfo {
        private Integer totalPendientes;
        private List<TrasladoPendienteDTO> trasladosRecibir; // Traslados que llegan a esta sede
        private List<TrasladoPendienteDTO> trasladosEnviar;  // Traslados que salen de esta sede
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrasladoPendienteDTO {
        private Long trasladoId;
        private String sedeOrigen;
        private String sedeDestino;
        private LocalDate fecha;
        private Integer totalProductos;
        private String estado; // "PENDIENTE_CONFIRMACION", "EN_TRANSITO"
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertasStockInfo {
        private Integer total;
        private List<ProductoBajoStockDTO> productosBajos;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoBajoStockDTO {
        private String codigo;
        private String nombre;
        private String categoria;
        private Double stockActual;
        private Integer nivelReorden; // Calculado como cantidad mínima recomendada
        private String estado; // "AGOTADO", "CRÍTICO", "BAJO"
    }
}