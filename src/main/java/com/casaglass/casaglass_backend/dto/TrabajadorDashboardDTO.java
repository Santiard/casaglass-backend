package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrabajadorDashboardDTO {

    private Resumen resumen;
    private List<VentaPorDia> ventasPorDia;
    private List<TopProducto> topProductos;
    private List<TopCliente> topClientes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resumen {
        private Long trabajadorId;
        private String trabajadorNombre;
        private Long totalOrdenes;
        private Long contadoCantidad;
        private Double contadoMonto;
        private Long creditoCantidad;
        private Double creditoMonto;
        private Double montoTotal;
        private Double ticketPromedio;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VentaPorDia {
        private LocalDate fecha;
        private Long cantidad;
        private Double monto;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProducto {
        private Long productoId;
        private String nombre;
        private String codigo;
        private Long cantidad;
        private Double monto;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCliente {
        private Long clienteId;
        private String nombre;
        private Long cantidadOrdenes;
        private Double monto;
    }
}


