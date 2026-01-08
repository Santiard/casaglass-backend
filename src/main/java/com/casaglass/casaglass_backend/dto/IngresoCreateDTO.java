package com.casaglass.casaglass_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngresoCreateDTO {
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;
    
    private ProveedorIdDTO proveedor;
    private String numeroFactura;
    private String observaciones;
    private List<IngresoDetalleCreateDTO> detalles;
    private Double totalCosto;
    private Boolean procesado;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProveedorIdDTO {
        private Long id;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngresoDetalleCreateDTO {
        private ProductoIdDTO producto;
        private Double cantidad;
        private Double costoUnitario; // Costo original del ingreso (para calcular totalCosto y trazabilidad)
        private Double costoUnitarioPonderado; // Costo calculado con promedio ponderado (viene del frontend, se usa para actualizar producto.costo)
        private Double totalLinea; // Se calcula con costoUnitario (costo original)
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ProductoIdDTO {
            private Long id;
        }
    }
}
