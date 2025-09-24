package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Ingreso;
import com.casaglass.casaglass_backend.model.IngresoDetalle;
import com.casaglass.casaglass_backend.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngresoDetalleRepository extends JpaRepository<IngresoDetalle, Long> {

    // Buscar detalles por ingreso
    List<IngresoDetalle> findByIngresoOrderByIdAsc(Ingreso ingreso);

    // Buscar detalles por producto
    List<IngresoDetalle> findByProductoOrderByIngreso_FechaDesc(Producto producto);

    // Obtener detalles con información completa para un ingreso
    @Query("SELECT id FROM IngresoDetalle id JOIN FETCH id.producto p WHERE id.ingreso = :ingreso")
    List<IngresoDetalle> findByIngresoWithProducto(@Param("ingreso") Ingreso ingreso);

    // Obtener cantidad total ingresada de un producto
    @Query("SELECT COALESCE(SUM(id.cantidad), 0) FROM IngresoDetalle id WHERE id.producto = :producto")
    Integer getTotalCantidadByProducto(@Param("producto") Producto producto);

    // Obtener historial de ingresos de un producto
    @Query("SELECT id FROM IngresoDetalle id JOIN FETCH id.ingreso i JOIN FETCH i.proveedor " +
           "WHERE id.producto = :producto ORDER BY i.fecha DESC")
    List<IngresoDetalle> getHistorialIngresosByProducto(@Param("producto") Producto producto);
}