package com.casaglass.casaglass_backend.dto;

import java.time.LocalDate;

public class OrdenEspecialMesItemDTO {
    public Long ordenId;
    public Long numeroOrden;
    public LocalDate fechaOrden;
    public String obra;
    public Double totalOrden;

    // Crédito asociado
    public Long creditoId;
    public Double totalCredito;
    public Double totalAbonado;
    public Double saldoPendiente;

    public OrdenEspecialMesItemDTO() {}
}
