package com.casaglass.casaglass_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GastoSedeCreateDTO {
    
    @NotNull(message = "La sede es obligatoria")
    private Long sedeId;
    
    @NotNull(message = "La fecha del gasto es obligatoria")
    private LocalDate fechaGasto;
    
    @NotNull(message = "El monto es obligatorio")
    @Min(value = 1, message = "El monto debe ser mayor a 0")
    private Double monto;
    
    @NotBlank(message = "El concepto es obligatorio")
    private String concepto;
    
    private String descripcion;
    private String comprobante;
    private String tipo = "OPERATIVO"; // OPERATIVO, COMBUSTIBLE, MANTENIMIENTO, SERVICIOS, EMERGENCIA, ALIMENTACION, OTRO
    private Long empleadoId; // Quien realizó el gasto (opcional)
    private Long proveedorId; // Opcional
    private String observaciones;
}