package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.TrasladoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrasladoDetalleRepository extends JpaRepository<TrasladoDetalle, Long> {

    List<TrasladoDetalle> findByTrasladoId(Long trasladoId);

    // Buscar detalles por producto
    List<TrasladoDetalle> findByProducto(com.casaglass.casaglass_backend.model.Producto producto);
    
    // Eliminar por ID usando consulta nativa para asegurar ejecución
    @Modifying
    @Query(value = "DELETE FROM traslado_detalles WHERE id = :id", nativeQuery = true)
    void deleteByIdNative(@Param("id") Long id);
}
