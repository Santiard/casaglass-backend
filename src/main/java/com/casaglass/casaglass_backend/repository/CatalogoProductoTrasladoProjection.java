package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.ColorProducto;

public interface CatalogoProductoTrasladoProjection {
    Long getId();
    String getCodigo();
    String getNombre();
    Long getCategoriaId();
    String getCategoriaNombre();
    ColorProducto getColor();
    Double getCantidadSedeOrigen();
    Double getCantidadTotal();
    Double getPrecio1();
    Double getPrecio2();
    Double getPrecio3();
}
