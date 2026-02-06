package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.ProductoActualizarDTO;
import com.casaglass.casaglass_backend.dto.ProductoPosicionDTO;
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
import com.casaglass.casaglass_backend.repository.IngresoDetalleRepository;
import com.casaglass.casaglass_backend.repository.TrasladoDetalleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductoService {

    private final IngresoDetalleRepository ingresoDetalleRepo;
    private final TrasladoDetalleRepository trasladoDetalleRepo;

    private final ProductoRepository repo;
    private final CategoriaRepository categoriaRepo;
    private final InventarioRepository inventarioRepo;
    private final SedeRepository sedeRepo;

    public ProductoService(ProductoRepository repo, CategoriaRepository categoriaRepo,
                          InventarioRepository inventarioRepo, SedeRepository sedeRepo,
                          IngresoDetalleRepository ingresoDetalleRepo, TrasladoDetalleRepository trasladoDetalleRepo) {
        this.repo = repo;
        this.categoriaRepo = categoriaRepo;
        this.inventarioRepo = inventarioRepo;
        this.sedeRepo = sedeRepo;
        this.ingresoDetalleRepo = ingresoDetalleRepo;
        this.trasladoDetalleRepo = trasladoDetalleRepo;
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
                inventario.setCantidad(0.0);
                
                inventarioRepo.save(inventario);
            }
        }
    }

    public List<Producto> listar() {
        List<Producto> productos = repo.findAll();
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

    /**
     * 💾 GUARDAR PRODUCTO CON MANEJO DE POSICIÓN
     * 
     * Si se especifica una posición, se inserta el producto en esa posición
     * y se corren todos los productos posteriores (sumando 1 a su posición).
     * 
     * Si no se especifica posición, se asigna la última posición + 1.
     * 
     * @param p Producto a guardar
     * @return Producto guardado con posición asignada
     */
    public Producto guardar(Producto p) {
        // Validar categoría si viene con ID
        if (p.getCategoria() != null && p.getCategoria().getId() != null) {
            Categoria cat = categoriaRepo.findById(p.getCategoria().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
            p.setCategoria(cat);
        } else {
            p.setCategoria(null);
        }
        
        // 📍 MANEJO DE POSICIÓN
        String posicionSolicitada = p.getPosicion();
        
        if (posicionSolicitada != null && !posicionSolicitada.trim().isEmpty()) {
            // Intentar parsear la posición como número
            try {
                Long posicionNumerica = Long.parseLong(posicionSolicitada.trim());
                
                // Validar que la posición sea positiva
                if (posicionNumerica <= 0) {
                    throw new IllegalArgumentException("La posición debe ser un número positivo mayor a 0");
                }
                
                // Correr todos los productos con posición >= a la solicitada
                correrPosicionesProductos(posicionNumerica);
                
                // Asignar la posición al nuevo producto
                p.setPosicion(String.valueOf(posicionNumerica));
                
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("La posición debe ser un número válido. Valor recibido: " + posicionSolicitada);
            }
        } else {
            // Si no viene posición, asignar la última posición + 1
            Long maximaPosicion = repo.obtenerMaximaPosicion();
            Long nuevaPosicion = (maximaPosicion != null) ? maximaPosicion + 1 : 1;
            p.setPosicion(String.valueOf(nuevaPosicion));
        }
        
        // Guardar el producto
        Producto productoGuardado = repo.save(p);
        
        // ✅ Crear inventario con cantidad 0 para las 3 sedes automáticamente
        crearInventarioInicial(productoGuardado);
        
        return productoGuardado;
    }

    /**
     * 🔄 CORRER POSICIONES DE PRODUCTOS
     * 
     * Cuando se inserta un producto en una posición específica, todos los productos
     * con posición >= a esa posición deben correrse hacia abajo (sumar 1).
     * 
     * Ejemplo:
     * - Si insertas en posición 5, los productos en posición 5, 6, 7, 8... pasan a 6, 7, 8, 9...
     * 
     * @param posicionInicial Posición desde la cual correr los productos
     */
    private void correrPosicionesProductos(Long posicionInicial) {
        // Obtener todos los productos con posición (excluye Cortes)
        List<Producto> todosLosProductosConPosicion = repo.encontrarProductosConPosicion();
        
        // Filtrar en Java: solo productos con posición >= posicionInicial
        List<Producto> productosACorrer = todosLosProductosConPosicion.stream()
                .filter(p -> {
                    try {
                        Long posicion = Long.parseLong(p.getPosicion());
                        return posicion >= posicionInicial;
                    } catch (NumberFormatException e) {
                        return false; // Si no se puede parsear, excluir
                    }
                })
                .collect(Collectors.toList());
        
        // Ordenar por posición descendente para evitar conflictos al actualizar
        productosACorrer.sort((a, b) -> {
            try {
                Long posA = Long.parseLong(a.getPosicion());
                Long posB = Long.parseLong(b.getPosicion());
                return posB.compareTo(posA); // Orden descendente
            } catch (NumberFormatException e) {
                return 0;
            }
        });
        
        // Correr cada producto sumando 1 a su posición
        for (Producto producto : productosACorrer) {
            try {
                Long posicionActual = Long.parseLong(producto.getPosicion());
                Long nuevaPosicion = posicionActual + 1;
                producto.setPosicion(String.valueOf(nuevaPosicion));
                repo.save(producto);
            } catch (NumberFormatException e) {
                // Si hay un error al parsear, saltar este producto
                // (no debería pasar si la consulta funciona correctamente)
                continue;
            }
        }
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

                // ✅ Usar saveAndFlush para forzar la persistencia inmediata
                Producto saved = repo.saveAndFlush(actual);
                
                // 📦 ACTUALIZAR INVENTARIO EN LAS 3 SEDES si se enviaron las cantidades
                if (dto.getCantidadInsula() != null || dto.getCantidadCentro() != null || dto.getCantidadPatios() != null) {
                    actualizarInventarioConValores(saved.getId(), 
                        dto.getCantidadInsula() != null ? dto.getCantidadInsula().doubleValue() : 0.0,
                        dto.getCantidadCentro() != null ? dto.getCantidadCentro().doubleValue() : 0.0,
                        dto.getCantidadPatios() != null ? dto.getCantidadPatios().doubleValue() : 0.0);
                }
                
                return saved;
                
            } catch (jakarta.persistence.OptimisticLockException e) {
                // 🔒 Lock optimista: Otro proceso modificó el producto (muy raro)
                throw new RuntimeException(
                    String.format("⚠️ Otro usuario modificó el producto ID %d. Por favor, recargue e intente nuevamente.", id)
                );
            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                // 🔒 Variante de Spring para OptimisticLockException
                throw new RuntimeException(
                    String.format("⚠️ Otro usuario modificó el producto ID %d. Por favor, recargue e intente nuevamente.", id)
                );
            } catch (Exception e) {
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
        
        return saved;
    }
    
    /**
     * 📦 ACTUALIZAR INVENTARIO CON VALORES ESPECÍFICOS DEL FRONTEND
     * Actualiza el inventario en las 3 sedes con los valores exactos enviados desde el frontend
     * 
     * Nota: Permite valores negativos para manejar ventas anticipadas
     */
    private void actualizarInventarioConValores(Long productoId, Double cantidadInsula, Double cantidadCentro, Double cantidadPatios) {
        // Obtener IDs de las 3 sedes
        Long insulaId = obtenerSedeId("insula");
        Long centroId = obtenerSedeId("centro");
        Long patiosId = obtenerSedeId("patios");
        
        if (insulaId == null || centroId == null || patiosId == null) {
            return;
        }
        // Permitir valores negativos (ventas anticipadas) - usar 0 como default solo si es null
        cantidadInsula = cantidadInsula != null ? cantidadInsula : 0.0;
        cantidadCentro = cantidadCentro != null ? cantidadCentro : 0.0;
        cantidadPatios = cantidadPatios != null ? cantidadPatios : 0.0;
        // Actualizar o crear inventario para cada sede
        actualizarInventarioSede(productoId, insulaId, cantidadInsula);
        actualizarInventarioSede(productoId, centroId, cantidadCentro);
        actualizarInventarioSede(productoId, patiosId, cantidadPatios);
    }
    
    /**
     * Actualizar o crear inventario para un producto en una sede específica
     */
    private void actualizarInventarioSede(Long productoId, Long sedeId, Double cantidad) {
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
        Producto producto = repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));

        // Verificar si tiene movimientos en ingresos
        if (!ingresoDetalleRepo.findByProductoOrderByIngreso_FechaDesc(producto).isEmpty()) {
            throw new RuntimeException("No se puede eliminar el producto porque tiene movimientos de ingreso asociados");
        }
        // Verificar si tiene movimientos en traslados
        if (!trasladoDetalleRepo.findByProducto(producto).isEmpty()) {
            throw new RuntimeException("No se puede eliminar el producto porque tiene movimientos de traslado asociados");
        }
        // TODO: Agregar validación para ventas/órdenes si aplica

        // Eliminar inventario asociado
        List<Inventario> inventarios = inventarioRepo.findByProductoId(id);
        for (Inventario inv : inventarios) {
            inventarioRepo.delete(inv);
        }

        // Eliminar el producto
        repo.delete(producto);
    }

    public List<String> listarCategoriasTexto() {
        return repo.findDistinctCategorias();
    }

    /**
     * 📍 LISTAR PRODUCTOS PARA TABLA DE POSICIONES
     * 
     * Retorna solo los campos necesarios para mostrar la tabla de posiciones:
     * - id, codigo, nombre, color, posicion, categoria
     * 
     * Incluye productos normales y ProductoVidrio
     * Excluye Cortes
     * 
     * Ordenamiento:
     * - Productos con posición: ordenados por posición numérica ascendente
     * - Productos sin posición: al final del array
     * 
     * @param categoriaId (opcional) Filtrar por categoría específica
     * @return Lista de ProductoPosicionDTO ordenados por posición
     */
    @Transactional(readOnly = true)
    public List<ProductoPosicionDTO> listarProductosParaPosiciones(Long categoriaId) {
        // Obtener todos los productos (incluyendo ProductoVidrio)
        List<Producto> todosLosProductos = repo.findAll();
        
        // Filtrar Cortes (excluir)
        List<Producto> productos = todosLosProductos.stream()
                .filter(p -> !(p instanceof com.casaglass.casaglass_backend.model.Corte))
                .collect(Collectors.toList());
        
        // Filtrar por categoría si se especifica
        if (categoriaId != null) {
            productos = productos.stream()
                    .filter(p -> p.getCategoria() != null && p.getCategoria().getId().equals(categoriaId))
                    .collect(Collectors.toList());
        }
        
        // Convertir a DTO
        List<ProductoPosicionDTO> dtos = productos.stream()
                .map(this::convertirAProductoPosicionDTO)
                .collect(Collectors.toList());
        
        // Ordenar: productos con posición primero (por posición numérica), luego sin posición
        dtos.sort((a, b) -> {
            // Si ambos tienen posición, ordenar numéricamente
            if (a.getPosicion() != null && b.getPosicion() != null) {
                try {
                    Long posA = Long.parseLong(a.getPosicion());
                    Long posB = Long.parseLong(b.getPosicion());
                    return posA.compareTo(posB);
                } catch (NumberFormatException e) {
                    // Si hay error al parsear, mantener orden original
                    return 0;
                }
            }
            // Si solo 'a' tiene posición, va primero
            if (a.getPosicion() != null && b.getPosicion() == null) {
                return -1;
            }
            // Si solo 'b' tiene posición, va primero
            if (a.getPosicion() == null && b.getPosicion() != null) {
                return 1;
            }
            // Si ninguno tiene posición, mantener orden original
            return 0;
        });
        
        return dtos;
    }

    /**
     * 🔄 CONVERTIR Producto a ProductoPosicionDTO
     */
    private ProductoPosicionDTO convertirAProductoPosicionDTO(Producto producto) {
        ProductoPosicionDTO dto = new ProductoPosicionDTO();
        dto.setId(producto.getId());
        dto.setCodigo(producto.getCodigo());
        dto.setNombre(producto.getNombre());
        
        // Color: convertir enum a String, o "NA" si es null
        if (producto.getColor() != null) {
            dto.setColor(producto.getColor().name());
        } else {
            dto.setColor("NA");
        }
        
        // Posición: puede ser null
        dto.setPosicion(producto.getPosicion());
        
        // Categoría: crear DTO simplificado
        if (producto.getCategoria() != null) {
            ProductoPosicionDTO.CategoriaDTO categoriaDTO = new ProductoPosicionDTO.CategoriaDTO();
            categoriaDTO.setId(producto.getCategoria().getId());
            categoriaDTO.setNombre(producto.getCategoria().getNombre());
            dto.setCategoria(categoriaDTO);
        } else {
            dto.setCategoria(null);
        }
        
        return dto;
    }
}
