package com.casaglass.casaglass_backend.model;

import java.util.List;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

@Entity
@Table(name = "traslados")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Traslado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "sede_origen_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Sede sedeOrigen;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "sede_destino_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Sede sedeDestino;

    @Column(nullable = false)
    private LocalDate fecha;

    // Trabajador que confirma llegada del traslado
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trabajador_confirmacion_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Trabajador trabajadorConfirmacion;

    // Fecha y hora de confirmación del traslado
    @Column(name = "fecha_confirmacion")
    private LocalDate fechaConfirmacion;

    @OneToMany(mappedBy = "traslado", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<TrasladoDetalle> detalles;
}
