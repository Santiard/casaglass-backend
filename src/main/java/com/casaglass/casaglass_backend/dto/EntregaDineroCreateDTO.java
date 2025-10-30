package com.casaglass.casaglass_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntregaDineroCreateDTO {
    
    @NotNull(message = "La sede es obligatoria")
    private Long sedeId;
    
    @NotNull(message = "El empleado es obligatorio")
    private Long empleadoId;
    
    @NotNull(message = "La fecha de entrega es obligatoria")
    private LocalDate fechaEntrega;
    
    @NotNull(message = "La fecha desde es obligatoria")
    private LocalDate fechaDesde;
    
    @NotNull(message = "La fecha hasta es obligatoria")
    private LocalDate fechaHasta;
    
    private String modalidadEntrega = "EFECTIVO"; // EFECTIVO, TRANSFERENCIA, etc.
    private String observaciones;
    private String numeroComprobante;
    
    // Lista de IDs de órdenes a incluir (opcional - se puede calcular automáticamente)
    private List<Long> ordenesIds;
    
    // Lista de gastos a incluir
    private List<GastoSedeCreateDTO> gastos;
    
    // Datos calculados (opcionales - el backend puede calcularlos)
    private Double montoEsperado;
    private Double montoGastos;
    private Double montoEntregado;

    // Desglose por método
    private Double montoEfectivo;
    private Double montoTransferencia;
    private Double montoCheque;
    private Double montoDeposito;
}