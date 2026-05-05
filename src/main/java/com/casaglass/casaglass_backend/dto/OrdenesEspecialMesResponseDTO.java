package com.casaglass.casaglass_backend.dto;

import java.util.List;

public class OrdenesEspecialMesResponseDTO {
    public Integer year;
    public Integer month;
    public Double totalVenta = 0.0;
    public Double totalPagos = 0.0;
    public Double totalSaldoPendiente = 0.0;
    public List<OrdenEspecialMesItemDTO> ordenes;

    public OrdenesEspecialMesResponseDTO() {}
}
