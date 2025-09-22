package com.casaglass.casaglass_backend.model;

import java.util.List;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Entity
@Table(name = "traslados")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Traslado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sede_origen_id", nullable = false)
    private Sede sedeOrigen;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sede_destino_id", nullable = false)
    private Sede sedeDestino;

    @Column(nullable = false)
    private LocalDateTime fecha;

    // Trabajador que confirma llegada del traslado
    @ManyToOne
    @JoinColumn(name = "trabajador_confirmacion_id")
    private Trabajador trabajadorConfirmacion;

    @OneToMany(mappedBy = "traslado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrasladoDetalle> detalles;
}
