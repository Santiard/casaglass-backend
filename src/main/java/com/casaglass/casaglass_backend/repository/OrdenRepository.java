package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Orden;
import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    Optional<Orden> findByNumero(Long numero);

    List<Orden> findByClienteId(Long clienteId);

    List<Orden> findByVenta(boolean venta);     // true = ventas, false = cotizaciones

    List<Orden> findByCredito(boolean credito); // true = a crédito

    List<Orden> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta);

    List<Orden> findBySedeId(Long sedeId);

    List<Orden> findByClienteIdAndSedeId(Long clienteId, Long sedeId);

    List<Orden> findBySedeIdAndVenta(Long sedeId, boolean venta);

    List<Orden> findBySedeIdAndCredito(Long sedeId, boolean credito);

    List<Orden> findBySedeIdAndFechaBetween(Long sedeId, LocalDateTime desde, LocalDateTime hasta);

    List<Orden> findAllById(Iterable<Long> ids);
}