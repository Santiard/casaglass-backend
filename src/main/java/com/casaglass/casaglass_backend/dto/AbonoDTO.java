package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Abono;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

/**
 * 💰 DTO PARA CREAR ABONOS DESDE EL FRONTEND
 * Incluye TODOS los campos que el frontend debe enviar (ninguno opcional)
 */
@Data
public class AbonoDTO {
    
    /** Monto del abono (OBLIGATORIO) */
    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    private Double total;
    
    /** Fecha del abono (OBLIGATORIO) */
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;
    
    /** Método de pago (OBLIGATORIO) */
    @NotNull(message = "El método de pago es obligatorio")
    private Abono.MetodoPago metodoPago;
    
    /** Número de factura/recibo (OBLIGATORIO - puede ser string vacío) */
    @NotNull(message = "El número de factura es obligatorio")
    private String factura;
}