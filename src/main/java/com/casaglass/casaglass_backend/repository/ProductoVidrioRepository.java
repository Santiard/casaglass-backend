package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.ProductoVidrio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductoVidrioRepository extends JpaRepository<ProductoVidrio, Long> {

    // Campo "codigo" heredado de Producto
    Optional<ProductoVidrio> findByCodigo(String codigo);

    // Búsquedas útiles (opcionales)
    List<ProductoVidrio> findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(String nombre, String codigo);

    // Filtros específicos
    List<ProductoVidrio> findByMm(Double mm);
    
    // 🔁 Métodos para filtrar por Categoria (heredada de Producto)
    List<ProductoVidrio> findByCategoria_Id(Long categoriaId);
    List<ProductoVidrio> findByCategoria_NombreIgnoreCase(String nombre);
    
    /**
     * 🔧 QUERY ALTERNATIVO: Obtener productos vidrio haciendo JOIN explícito
     * Esto asegura que encontremos todos los productos que tienen registro en productos_vidrio
     */
    @Query("SELECT pv FROM ProductoVidrio pv " +
           "LEFT JOIN FETCH pv.categoria")
    List<ProductoVidrio> findAllWithExplicitJoin();
    
    /**
     * 🔧 QUERY ALTERNATIVO 2: Obtener IDs de productos que tienen registro en productos_vidrio
     * Usa query nativo para verificar directamente en la tabla
     * IMPORTANTE: Este query verifica que exista el registro en productos_vidrio
     */
    @Query(value = "SELECT pv.id FROM productos_vidrio pv", nativeQuery = true)
    List<Long> findProductoVidrioIds();
}