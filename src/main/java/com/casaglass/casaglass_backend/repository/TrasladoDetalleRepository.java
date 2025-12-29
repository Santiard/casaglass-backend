package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.TrasladoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrasladoDetalleRepository extends JpaRepository<TrasladoDetalle, Long> {

    List<TrasladoDetalle> findByTrasladoId(Long trasladoId);

    // Buscar detalles por producto
    List<TrasladoDetalle> findByProducto(com.casaglass.casaglass_backend.model.Producto producto);
}
