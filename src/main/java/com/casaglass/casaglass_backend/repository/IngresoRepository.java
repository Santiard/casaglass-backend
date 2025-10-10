package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Ingreso;
import com.casaglass.casaglass_backend.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IngresoRepository extends JpaRepository<Ingreso, Long> {

    // Buscar ingresos por proveedor
    List<Ingreso> findByProveedorOrderByFechaDesc(Proveedor proveedor);

    // Buscar ingresos por rango de fechas
  List<Ingreso> findByFechaBetweenOrderByFechaDesc(LocalDate fechaInicio, LocalDate fechaFin);

    // Buscar ingresos no procesados
    List<Ingreso> findByProcesadoFalseOrderByFechaAsc();

    // Buscar ingresos por número de factura
    List<Ingreso> findByNumeroFacturaContainingIgnoreCase(String numeroFactura);

    // Obtener ingresos con sus detalles (para evitar N+1)
    @Query("""
      select i
      from Ingreso i
      left join fetch i.proveedor p
      left join fetch i.detalles d
      left join fetch d.producto prod
      where i.id = :id
    """)
    Ingreso findByIdWithDetalles(@Param("id") Long id);

    // Obtener todos los ingresos con sus proveedores
    @Query("""
      select distinct i
      from Ingreso i
      left join fetch i.proveedor p
      left join fetch i.detalles d
      left join fetch d.producto prod
      order by i.fecha desc
    """)
    List<Ingreso> findAllWithProveedores(); 

    // Contar ingresos por proveedor
    @Query("SELECT COUNT(i) FROM Ingreso i WHERE i.proveedor = :proveedor")
    Long countByProveedor(@Param("proveedor") Proveedor proveedor);
}