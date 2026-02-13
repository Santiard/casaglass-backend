package com.casaglass.casaglass_backend.exception;

/**
 * Excepción personalizada para casos donde se intenta trasladar o usar
 * una cantidad mayor de la disponible en inventario
 */
public class InventarioInsuficienteException extends RuntimeException {
    
    private final Double cantidadDisponible;
    private final Double cantidadRequerida;
    private final Long productoId;
    private final Long sedeId;
    
    public InventarioInsuficienteException(String message) {
        super(message);
        this.cantidadDisponible = null;
        this.cantidadRequerida = null;
        this.productoId = null;
        this.sedeId = null;
    }
    
    public InventarioInsuficienteException(String message, Double cantidadDisponible, Double cantidadRequerida, 
                                          Long productoId, Long sedeId) {
        super(message);
        this.cantidadDisponible = cantidadDisponible;
        this.cantidadRequerida = cantidadRequerida;
        this.productoId = productoId;
        this.sedeId = sedeId;
    }
    
    // Getters
    public Double getCantidadDisponible() { return cantidadDisponible; }
    public Double getCantidadRequerida() { return cantidadRequerida; }
    public Long getProductoId() { return productoId; }
    public Long getSedeId() { return sedeId; }
}