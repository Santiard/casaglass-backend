package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Orden;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    Optional<Orden> findByNumero(Long numero);

    List<Orden> findByClienteId(Long clienteId);

    List<Orden> findByVenta(boolean venta);     // true = ventas, false = cotizaciones

    List<Orden> findByCredito(boolean credito); // true = a crédito

    List<Orden> findByFechaBetween(LocalDate desde, LocalDate hasta);

    List<Orden> findBySedeId(Long sedeId);

    List<Orden> findByClienteIdAndSedeId(Long clienteId, Long sedeId);

    List<Orden> findBySedeIdAndVenta(Long sedeId, boolean venta);

    List<Orden> findBySedeIdAndCredito(Long sedeId, boolean credito);

    List<Orden> findBySedeIdAndFechaBetween(Long sedeId, LocalDate desde, LocalDate hasta);

    // 🆕 Métodos para filtrar por trabajador
    List<Orden> findByTrabajadorId(Long trabajadorId);

    List<Orden> findByTrabajadorIdAndVenta(Long trabajadorId, boolean venta);

    List<Orden> findByTrabajadorIdAndFechaBetween(Long trabajadorId, LocalDate desde, LocalDate hasta);

    List<Orden> findBySedeIdAndTrabajadorId(Long sedeId, Long trabajadorId);

    List<Orden> findAllById(Iterable<Long> ids);

    // Método para obtener el siguiente número de orden disponible (thread-safe)
    @Query("SELECT COALESCE(MAX(o.numero), 0) + 1 FROM Orden o")
    Long obtenerSiguienteNumero();
    @EntityGraph(attributePaths = {"cliente", "sede", "items", "items.producto"})
    @Query("SELECT o FROM Orden o")
    List<Orden> findAllWithFullRelations();
}