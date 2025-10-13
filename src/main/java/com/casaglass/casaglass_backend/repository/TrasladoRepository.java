package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Traslado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TrasladoRepository extends JpaRepository<Traslado, Long> {

    List<Traslado> findBySedeOrigenId(Long sedeOrigenId);

    List<Traslado> findBySedeDestinoId(Long sedeDestinoId);

    List<Traslado> findByFechaBetween(LocalDate desde, LocalDate hasta);

    List<Traslado> findBySedeOrigenIdAndSedeDestinoId(Long sedeOrigenId, Long sedeDestinoId);

    // 🔁 Query optimizada para cargar todas las relaciones y evitar LazyInitializationException
    @Query("""
        SELECT DISTINCT t FROM Traslado t 
        LEFT JOIN FETCH t.sedeOrigen 
        LEFT JOIN FETCH t.sedeDestino 
        LEFT JOIN FETCH t.trabajadorConfirmacion 
        LEFT JOIN FETCH t.detalles d 
        LEFT JOIN FETCH d.producto p 
        LEFT JOIN FETCH p.categoria
        ORDER BY t.fecha DESC, t.id DESC
        """)
    List<Traslado> findAllWithDetails();
}