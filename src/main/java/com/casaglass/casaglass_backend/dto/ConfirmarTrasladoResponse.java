package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta de confirmación de traslado
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarTrasladoResponse {
    private String message;
    private TrasladoMovimientoDTO traslado;
}