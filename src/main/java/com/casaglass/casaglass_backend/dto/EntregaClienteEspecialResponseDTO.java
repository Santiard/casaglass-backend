package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.EntregaClienteEspecial;
import com.casaglass.casaglass_backend.model.EntregaClienteEspecialDetalle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntregaClienteEspecialResponseDTO {

    private Long id;
    private LocalDateTime fechaRegistro;
    private String ejecutadoPor;
    private Integer totalCreditos;
    private Double totalMontoCredito;
    private String observaciones;
    private List<Detalle> detalles;

    public EntregaClienteEspecialResponseDTO(EntregaClienteEspecial entrega) {
        this.id = entrega.getId();
        this.fechaRegistro = entrega.getFechaRegistro();
        this.ejecutadoPor = entrega.getEjecutadoPor();
        this.totalCreditos = entrega.getTotalCreditos();
        this.totalMontoCredito = entrega.getTotalMontoCredito();
        this.observaciones = entrega.getObservaciones();
        if (entrega.getDetalles() != null) {
            this.detalles = entrega.getDetalles().stream()
                    .map(Detalle::new)
                    .collect(Collectors.toList());
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detalle {
        private Long creditoId;
        private Long ordenId;
        private Long numeroOrden;
        private String obra;
        private LocalDate fechaCredito;
        private Double totalCredito;
        private Double saldoAnterior;

        public Detalle(EntregaClienteEspecialDetalle entity) {
            this.creditoId = entity.getCredito() != null ? entity.getCredito().getId() : null;
            this.ordenId = entity.getOrden() != null ? entity.getOrden().getId() : null;
            this.numeroOrden = entity.getNumeroOrden();
            this.obra = entity.getOrden() != null ? entity.getOrden().getObra() : null;
            this.fechaCredito = entity.getFechaCredito();
            this.totalCredito = entity.getTotalCredito();
            this.saldoAnterior = entity.getSaldoAnterior();
        }
    }
}
