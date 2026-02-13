package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Abono;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbonoSimpleDTO {
    
    private Long id;
    private LocalDate fecha;
    private LocalDate fechaAbono;  // ← Alias para fecha (compatibilidad frontend)
    private Double total;
    private Double montoAbono;  // ← Alias para total (compatibilidad frontend)
    private String metodoPago;
    private String factura;
    private String numeroFactura;  // ← Alias para factura (compatibilidad frontend)
    private Double saldo;
    private Long numeroOrden;
    private String observaciones;  // ← Campo adicional para observaciones
    
    // Constructor desde entidad (SIN referencia al crédito completo para evitar ciclos)
    public AbonoSimpleDTO(Abono abono) {
        this.id = abono.getId();
        this.fecha = abono.getFecha();
        this.fechaAbono = abono.getFecha();  // ← Mismo valor
        this.total = abono.getTotal();
        this.montoAbono = abono.getTotal();  // ← Mismo valor
        this.metodoPago = abono.getMetodoPago();
        this.factura = abono.getFactura();
        this.numeroFactura = abono.getFactura();  // ← Mismo valor
        this.saldo = abono.getSaldo();
        this.numeroOrden = abono.getNumeroOrden();
        this.observaciones = null;  // ← Campo opcional, puede extraerse del metodoPago si es necesario
    }
}