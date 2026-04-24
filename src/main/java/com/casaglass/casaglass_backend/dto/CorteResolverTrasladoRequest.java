package com.casaglass.casaglass_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Localizar o crear un {@link com.casaglass.casaglass_backend.model.Corte} para líneas de traslado
 * cuando en UI se corta desde un <strong>producto entero</strong> (perfil) y hace falta un id de BD
 * aunque el producto de catálogo no venga con toda la meta en el JSON del front.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorteResolverTrasladoRequest {
    /** Id del <strong>producto entero</strong> (no corte) en <code>productos</code>. */
    private Long productoPerfilId;
    /** Medida del corte en centímetros (misma unidad que {@code Corte.largoCm} en flujos de venta). */
    private Integer medidaCm;
}
