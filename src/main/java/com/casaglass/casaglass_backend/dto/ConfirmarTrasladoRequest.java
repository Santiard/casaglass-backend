package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para recibir datos de confirmación de traslado
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarTrasladoRequest {
    private Long trabajadorId;
}