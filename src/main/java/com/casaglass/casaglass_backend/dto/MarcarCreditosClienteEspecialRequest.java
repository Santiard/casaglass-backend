package com.casaglass.casaglass_backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MarcarCreditosClienteEspecialRequest {

    @NotEmpty(message = "Debe proporcionar al menos un ID de crédito")
    private List<Long> creditoIds;

    /** Nombre o identificador del usuario que ejecuta el cierre masivo (opcional). */
    private String ejecutadoPor;

    /** Campo libre para observaciones del lote. */
    private String observaciones;
}
