package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigo(String codigo);

    List<Producto> findByCategoriaIgnoreCase(String categoria);

    List<Producto> findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(
            String nombre, String codigo
    );
}