package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.ProductoVidrio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoVidrioRepository extends JpaRepository<ProductoVidrio, Long> {

    // Campo "codigo" heredado de Producto
    Optional<ProductoVidrio> findByCodigo(String codigo);

    // Búsquedas útiles (opcionales)
    List<ProductoVidrio> findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(String nombre, String codigo);

    // Filtros específicos
    List<ProductoVidrio> findByMm(Double mm);
    List<ProductoVidrio> findByLaminas(Integer laminas);
}