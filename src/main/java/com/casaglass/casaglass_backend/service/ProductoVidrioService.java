package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Categoria;
import com.casaglass.casaglass_backend.model.Inventario;
import com.casaglass.casaglass_backend.model.ProductoVidrio;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.repository.CategoriaRepository;
import com.casaglass.casaglass_backend.repository.InventarioRepository;
import com.casaglass.casaglass_backend.repository.ProductoVidrioRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductoVidrioService {

    private final ProductoVidrioRepository repo;
    private final CategoriaRepository categoriaRepo;
    private final InventarioRepository inventarioRepo;
    private final SedeRepository sedeRepo;
    
    @PersistenceContext
    private EntityManager entityManager;

    public ProductoVidrioService(ProductoVidrioRepository repo, 
                                 CategoriaRepository categoriaRepo,
                                 InventarioRepository inventarioRepo,
                                 SedeRepository sedeRepo) {
        this.repo = repo;
        this.categoriaRepo = categoriaRepo;
        this.inventarioRepo = inventarioRepo;
        this.sedeRepo = sedeRepo;
    }

    public List<ProductoVidrio> listar() {
        return repo.findAll();
    }

    public Optional<ProductoVidrio> obtenerPorId(Long id) {
        return repo.findById(id);
    }

    public Optional<ProductoVidrio> obtenerPorCodigo(String codigo) {
        return repo.findByCodigo(codigo);
    }

    public List<ProductoVidrio> buscar(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return repo.findAll();
        return repo.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(q, q);
    }

    public List<ProductoVidrio> listarPorMm(Double mm) {
        return repo.findByMm(mm);
    }


    public List<ProductoVidrio> listarPorCategoriaId(Long categoriaId) {
        return repo.findByCategoria_Id(categoriaId);
    }

    public ProductoVidrio guardar(ProductoVidrio p) {

        // Validar categoría si viene con ID
        if (p.getCategoria() != null && p.getCategoria().getId() != null) {
            Categoria cat = categoriaRepo.findById(p.getCategoria().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
            p.setCategoria(cat);
        } else {
            p.setCategoria(null);
        }
        
        // ✅ m1m2 se calcula automáticamente mediante @PrePersist antes de guardar
        // Solo asegurarnos de que m1m2 tenga un valor si m1 o m2 son null
        if (p.getM1m2() == null) {
            if (p.getM1() != null && p.getM2() != null) {
                p.setM1m2(p.getM1() * p.getM2());
            } else {
                p.setM1m2(0.0);
            }
        }
        
        // ✅ USAR entityManager.persist() DIRECTAMENTE para forzar que Hibernate detecte el tipo
        // Esto asegura que Hibernate cree el registro en productos_vidrio
        entityManager.persist(p);
        entityManager.flush();
        entityManager.refresh(p); // Refrescar para obtener el ID generado
        
        // ✅ VERIFICAR que se creó el registro en productos_vidrio usando query nativo
        Long idGuardado = p.getId();
        
        // Verificar con query nativo directo
        jakarta.persistence.Query query = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM productos_vidrio WHERE id = ?1"
        );
        query.setParameter(1, idGuardado);
        Long count = ((Number) query.getSingleResult()).longValue();
        
        if (count == 0) {
            // 🔧 SOLUCIÓN DE EMERGENCIA: Insertar manualmente en productos_vidrio
            try {
                jakarta.persistence.Query insertQuery = entityManager.createNativeQuery(
                    "INSERT INTO productos_vidrio (id, mm, m1, m2, m1m2) VALUES (?1, ?2, ?3, ?4, ?5)"
                );
                insertQuery.setParameter(1, idGuardado);
                insertQuery.setParameter(2, p.getMm());
                insertQuery.setParameter(3, p.getM1());
                insertQuery.setParameter(4, p.getM2());
                insertQuery.setParameter(5, p.getM1m2());
                insertQuery.executeUpdate();
                entityManager.flush();
            } catch (Exception e) {
                throw new RuntimeException("Error al insertar producto vidrio: " + e.getMessage(), e);
            }
        }
        
        // ✅ Crear inventario con cantidad 0 para las 3 sedes automáticamente
        crearInventarioInicial(p);
        
        return p;
    }
    
    /**
     * 📦 Crea registros de inventario con cantidad 0 para las 3 sedes
     * Esto asegura que el producto aparezca en el inventario completo
     */
    private void crearInventarioInicial(ProductoVidrio producto) {
        // IDs de las 3 sedes (Insula=1, Centro=2, Patios=3)
        Long[] sedesIds = {1L, 2L, 3L};
        
        for (Long sedeId : sedesIds) {
            // Verificar si ya existe un registro de inventario para este producto y sede
            boolean existeInventario = inventarioRepo.findByProductoIdAndSedeId(producto.getId(), sedeId)
                    .isPresent();
            
            if (!existeInventario) {
                Sede sede = sedeRepo.findById(sedeId)
                        .orElseThrow(() -> new RuntimeException("Sede no encontrada con ID: " + sedeId));
                
                Inventario inventario = new Inventario();
                inventario.setProducto(producto);
                inventario.setSede(sede);
                inventario.setCantidad(0);
                
                inventarioRepo.save(inventario);
            }
        }
    }

    public ProductoVidrio actualizar(Long id, ProductoVidrio p) {
        return repo.findById(id).map(actual -> {
            // Campos heredados de Producto
            actual.setPosicion(p.getPosicion());
            actual.setCodigo(p.getCodigo());
            actual.setNombre(p.getNombre());
            actual.setColor(p.getColor());
            actual.setCantidad(p.getCantidad());
            actual.setCosto(p.getCosto());
            actual.setPrecio1(p.getPrecio1());
            actual.setPrecio2(p.getPrecio2());
            actual.setPrecio3(p.getPrecio3());
            actual.setDescripcion(p.getDescripcion());

            // Actualizar categoría si se envía
            if (p.getCategoria() != null && p.getCategoria().getId() != null) {
                Categoria cat = categoriaRepo.findById(p.getCategoria().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
                actual.setCategoria(cat);
            } else {
                actual.setCategoria(null);
            }

            // Campos específicos de ProductoVidrio
            actual.setMm(p.getMm());
            actual.setM1(p.getM1());
            actual.setM2(p.getM2());
            // ✅ m1m2 se calcula automáticamente mediante @PreUpdate antes de guardar

            return repo.save(actual);
        }).orElseThrow(() -> new RuntimeException("ProductoVidrio no encontrado con id " + id));
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}