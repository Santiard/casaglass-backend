package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.ProductoActualizarDTO;
import com.casaglass.casaglass_backend.model.Categoria;
import com.casaglass.casaglass_backend.model.Inventario;
import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.model.TipoProducto;
import com.casaglass.casaglass_backend.model.ColorProducto;
import com.casaglass.casaglass_backend.repository.CategoriaRepository;
import com.casaglass.casaglass_backend.repository.InventarioRepository;
import com.casaglass.casaglass_backend.repository.ProductoRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductoService {

    private final ProductoRepository repo;
    private final CategoriaRepository categoriaRepo;
    private final InventarioRepository inventarioRepo;
    private final SedeRepository sedeRepo;

    public ProductoService(ProductoRepository repo, CategoriaRepository categoriaRepo,
                          InventarioRepository inventarioRepo, SedeRepository sedeRepo) {
        this.repo = repo;
        this.categoriaRepo = categoriaRepo;
        this.inventarioRepo = inventarioRepo;
        this.sedeRepo = sedeRepo;
    }

    public List<Producto> listar() {
        List<Producto> productos = repo.findAll();
        System.out.println("=== DEBUG PRODUCTOS ===");
        System.out.println("Total productos encontrados: " + productos.size());
        if (!productos.isEmpty()) {
            System.out.println("Primer producto: ID=" + productos.get(0).getId() + ", Nombre=" + productos.get(0).getNombre());
        }
        System.out.println("======================");
        return productos;
    }

    public Optional<Producto> obtenerPorId(Long id) {
        return repo.findById(id);
    }

    public Optional<Producto> obtenerPorCodigo(String codigo) {
        return repo.findByCodigo(codigo);
    }

    public List<Producto> listarPorCategoriaId(Long categoriaId) {
        return repo.findByCategoria_Id(categoriaId);
    }

    public List<Producto> buscar(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return repo.findAll();
        return repo.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(q, q);
    }

    public Producto guardar(Producto p) {
        // Validar categoría si viene con ID
        if (p.getCategoria() != null && p.getCategoria().getId() != null) {
            Categoria cat = categoriaRepo.findById(p.getCategoria().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
            p.setCategoria(cat);
        } else {
            p.setCategoria(null);
        }
        return repo.save(p);
    }

    public Producto actualizar(Long id, ProductoActualizarDTO dto) {
        return repo.findById(id).map(actual -> {
            try {
                // 🔧 NO TOCAR el version - Hibernate lo maneja automáticamente
                // actual.setVersion(dto.getVersion()); // ❌ NO hacer esto
                
                actual.setPosicion(dto.getPosicion());
                actual.setCodigo(dto.getCodigo());
                actual.setNombre(dto.getNombre());
                
                // Convertir String a Enum
                if (dto.getTipo() != null) {
                    actual.setTipo(TipoProducto.valueOf(dto.getTipo()));
                }
                if (dto.getColor() != null) {
                    actual.setColor(ColorProducto.valueOf(dto.getColor()));
                }
                
                actual.setCantidad(dto.getCantidad());
                actual.setCosto(dto.getCosto());
                actual.setPrecio1(dto.getPrecio1());
                actual.setPrecio2(dto.getPrecio2());
                actual.setPrecio3(dto.getPrecio3());
                actual.setDescripcion(dto.getDescripcion());

                // Actualizar categoría si se envía
                if (dto.getCategoria() != null && dto.getCategoria().getId() != null) {
                    Categoria cat = categoriaRepo.findById(dto.getCategoria().getId())
                            .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
                    actual.setCategoria(cat);
                } else {
                    actual.setCategoria(null);
                }

                Producto saved = repo.save(actual);
                
                // 📦 ACTUALIZAR INVENTARIO EN LAS 3 SEDES si se enviaron las cantidades
                if (dto.getCantidadInsula() != null || dto.getCantidadCentro() != null || dto.getCantidadPatios() != null) {
                    actualizarInventarioConValores(saved.getId(), 
                        dto.getCantidadInsula() != null ? dto.getCantidadInsula() : 0,
                        dto.getCantidadCentro() != null ? dto.getCantidadCentro() : 0,
                        dto.getCantidadPatios() != null ? dto.getCantidadPatios() : 0);
                }
                
                return saved;
                
            } catch (Exception e) {
                System.err.println("ERROR al actualizar producto: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Error al actualizar producto: " + e.getMessage(), e);
            }
        }).orElseThrow(() -> new RuntimeException("Producto no encontrado con id " + id));
    }
    
    // Método sobrecargado para mantener compatibilidad con el método anterior
    public Producto actualizar(Long id, Producto p) {
        ProductoActualizarDTO dto = new ProductoActualizarDTO();
        dto.setId(p.getId());
        dto.setPosicion(p.getPosicion());
        dto.setCodigo(p.getCodigo());
        dto.setNombre(p.getNombre());
        dto.setTipo(p.getTipo() != null ? p.getTipo().name() : null);
        dto.setColor(p.getColor() != null ? p.getColor().name() : null);
        dto.setCantidad(p.getCantidad());
        dto.setCosto(p.getCosto());
        dto.setPrecio1(p.getPrecio1());
        dto.setPrecio2(p.getPrecio2());
        dto.setPrecio3(p.getPrecio3());
        dto.setDescripcion(p.getDescripcion());
        dto.setCategoria(p.getCategoria());
        dto.setVersion(p.getVersion());
        return actualizar(id, dto);
    }
    
    /**
     * 📦 ACTUALIZAR INVENTARIO CON VALORES ESPECÍFICOS DEL FRONTEND
     * Actualiza el inventario en las 3 sedes con los valores exactos enviados desde el frontend
     * 
     * Nota: Permite valores negativos para manejar ventas anticipadas
     */
    private void actualizarInventarioConValores(Long productoId, Integer cantidadInsula, Integer cantidadCentro, Integer cantidadPatios) {
        // Obtener IDs de las 3 sedes
        Long insulaId = obtenerSedeId("insula");
        Long centroId = obtenerSedeId("centro");
        Long patiosId = obtenerSedeId("patios");
        
        if (insulaId == null || centroId == null || patiosId == null) {
            System.err.println("⚠️ No se encontraron las 3 sedes. No se actualizará el inventario.");
            return;
        }
        
        // Permitir valores negativos (ventas anticipadas) - usar 0 como default solo si es null
        cantidadInsula = cantidadInsula != null ? cantidadInsula : 0;
        cantidadCentro = cantidadCentro != null ? cantidadCentro : 0;
        cantidadPatios = cantidadPatios != null ? cantidadPatios : 0;
        
        System.out.println("📦 Actualizando inventario para producto " + productoId + " con valores del frontend:");
        System.out.println("   Insula (ID " + insulaId + "): " + cantidadInsula + 
                         (cantidadInsula < 0 ? " (⚠️ negativo)" : ""));
        System.out.println("   Centro (ID " + centroId + "): " + cantidadCentro + 
                         (cantidadCentro < 0 ? " (⚠️ negativo)" : ""));
        System.out.println("   Patios (ID " + patiosId + "): " + cantidadPatios + 
                         (cantidadPatios < 0 ? " (⚠️ negativo)" : ""));
        System.out.println("   Total: " + (cantidadInsula + cantidadCentro + cantidadPatios));
        
        // Actualizar o crear inventario para cada sede
        actualizarInventarioSede(productoId, insulaId, cantidadInsula);
        actualizarInventarioSede(productoId, centroId, cantidadCentro);
        actualizarInventarioSede(productoId, patiosId, cantidadPatios);
    }
    
    /**
     * Actualizar o crear inventario para un producto en una sede específica
     */
    private void actualizarInventarioSede(Long productoId, Long sedeId, Integer cantidad) {
        Optional<Inventario> inventarioOpt = inventarioRepo.findByProductoIdAndSedeId(productoId, sedeId);
        
        if (inventarioOpt.isPresent()) {
            // Actualizar inventario existente
            Inventario inventario = inventarioOpt.get();
            inventario.setCantidad(cantidad);
            inventarioRepo.save(inventario);
        } else {
            // Crear nuevo inventario
            Inventario nuevoInventario = new Inventario();
            nuevoInventario.setProducto(repo.getReferenceById(productoId));
            nuevoInventario.setSede(sedeRepo.getReferenceById(sedeId));
            nuevoInventario.setCantidad(cantidad);
            inventarioRepo.save(nuevoInventario);
        }
    }
    
    /**
     * Obtener ID de sede por nombre (búsqueda parcial, case-insensitive)
     */
    private Long obtenerSedeId(String nombreSede) {
        return sedeRepo.findByNombreContainingIgnoreCase(nombreSede)
            .stream()
            .findFirst()
            .map(Sede::getId)
            .orElse(null);
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    public List<String> listarCategoriasTexto() {
        return repo.findDistinctCategorias();
    }
}
