package com.casaglass.casaglass_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "creditos", indexes = {
    @Index(name = "idx_credito_cliente", columnList = "cliente_id"),
    @Index(name = "idx_credito_orden", columnList = "orden_id"),
    @Index(name = "idx_credito_estado", columnList = "estado"),
    @Index(name = "idx_credito_fecha", columnList = "fecha_inicio")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Credito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Cliente cliente;

    /** 
     * Orden que originó el crédito (obligatoria para trazabilidad)
     * En tu sistema cada venta a crédito genera un crédito específico
     */
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "orden_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Orden orden;

    @NotNull
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;

    /** Monto total inicial del crédito (igual al total de la orden) */
    @NotNull
    @Column(name = "total_credito", nullable = false)
    private Double totalCredito;

    /** Monto total abonado hasta el momento */
    @NotNull
    @Column(name = "total_abonado", nullable = false)
    private Double totalAbonado = 0.0;

    /** Saldo pendiente = totalCredito - totalAbonado (calculado automáticamente) */
    @NotNull
    @Column(name = "saldo_pendiente", nullable = false)
    private Double saldoPendiente;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EstadoCredito estado = EstadoCredito.ABIERTO;

    /** Comentarios o notas adicionales sobre el crédito */
    @Column(length = 500)
    private String observaciones;

    /** 
     * Lista de abonos realizados a este crédito
     * EAGER para evitar problemas de lazy loading como en el resto del proyecto
     */
    @OneToMany(mappedBy = "credito", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"credito", "hibernateLazyInitializer", "handler"})
    private List<Abono> abonos = new ArrayList<>();

    public enum EstadoCredito {
        ABIERTO,    // Crédito activo con saldo pendiente
        CERRADO,    // Crédito completamente pagado
        VENCIDO,    // Crédito con pagos atrasados (si implementas fechas límite)
        ANULADO     // Crédito cancelado (por anulación de orden)
    }

    /**
     * Método helper para calcular y actualizar el saldo pendiente
     * Se debe llamar después de agregar/modificar abonos
     */
    public void actualizarSaldo() {
        this.saldoPendiente = this.totalCredito - this.totalAbonado;
        
        // Actualizar estado automáticamente
        if (this.saldoPendiente <= 0.0) {
            this.estado = EstadoCredito.CERRADO;
            if (this.fechaCierre == null) {
                this.fechaCierre = LocalDate.now();
            }
        } else if (this.estado == EstadoCredito.CERRADO) {
            // Si había estado cerrado pero ahora tiene saldo, reabrir
            this.estado = EstadoCredito.ABIERTO;
            this.fechaCierre = null;
        }
    }

    /**
     * Método helper para agregar un abono y actualizar totales
     */
    public void agregarAbono(Abono abono) {
        if (abono != null) {
            this.abonos.add(abono);
            abono.setCredito(this);
            this.totalAbonado += abono.getTotal();
            actualizarSaldo();
        }
    }

    /**
     * Método helper para verificar si el crédito está completamente pagado
     */
    public boolean estaPagado() {
        return this.saldoPendiente <= 0.0;
    }

    /**
     * Método helper para obtener el porcentaje pagado
     */
    public double getPorcentajePagado() {
        if (this.totalCredito == 0.0) return 100.0;
        return (this.totalAbonado / this.totalCredito) * 100.0;
    }
}