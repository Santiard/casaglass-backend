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

import jakarta.persistence.EntityNotFoundException;
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
    
    /**
     * 📦 Crea registros de inventario con cantidad 0 para las 3 sedes
     * Esto asegura que el producto aparezca en el inventario completo
     */
    private void crearInventarioInicial(Producto producto) {
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
                System.out.println("✅ Inventario creado: Producto ID=" + producto.getId() + 
                                 ", Sede ID=" + sedeId + ", Cantidad=0");
            }
        }
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

    /**
     * 🚀 LISTADO DE PRODUCTOS CON FILTROS COMPLETOS
     * Acepta múltiples filtros opcionales y retorna lista o respuesta paginada
     * Nota: conStock requiere verificar inventario, se filtra después de obtener productos
     */
    @Transactional(readOnly = true)
    public Object listarProductosConFiltros(
            Long categoriaId,
            String categoriaNombre,
            TipoProducto tipo,
            ColorProducto color,
            String codigo,
            String nombre,
            Boolean conStock,
            Long sedeId,
            Integer page,
            Integer size,
            String sortBy,
            String sortOrder) {
        
        // Validar y normalizar ordenamiento
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "codigo";
        }
        if (sortOrder == null || sortOrder.isEmpty()) {
            sortOrder = "ASC";
        }
        sortOrder = sortOrder.toUpperCase();
        if (!sortOrder.equals("ASC") && !sortOrder.equals("DESC")) {
            sortOrder = "ASC";
        }
        
        // Buscar productos con filtros
        List<Producto> productos = repo.buscarConFiltros(
            categoriaId, categoriaNombre, tipo, color, codigo, nombre
        );
        
        // Filtrar por stock si se solicita (requiere verificar inventario)
        if (conStock != null && conStock && sedeId != null) {
            productos = productos.stream()
                    .filter(p -> {
                        Optional<Inventario> inventario = inventarioRepo.findByProductoIdAndSedeId(p.getId(), sedeId);
                        return inventario.isPresent() && inventario.get().getCantidad() != null && inventario.get().getCantidad() > 0;
                    })
                    .collect(java.util.stream.Collectors.toList());
        }
        
        // Aplicar ordenamiento adicional si es necesario (el query ya ordena por codigo ASC)
        if (!sortBy.equals("codigo") || !sortOrder.equals("ASC")) {
            productos = aplicarOrdenamientoProductos(productos, sortBy, sortOrder);
        }
        
        // Si se solicita paginación
        if (page != null && size != null) {
            // Validar y ajustar parámetros
            if (page < 1) page = 1;
            if (size < 1) size = 50;
            if (size > 200) size = 200; // Límite máximo para productos
            
            long totalElements = productos.size();
            
            // Calcular índices para paginación
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, productos.size());
            
            if (fromIndex >= productos.size()) {
                // Página fuera de rango, retornar lista vacía
                return com.casaglass.casaglass_backend.dto.PageResponse.of(
                    new java.util.ArrayList<>(), totalElements, page, size
                );
            }
            
            // Obtener solo la página solicitada
            List<Producto> contenido = productos.subList(fromIndex, toIndex);
            
            return com.casaglass.casaglass_backend.dto.PageResponse.of(contenido, totalElements, page, size);
        }
        
        // Sin paginación: retornar lista completa
        return productos;
    }
    
    /**
     * Aplica ordenamiento a la lista de productos según sortBy y sortOrder
     */
    private List<Producto> aplicarOrdenamientoProductos(List<Producto> productos, String sortBy, String sortOrder) {
        boolean ascendente = "ASC".equals(sortOrder);
        
        switch (sortBy.toLowerCase()) {
            case "codigo":
                productos.sort((a, b) -> {
                    int cmp = (a.getCodigo() != null ? a.getCodigo() : "").compareToIgnoreCase(b.getCodigo() != null ? b.getCodigo() : "");
                    return ascendente ? cmp : -cmp;
                });
                break;
            case "nombre":
                productos.sort((a, b) -> {
                    int cmp = (a.getNombre() != null ? a.getNombre() : "").compareToIgnoreCase(b.getNombre() != null ? b.getNombre() : "");
                    return ascendente ? cmp : -cmp;
                });
                break;
            case "categoria":
                productos.sort((a, b) -> {
                    String catA = a.getCategoria() != null && a.getCategoria().getNombre() != null ? a.getCategoria().getNombre() : "";
                    String catB = b.getCategoria() != null && b.getCategoria().getNombre() != null ? b.getCategoria().getNombre() : "";
                    int cmp = catA.compareToIgnoreCase(catB);
                    return ascendente ? cmp : -cmp;
                });
                break;
            default:
                // Por defecto ordenar por codigo ASC
                productos.sort((a, b) -> (a.getCodigo() != null ? a.getCodigo() : "").compareToIgnoreCase(b.getCodigo() != null ? b.getCodigo() : ""));
        }
        
        return productos;
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
        
        // Guardar el producto primero
        Producto productoGuardado = repo.save(p);
        
        // ✅ Crear inventario con cantidad 0 para las 3 sedes automáticamente
        crearInventarioInicial(productoGuardado);
        
        return productoGuardado;
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
                // ✅ Actualizar costo explícitamente (permite null y 0)
                System.out.println("🔧 DEBUG: Actualizando costo - DTO costo: " + dto.getCosto() + ", Costo actual antes: " + actual.getCosto());
                actual.setCosto(dto.getCosto());
                System.out.println("🔧 DEBUG: Costo actual después de set: " + actual.getCosto());
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

                // ✅ Usar saveAndFlush para forzar la persistencia inmediata
                Producto saved = repo.saveAndFlush(actual);
                System.out.println("🔧 DEBUG: Costo guardado en BD: " + saved.getCosto());
                
                // 📦 ACTUALIZAR INVENTARIO EN LAS 3 SEDES si se enviaron las cantidades
                if (dto.getCantidadInsula() != null || dto.getCantidadCentro() != null || dto.getCantidadPatios() != null) {
                    actualizarInventarioConValores(saved.getId(), 
                        dto.getCantidadInsula() != null ? dto.getCantidadInsula() : 0,
                        dto.getCantidadCentro() != null ? dto.getCantidadCentro() : 0,
                        dto.getCantidadPatios() != null ? dto.getCantidadPatios() : 0);
                }
                
                return saved;
                
            } catch (jakarta.persistence.OptimisticLockException e) {
                // 🔒 Lock optimista: Otro proceso modificó el producto (muy raro)
                System.err.println("⚠️ Conflicto de versión (lock optimista) en producto ID " + id);
                System.err.println("⚠️ Tipo: " + e.getClass().getName());
                System.err.println("⚠️ Mensaje: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(
                    String.format("⚠️ Otro usuario modificó el producto ID %d. Por favor, recargue e intente nuevamente.", id)
                );
            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                // 🔒 Variante de Spring para OptimisticLockException
                System.err.println("⚠️ Conflicto de versión (Spring) en producto ID " + id);
                System.err.println("⚠️ Tipo: " + e.getClass().getName());
                System.err.println("⚠️ Mensaje: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(
                    String.format("⚠️ Otro usuario modificó el producto ID %d. Por favor, recargue e intente nuevamente.", id)
                );
            } catch (Exception e) {
                System.err.println("ERROR al actualizar producto ID " + id);
                System.err.println("ERROR Tipo: " + e.getClass().getName());
                System.err.println("ERROR Mensaje: " + e.getMessage());
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
     * 💰 ACTUALIZAR SOLO EL COSTO DE UN PRODUCTO
     * Endpoint específico para actualizar únicamente el costo, evitando problemas con otros campos
     */
    public Producto actualizarCosto(Long id, Double nuevoCosto) {
        Producto producto = repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + id));
        
        producto.setCosto(nuevoCosto);
        Producto saved = repo.saveAndFlush(producto);
        
        System.out.println("🔧 DEBUG: Costo actualizado - ID: " + id + ", Nuevo costo: " + nuevoCosto + ", Costo guardado en BD: " + saved.getCosto());
        return saved;
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
