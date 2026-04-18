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
    private VentasMesInfo ventasMes;
    private FaltanteEntregaInfo faltanteEntrega;
    private CreditosPendientesInfo creditosPendientes;
    private DeudasMesInfo deudasMes;
    private DeudasActivasInfo deudasActivas;
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
        private Double montoFaltante;          // Total pendiente de entregar (contado + abonos)
        private Double montoOrdenesContado;    // Parte de ventas a contado sin entrega
        private Double montoAbonos;            // Parte de abonos de crédito sin entrega
        private Integer cantidadOrdenes;       // N.º de órdenes contado pendientes
        private Integer cantidadAbonos;        // N.º de abonos pendientes
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
        private String color; // Color del producto (MATE, BRILLANTE, etc.)
        private String categoria;
        private Double stockActual;
        private Integer nivelReorden; // Calculado como cantidad mínima recomendada
        private String estado; // "AGOTADO", "CRÍTICO", "BAJO"
    }

    /** Ventas realizadas durante el mes en curso para la sede */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VentasMesInfo {
        private Integer cantidad;
        private Double total;
        private Integer ventasContado;
        private Integer ventasCredito;
        private Double totalContado;
        private Double totalCredito;
    }

    /** Créditos (deudas) creados durante el mes en curso para la sede */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeudasMesInfo {
        private Integer totalDeudas;
        private Double montoTotalDeudas;
        private Double montoPendiente;
        private Integer deudasAbiertas;
        private Integer deudasCerradas;
    }

    /** Histórico completo de deudas de la sede (todos los estados, desde siempre) */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeudasActivasInfo {
        private Integer totalDeudas;
        private Double montoTotalHistorico;
        private Double montoPendienteActivo;
        private Integer deudasAbiertas;
        private Integer deudasCerradas;
        private Integer deudasAnuladas;
    }
}