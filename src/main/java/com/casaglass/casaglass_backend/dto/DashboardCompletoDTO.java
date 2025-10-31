package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO consolidado con todos los datos del dashboard
 * Una sola llamada retorna todo para que el frontend lo organice
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCompletoDTO {
    
    // 📊 RESUMEN GENERAL
    private ResumenGeneral resumenGeneral;
    
    // 📈 VENTAS POR DÍA (últimos 7 días o rango personalizado)
    private List<VentaPorDia> ventasPorDia;
    
    // 🏢 VENTAS POR SEDE
    private List<VentaPorSede> ventasPorSede;
    
    // 🏆 TOP PRODUCTOS MÁS VENDIDOS
    private List<TopProducto> topProductos;
    
    // 👥 TOP CLIENTES POR MONTO
    private List<TopCliente> topClientes;
    
    // 💳 RESUMEN DE CRÉDITOS
    private ResumenCreditos resumenCreditos;
    
    // 🧾 FACTURACIÓN POR ESTADO
    private FacturacionPorEstado facturacionPorEstado;
    
    // 💰 TICKET PROMEDIO POR SEDE
    private List<TicketPromedioSede> ticketPromedioPorSede;
    
    // ===== INNER CLASSES =====
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenGeneral {
        private Long totalOrdenes;
        private Long totalVentas;
        private Long totalCotizaciones;
        private Double montoTotalVentas;
        private Double montoTotalCotizaciones;
        private Long totalFacturas;
        private Double montoTotalFacturado;
        private Long totalCreditosAbiertos;
        private Double montoCreditosPendiente;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VentaPorDia {
        private LocalDate fecha;
        private Long cantidadOrdenes;
        private Double montoTotal;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VentaPorSede {
        private Long sedeId;
        private String nombreSede;
        private Long cantidadOrdenes;
        private Double montoTotal;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProducto {
        private Long productoId;
        private String nombreProducto;
        private String codigo;
        private Long cantidadVendida;
        private Double montoTotal;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCliente {
        private Long clienteId;
        private String nombreCliente;
        private String nit;
        private Long cantidadOrdenes;
        private Double montoTotal;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenCreditos {
        private Long totalAbiertos;
        private Long totalCerrados;
        private Long totalVencidos;
        private Long totalAnulados;
        private Double montoTotalPendiente;
        private Double montoTotalAbonado;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacturacionPorEstado {
        private Long pendientes;
        private Long pagadas;
        private Long anuladas;
        private Double montoPendiente;
        private Double montoPagado;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketPromedioSede {
        private Long sedeId;
        private String nombreSede;
        private Double ticketPromedio;
        private Long cantidadVentas;
    }
}

