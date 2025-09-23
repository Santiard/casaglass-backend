package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    List<Inventario> findByProductoId(Long productoId);

    List<Inventario> findBySedeId(Long sedeId);

    Optional<Inventario> findByProductoIdAndSedeId(Long productoId, Long sedeId);
}