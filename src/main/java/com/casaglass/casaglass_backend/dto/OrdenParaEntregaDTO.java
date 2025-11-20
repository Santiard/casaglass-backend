package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para mostrar órdenes disponibles para incluir en una entrega
 * Solo muestra órdenes A CONTADO que aún no han sido entregadas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenParaEntregaDTO {
    
    private Long id;
    private Long numero;
    private LocalDate fecha;
    private String clienteNombre;
    private String clienteNit;
    private Double total;
    private String obra;
    private String sedeNombre;
    private String trabajadorNombre;
    private boolean yaEntregada; // Si ya está en otra entrega
    private Long entregaId; // ID de la entrega actual (si aplica)
    
    // Información adicional para validación
    private boolean esContado; // true si NO es crédito
    private String estado; // ACTIVA, ANULADA
    private boolean venta; // true si es una venta (no compra)
}