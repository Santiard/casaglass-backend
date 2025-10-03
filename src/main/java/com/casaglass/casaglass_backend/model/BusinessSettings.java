package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "business_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BusinessSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** IVA en porcentaje (0–100), ej: 19.0 */
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "iva_rate", nullable = false)
    private Double ivaRate = 19.0;

    /** Retención en la fuente (%) (0–100), ej: 2.5 */
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "rete_rate", nullable = false)
    private Double reteRate = 2.5;

    /** Umbral desde el que aplica retención (COP) */
    @NotNull
    @Min(0)
    @Column(name = "rete_threshold", nullable = false)
    private Long reteThreshold = 1_000_000L;

    /** Marca de tiempo simple (usa LocalDate para seguir tu estilo) */
    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDate updatedAt = LocalDate.now();

    @PrePersist
    @PreUpdate
    public void touchUpdatedAt() {
        this.updatedAt = LocalDate.now();
    }
}
