package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO optimizado para crear órdenes de venta desde el frontend
 * Contiene toda la información necesaria para realizar una venta real
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenVentaDTO {
    
    // INFORMACIÓN BÁSICA DE LA ORDEN
    private LocalDate fecha; // opcional - si no se envía, se usa la fecha actual
    private String obra; // opcional - descripción del proyecto/obra
    private String descripcion; // opcional - descripción/observaciones adicionales de la orden
    private boolean venta = true; // por defecto true para ventas
    private boolean credito = false; // si es venta a crédito
    private boolean incluidaEntrega = false; // si incluye entrega
    private boolean tieneRetencionFuente = false; // si la orden tiene retención de fuente
    private Double descuentos = 0.0; // Descuentos aplicados a la orden
    
    // IDs DE ENTIDADES RELACIONADAS (requeridos)
    private Long clienteId; // OBLIGATORIO
    private Long sedeId; // OBLIGATORIO - sede donde se realiza la venta
    private Long trabajadorId; // OPCIONAL - vendedor encargado
    
    // ITEMS DE LA VENTA (mínimo 1 item requerido)
    private List<OrdenItemVentaDTO> items;
    
    // 🆕 CORTES DE PRODUCTOS PERFIL (opcional)
    private List<CorteSolicitadoDTO> cortes;
    
    /**
     * DTO para items de venta
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrdenItemVentaDTO {
        private Long productoId; // OBLIGATORIO - producto a vender
        private String descripcion; // OPCIONAL - descripción personalizada
        private Integer cantidad; // OBLIGATORIO - cantidad a vender (min: 1)
        private Double precioUnitario; // OBLIGATORIO - precio unitario
        // Opcional: si se reutiliza un corte solicitado existente para vender
        private Long reutilizarCorteSolicitadoId;
        
        // totalLinea se calcula automáticamente en el backend
    }
    
    /**
     * 🆕 DTO para cortes de productos PERFIL
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CorteSolicitadoDTO {
        private Long productoId;              // Producto PERFIL original
        private Integer medidaSolicitada;     // Medida en cm del corte a vender
        private Integer cantidad;             // Cantidad de cortes
        
        // Datos ya calculados por el frontend:
        private Double precioUnitarioSolicitado;  // Precio del corte a vender
        private Double precioUnitarioSobrante;     // Precio del corte sobrante

        // Opcional: reutilizar un corte sobrante existente (evita crear uno nuevo)
        private Long reutilizarCorteId;
        
        // 🔧 Cantidades por sede usando IDs (más robusto que nombres)
        // Formato: [{sedeId: 1, cantidad: 1}, {sedeId: 2, cantidad: 0}, {sedeId: 3, cantidad: 0}]
        // IMPORTANTE: Solo se aplican al sobrante si esSobrante === true
        private List<CantidadPorSedeDTO> cantidadesPorSede;
        
        // 🎯 Indica si este corte es el sobrante (true) o el solicitado (false)
        // Si esSobrante === true: se aplica cantidadesPorSede para incrementar stock
        // Si esSobrante === false: NO se incrementa stock (se vende, stock queda en 0)
        private Boolean esSobrante = false;
        
        // Medida del corte sobrante (calculada por el frontend)
        private Integer medidaSobrante;       // Medida en cm del corte sobrante
        
        /**
         * DTO interno para cantidad por sede
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CantidadPorSedeDTO {
            private Long sedeId;           // ID de la sede
            private Integer cantidad;      // Cantidad a agregar en esa sede
        }
    }
}